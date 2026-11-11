package com.kmab.singalong.backup

import com.kmab.singalong.model.LyricVersion
import com.kmab.singalong.model.Recording
import com.kmab.singalong.translation.TranslationRecord
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.FilterOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.CRC32
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable private data class ArchiveIndex(val format: String = "singalong", val schema: Int = 1,
    val options: BackupOptions, val entries: List<ArchiveEntry>)
@Serializable private data class ArchiveEntry(val path: String, val bytes: Int, val sha256: String)
@Serializable private data class ArchiveDetails(val recordings: List<Recording>, val mediaFilesIncluded: Boolean = false,
    val createdAtEpochMs: Long? = null, val appVersion: String? = null)
@Serializable private data class SongMetadata(val recording: Recording, val favorite: Boolean)
@Serializable private data class SongHistory(val recordingId: String, val plays: Long, val lastPlayed: Long?)
@Serializable private data class SongLyrics(val recordingId: String, val versions: List<String>,
    val references: List<LyricReference>, val selected: String?, val adjustments: List<BackupAdjustment>,
    val translations: List<TranslationRecord>)

/** Bounded, self-describing ZIP format. No entry is extracted to the filesystem. */
object BackupArchive {
    const val MAX_ENTRY_BYTES = 8 * 1024 * 1024
    const val MAX_ENTRIES = 20_006
    private const val MAX_ARCHIVE_BYTES = BackupCodec.MAX_BYTES + 4 * 1024 * 1024
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun write(document: BackupDocument, output: OutputStream, options: BackupOptions = document.options,
        createdAtEpochMs: Long? = null, appVersion: String? = null) {
        require((createdAtEpochMs ?: 0) >= 0 && (appVersion?.length ?: 0) <= 100) { "Invalid backup creation details." }
        val selected = document.select(options)
        BackupCodec.validate(selected)
        val entries = linkedMapOf<String, ByteArray>()
        var expandedBytes = 0L
        fun add(path: String, text: String) {
            val bytes = text.toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_ENTRY_BYTES) { "A backup section exceeds 8 MiB." }
            expandedBytes += bytes.size
            require(entries.size < MAX_ENTRIES - 1 && expandedBytes <= BackupCodec.MAX_BYTES) { "Backup exceeds its size limit." }
            entries[path] = bytes
        }
        add("details.json", json.encodeToString(ArchiveDetails(selected.songs.map { it.recording.identityOnly() },
            createdAtEpochMs = createdAtEpochMs, appVersion = appVersion)))
        if (options.songMetadata) add("songs.json", json.encodeToString(selected.songs.map { SongMetadata(it.recording, it.favorite) }))
        if (options.history) add("history.json", json.encodeToString(selected.songs.map { SongHistory(it.recording.id, it.plays, it.lastPlayed) }))
        if (options.settings) add("settings.json", json.encodeToString(selected.preferences))
        if (options.lyrics != LyricsBackupMode.NONE) {
            var versionIndex = 0
            val lyrics = selected.songs.map { song ->
                val paths = song.versions.map { version ->
                    val path = "lyrics/${versionIndex++}.json"
                    add(path, json.encodeToString(version)); path
                }
                SongLyrics(song.recording.id, paths, song.lyricReferences, song.selected, song.adjustments, song.translations)
            }
            add("lyrics.json", json.encodeToString(lyrics))
        }
        val index = json.encodeToString(ArchiveIndex(options = options, entries = entries.map { (path, bytes) -> ArchiveEntry(path, bytes.size, hash(bytes)) }))
        require(index.toByteArray().size <= MAX_ENTRY_BYTES && entries.values.sumOf { it.size.toLong() } + index.toByteArray().size <= BackupCodec.MAX_BYTES) { "Backup exceeds 50 MiB." }
        // Release the compressor while leaving ownership of the destination with the caller.
        ZipOutputStream(object : FilterOutputStream(output) {
            override fun write(bytes: ByteArray, offset: Int, length: Int) = output.write(bytes, offset, length)
            override fun close() = flush()
        }).use { zip ->
            fun put(path: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }
            put("index.json", index.toByteArray(Charsets.UTF_8))
            entries.forEach { (path, bytes) -> put(path, bytes) }
        }
    }

    fun read(input: InputStream): BackupDocument {
        val bytes = boundedRead(input, MAX_ARCHIVE_BYTES)
        if (bytes.size < 4 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4b.toByte()) {
            return BackupCodec.decode(utf8(bytes).removePrefix("\uFEFF"))
        }
        val directory = readDirectory(bytes)
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && safePath(entry.name)) { "Unsafe backup entry." }
                require(entries.size < MAX_ENTRIES && !entries.containsKey(entry.name)) { "Duplicate or excessive backup entries." }
                val content = boundedRead(zip, minOf(MAX_ENTRY_BYTES, (BackupCodec.MAX_BYTES - total).toInt()))
                total += content.size
                entries[entry.name] = content
                zip.closeEntry()
            }
        }
        require(entries.keys == directory.keys) { "Incomplete ZIP directory." }
        entries.forEach { (path, content) ->
            val entry = requireNotNull(directory[path])
            require(entry.size == content.size.toLong() && entry.crc == CRC32().apply { update(content) }.value) { "Damaged ZIP directory." }
        }
        fun text(path: String) = utf8(requireNotNull(entries[path]) { "Missing backup entry: $path" })
        val index = json.decodeFromString<ArchiveIndex>(text("index.json"))
        require(index.format == "singalong" && index.schema == 1) { "Unsupported backup version. Update Singalong before restoring." }
        require(index.options.hasContent && index.entries.size < MAX_ENTRIES)
        require(index.entries.map { it.path }.toSet().size == index.entries.size && index.entries.none { it.path == "index.json" }) { "Duplicate backup index entries." }
        require(entries.keys == (index.entries.map { it.path } + "index.json").toSet()) { "Backup entries do not match its index." }
        index.entries.forEach { item ->
            val data = requireNotNull(entries[item.path])
            require(item.bytes == data.size && item.sha256 == hash(data)) { "Damaged backup entry: ${item.path}" }
        }
        val details = json.decodeFromString<ArchiveDetails>(text("details.json"))
        require(!details.mediaFilesIncluded) { "Audio files are not supported in backups." }
        require((details.createdAtEpochMs ?: 0) >= 0 && (details.appVersion?.length ?: 0) <= 100) { "Invalid backup creation details." }
        require(details.recordings.size <= 20_000 && details.recordings.all { it == it.identityOnly() })
        val ids = details.recordings.map { it.id }.toSet()
        require(ids.size == details.recordings.size) { "Duplicate recordings." }
        val used = mutableSetOf("index.json", "details.json")
        fun section(path: String): String { used.add(path); return text(path) }
        val options = index.options
        val metadata = if (options.songMetadata) json.decodeFromString<List<SongMetadata>>(section("songs.json")) else emptyList()
        val history = if (options.history) json.decodeFromString<List<SongHistory>>(section("history.json")) else emptyList()
        val lyrics = if (options.lyrics != LyricsBackupMode.NONE) json.decodeFromString<List<SongLyrics>>(section("lyrics.json")) else emptyList()
        fun requireIds(values: List<String>, included: Boolean) {
            require(values.toSet().size == values.size && (!included || values.toSet() == ids)) { "Inconsistent recording references." }
        }
        requireIds(metadata.map { it.recording.id }, options.songMetadata)
        requireIds(history.map { it.recordingId }, options.history)
        requireIds(lyrics.map { it.recordingId }, options.lyrics != LyricsBackupMode.NONE)
        val metadataById = metadata.associateBy { it.recording.id }
        val historyById = history.associateBy { it.recordingId }
        val lyricsById = lyrics.associateBy { it.recordingId }
        val songs = details.recordings.map { identity ->
            val meta = metadataById[identity.id]
            require(meta == null || meta.recording.identityOnly() == identity) { "Inconsistent song identity." }
            val plays = historyById[identity.id]
            val lyric = lyricsById[identity.id]
            val versions = lyric?.versions.orEmpty().map { path ->
                require(path.matches(Regex("lyrics/[0-9]+\\.json")) && used.add(path)) { "Invalid or repeated lyric file reference." }
                json.decodeFromString<LyricVersion>(text(path))
            }
            BackupSong(meta?.recording ?: identity, plays?.plays ?: 0, plays?.lastPlayed, meta?.favorite ?: false,
                versions, lyric?.selected, lyric?.adjustments.orEmpty(), lyric?.translations.orEmpty(), lyric?.references.orEmpty())
        }
        val settings = if (options.settings) json.decodeFromString<Map<String, String>>(section("settings.json")) else emptyMap()
        require(used == entries.keys) { "Unexpected backup entries." }
        return BackupDocument(songs = songs, preferences = settings, options = options).also(BackupCodec::validate)
    }

    private fun boundedRead(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= limit) { "Backup exceeds its size limit." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
    private fun utf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes)).toString()
    private fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun safePath(path: String): Boolean = path.length in 1..200 && !path.startsWith('/') &&
        path.split('/').all { it.isNotEmpty() && it != "." && it != ".." } && path.all { it.isLetterOrDigit() || it in "/_.-" }

    private data class DirectoryEntry(val size: Long, val crc: Long)

    /** Check central and local records together, rejecting truncation, duplicate names and ZIP64. */
    private fun readDirectory(bytes: ByteArray): Map<String, DirectoryEntry> {
        fun short(at: Int) = (bytes[at].toInt() and 255) or ((bytes[at + 1].toInt() and 255) shl 8)
        fun uint(at: Int): Long = (0..3).sumOf { (bytes[at + it].toLong() and 255) shl (8 * it) }
        for (at in bytes.size - 22 downTo maxOf(0, bytes.size - 65_557)) {
            if (uint(at) != 0x06054b50L || at + 22 + short(at + 20) != bytes.size) continue
            val count = short(at + 10)
            require(short(at + 4) == 0 && short(at + 6) == 0 && short(at + 8) == count && count in 1..MAX_ENTRIES)
            require(uint(at + 12) + uint(at + 16) == at.toLong()) { "Invalid ZIP directory." }
            var cursor = uint(at + 16).toInt()
            val result = linkedMapOf<String, DirectoryEntry>()
            val offsets = mutableSetOf<Int>()
            repeat(count) {
                require(cursor >= 0 && cursor + 46 <= at && uint(cursor) == 0x02014b50L) { "Invalid ZIP directory entry." }
                val nameLength = short(cursor + 28)
                val end = cursor + 46 + nameLength + short(cursor + 30) + short(cursor + 32)
                require(end <= at && short(cursor + 34) == 0)
                val nameBytes = bytes.copyOfRange(cursor + 46, cursor + 46 + nameLength)
                val name = utf8(nameBytes)
                val localOffset = uint(cursor + 42)
                require(localOffset <= bytes.size.toLong() && offsets.add(localOffset.toInt()))
                val local = localOffset.toInt()
                require(local >= 0 && local + 30 <= uint(at + 16) && uint(local) == 0x04034b50L) { "Missing local ZIP entry." }
                require(local + 30 + short(local + 26) <= cursor && short(local + 26) == nameLength &&
                    bytes.copyOfRange(local + 30, local + 30 + nameLength).contentEquals(nameBytes)) { "Inconsistent ZIP paths." }
                require(safePath(name) && result.put(name, DirectoryEntry(uint(cursor + 24), uint(cursor + 16))) == null) { "Duplicate or unsafe ZIP paths." }
                cursor = end
            }
            require(cursor == at) { "Unexpected ZIP directory data." }
            return result
        }
        throw IllegalArgumentException("Incomplete ZIP backup.")
    }
}
