@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.kmab.singalong.backup

import com.kmab.singalong.model.LyricVersion
import com.kmab.singalong.model.Recording
import com.kmab.singalong.translation.TranslationRecord
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Serializable private data class ArchiveIndex(val format: String = "singalong", val schema: Int = 1,
    val options: BackupOptions, val entries: List<ArchiveEntry>)
@Serializable private data class ArchiveEntry(val path: String, val bytes: Long, val sha256: String)
@Serializable private data class ArchiveDetails(val recordings: List<Recording>, val mediaFilesIncluded: Boolean = false,
    val createdAtEpochMs: Long? = null, val appVersion: String? = null)
@Serializable private data class SongMetadata(val recording: Recording, val favorite: Boolean)
@Serializable private data class SongHistory(val recordingId: String, val plays: Long, val lastPlayed: Long?)
@Serializable private data class SongLyrics(val recordingId: String, val versions: List<String>,
    val references: List<LyricReference>, val selected: String?, val adjustments: List<BackupAdjustment>,
    val translations: List<TranslationRecord>)

/** Streaming ZIP format. Only a temporary archive is written; archive paths are never extracted. */
object BackupArchive {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun write(document: BackupDocument, output: OutputStream, options: BackupOptions = document.options,
        createdAtEpochMs: Long? = null, appVersion: String? = null, limits: BackupLimits = BackupLimits()) {
        require((createdAtEpochMs ?: 0) >= 0 && (appVersion?.length ?: 0) <= 100) { "Invalid backup creation details." }
        val selected = document.select(options)
        BackupCodec.validate(selected)
        // Close the compressor while leaving the destination owned by the caller.
        ZipOutputStream(object : FilterOutputStream(output) {
            override fun write(bytes: ByteArray, offset: Int, length: Int) = output.write(bytes, offset, length)
            override fun close() = flush()
        }).use { zip ->
            val writer = EntryWriter(zip, BackupByteBudget(limits))
            writer.add("details.json", ArchiveDetails(selected.songs.map { it.recording.identityOnly() },
                createdAtEpochMs = createdAtEpochMs, appVersion = appVersion))
            if (options.songMetadata) writer.add("songs.json", selected.songs.map { SongMetadata(it.recording, it.favorite) })
            if (options.history) writer.add("history.json", selected.songs.map { SongHistory(it.recording.id, it.plays, it.lastPlayed) })
            if (options.settings) writer.add("settings.json", selected.preferences)
            if (options.lyrics != LyricsBackupMode.NONE) {
                var versionIndex = 0L
                val lyrics = selected.songs.map { song ->
                    val paths = song.versions.map { version ->
                        val path = "lyrics/${versionIndex++}.json"
                        writer.add(path, version)
                        path
                    }
                    SongLyrics(song.recording.id, paths, song.lyricReferences, song.selected, song.adjustments, song.translations)
                }
                writer.add("lyrics.json", lyrics)
            }
            // The manifest follows the streamed payloads so it needs only hashes and byte counts.
            writer.add("index.json", ArchiveIndex(options = options, entries = writer.entries.toList()), indexed = false)
        }
    }

    fun read(input: InputStream, limits: BackupLimits = BackupLimits(), scratchDirectory: File? = null): BackupDocument {
        val source = PushbackInputStream(input, 4)
        val prefix = ByteArray(4)
        var count = 0
        while (count < prefix.size) {
            val next = source.read(prefix, count, prefix.size - count)
            if (next < 0) break
            count += next
        }
        val isZip = count >= 2 && prefix[0] == 0x50.toByte() && prefix[1] == 0x4b.toByte()
        val bom = if (!isZip && count >= 3 && prefix[0] == 0xef.toByte() && prefix[1] == 0xbb.toByte() && prefix[2] == 0xbf.toByte()) 3 else 0
        source.unread(prefix, bom, count - bom)
        if (!isZip) return BackupCodec.decode(source, limits)

        val temporary = File.createTempFile("singalong-backup-", ".zip", scratchDirectory)
        try {
            val compressedBudget = BackupByteBudget(BackupLimits(limits.compressedLimit()))
            temporary.outputStream().buffered().use { destination ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    compressedBudget.add(read)
                    destination.write(buffer, 0, read)
                }
            }
            try {
                ZipFile(temporary).use { zip ->
                    val entries = linkedMapOf<String, ZipEntry>()
                    var declaredSize = 0L
                    val directory = zip.entries()
                    while (directory.hasMoreElements()) {
                        val entry = directory.nextElement()
                        require(!entry.isDirectory && safePath(entry.name) && !entries.containsKey(entry.name)) { "Duplicate or unsafe backup entries." }
                        require(entry.size >= 0 && entry.compressedSize >= 0 && declaredSize <= Long.MAX_VALUE - entry.size) { "Invalid ZIP entry size." }
                        declaredSize += entry.size
                        limits.check(declaredSize)
                        entries[entry.name] = entry
                    }
                    val budget = BackupByteBudget(limits)
                    val verified = entries.mapValues { (name, entry) ->
                        zip.getInputStream(entry).use { content ->
                            val result = inspect(name, content, budget)
                            require(result.bytes == entry.size && result.crc == entry.crc) { "Damaged ZIP entry: $name" }
                            result
                        }
                    }
                    verifyLocalEntries(temporary, verified, limits)
                    return decodeArchive(zip, entries, verified)
                }
            } catch (failure: ZipException) {
                throw IllegalArgumentException("Damaged or incomplete ZIP backup.", failure)
            }
        } finally {
            if (!temporary.delete() && temporary.exists()) throw java.io.IOException("Could not remove temporary backup file.")
        }
    }

    private fun decodeArchive(zip: ZipFile, entries: Map<String, ZipEntry>, verified: Map<String, VerifiedEntry>): BackupDocument {
        val index = decode<ArchiveIndex>(zip, entries, "index.json")
        require(index.format == "singalong" && index.schema == 1) { "Unsupported backup version. Update Singalong before restoring." }
        require(index.options.hasContent)
        require(index.entries.map { it.path }.toSet().size == index.entries.size && index.entries.none { it.path == "index.json" }) { "Duplicate backup index entries." }
        require(entries.keys == (index.entries.map { it.path } + "index.json").toSet()) { "Backup entries do not match its index." }
        index.entries.forEach { item ->
            val entry = requireNotNull(verified[item.path])
            require(item.bytes == entry.bytes && item.sha256 == entry.sha256) { "Damaged backup entry: ${item.path}" }
        }
        val details = decode<ArchiveDetails>(zip, entries, "details.json")
        require(!details.mediaFilesIncluded) { "Audio files are not supported in backups." }
        require((details.createdAtEpochMs ?: 0) >= 0 && (details.appVersion?.length ?: 0) <= 100) { "Invalid backup creation details." }
        require(details.recordings.all { it == it.identityOnly() })
        val ids = details.recordings.map { it.id }.toSet()
        require(ids.size == details.recordings.size) { "Duplicate recordings." }
        val used = mutableSetOf("index.json", "details.json")
        val options = index.options
        val metadata = if (options.songMetadata) decode<List<SongMetadata>>(zip, entries, "songs.json".also(used::add)) else emptyList()
        val history = if (options.history) decode<List<SongHistory>>(zip, entries, "history.json".also(used::add)) else emptyList()
        val lyrics = if (options.lyrics != LyricsBackupMode.NONE) decode<List<SongLyrics>>(zip, entries, "lyrics.json".also(used::add)) else emptyList()
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
                decode<LyricVersion>(zip, entries, path)
            }
            BackupSong(meta?.recording ?: identity, plays?.plays ?: 0, plays?.lastPlayed, meta?.favorite ?: false,
                versions, lyric?.selected, lyric?.adjustments.orEmpty(), lyric?.translations.orEmpty(), lyric?.references.orEmpty())
        }
        val settings = if (options.settings) decode<Map<String, String>>(zip, entries, "settings.json".also(used::add)) else emptyMap()
        require(used == entries.keys) { "Unexpected backup entries." }
        return BackupDocument(songs = songs, preferences = settings, options = options).also(BackupCodec::validate)
    }

    private inline fun <reified T> decode(zip: ZipFile, entries: Map<String, ZipEntry>, path: String): T {
        val entry = requireNotNull(entries[path]) { "Missing backup entry: $path" }
        return zip.getInputStream(entry).use { json.decodeFromStream<T>(it) }
    }

    private class EntryWriter(private val zip: ZipOutputStream, private val budget: BackupByteBudget) {
        val entries = mutableListOf<ArchiveEntry>()
        inline fun <reified T> add(path: String, value: T, indexed: Boolean = true) {
            zip.putNextEntry(ZipEntry(path))
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            val output = object : OutputStream() {
                override fun write(value: Int) {
                    budget.add(1)
                    count++
                    digest.update(value.toByte())
                    zip.write(value)
                }
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    budget.add(length)
                    count += length
                    digest.update(bytes, offset, length)
                    zip.write(bytes, offset, length)
                }
            }
            json.encodeToStream(value, output)
            zip.closeEntry()
            if (indexed) entries.add(ArchiveEntry(path, count, hex(digest.digest())))
        }
    }

    private data class VerifiedEntry(val bytes: Long, val sha256: String, val crc: Long)
    private fun inspect(name: String, input: InputStream, budget: BackupByteBudget): VerifiedEntry {
        val digest = MessageDigest.getInstance("SHA-256")
        val crc = CRC32()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var count = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            budget.add(read)
            require(count <= Long.MAX_VALUE - read) { "Invalid byte count for $name." }
            count += read
            digest.update(buffer, 0, read)
            crc.update(buffer, 0, read)
        }
        return VerifiedEntry(count, hex(digest.digest()), crc.value)
    }

    /** Also inspect local headers, so central-directory aliases cannot conceal different entries. */
    private fun verifyLocalEntries(file: File, expected: Map<String, VerifiedEntry>, limits: BackupLimits) {
        val seen = mutableSetOf<String>()
        val budget = BackupByteBudget(limits)
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && safePath(entry.name) && seen.add(entry.name)) { "Duplicate or unsafe local ZIP entries." }
                val verified = requireNotNull(expected[entry.name]) { "Unexpected local ZIP entry." }
                require(inspect(entry.name, zip, budget) == verified) { "Local ZIP entry differs from directory." }
                zip.closeEntry()
            }
        }
        require(seen == expected.keys) { "Incomplete ZIP directory." }
    }

    private fun hex(bytes: ByteArray): String {
        val digits = "0123456789abcdef"
        val characters = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, value ->
            val unsigned = value.toInt() and 255
            characters[index * 2] = digits[unsigned ushr 4]
            characters[index * 2 + 1] = digits[unsigned and 15]
        }
        return String(characters)
    }
    private fun safePath(path: String): Boolean = path.length in 1..200 && !path.startsWith('/') &&
        path.split('/').all { it.isNotEmpty() && it != "." && it != ".." } && path.all { it.isLetterOrDigit() || it in "/_.-" }
}
