package com.kmab.singalong.backup

import com.kmab.singalong.model.*
import com.kmab.singalong.translation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

@Serializable data class BackupSong(val recording: Recording, val plays: Long, val lastPlayed: Long?, val favorite: Boolean,
    val versions: List<LyricVersion>, val selected: String?, val adjustments: List<BackupAdjustment> = emptyList(), val translations: List<TranslationRecord> = emptyList())
@Serializable data class BackupAdjustment(val versionId: String, val delayMs: Long, val allowUncertainSync: Boolean)
@Serializable data class BackupDocument(val schema: Int = 1, val songs: List<BackupSong>, val preferences: Map<String, String> = emptyMap())

object BackupCodec {
    const val MAX_BYTES = 50 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = false }
    fun read(input: InputStream, limit: Int): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(out.size() + count <= limit) { "File is too large (limit ${limit / 1024 / 1024} MiB)." }
            out.write(buffer, 0, count)
        }
        return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(out.toByteArray())).toString().removePrefix("\uFEFF")
    }
    fun encode(document: BackupDocument): String { validate(document); return json.encodeToString(document).also { require(it.toByteArray().size <= MAX_BYTES) { "Backup exceeds 50 MiB." } } }
    fun decode(text: String): BackupDocument {
        require(text.toByteArray().size <= MAX_BYTES) { "Backup exceeds 50 MiB." }
        return json.decodeFromString<BackupDocument>(text).also(::validate)
    }
    fun validate(document: BackupDocument) {
        require(document.schema == 1) { "Unsupported backup version. Update Singalong before restoring." }
        require(document.songs.size <= 20_000 && document.songs.map { it.recording.id }.distinct().size == document.songs.size) { "Duplicate or excessive recordings." }
        require(document.preferences.size <= 50 && document.preferences.all { it.key.length <= 100 && it.value.length <= 1000 })
        val ids = mutableSetOf<String>()
        document.songs.forEach { song ->
            val r = song.recording
            require(r.id.isNotBlank() && r.id.length <= 200 && r.title.isNotBlank() && r.title.length <= 4096 && r.artists.size <= 100 && r.artists.all { it.length <= 4096 } && (r.album?.length ?: 0) <= 4096)
            require(song.plays >= 0 && (song.lastPlayed ?: 0) >= 0 && (r.durationMs ?: 0) >= 0)
            require(song.versions.size <= 500)
            song.versions.forEach { v ->
                require(ids.add(v.id) && v.id.isNotBlank() && v.id.length <= 512 && v.recordingId == r.id)
                require(v.lines.isNotEmpty() && v.lines.size <= 20_000 && v.lines.map { it.id }.distinct().size == v.lines.size)
                require(v.lines.all { it.id.length <= 200 && it.text.length <= 100_000 && (it.startMs ?: 0) >= 0 })
                require(v.lines.sumOf { it.text.toByteArray().size.toLong() } <= 2 * 1024 * 1024)
                require(v.lines.mapNotNull { it.startMs }.zipWithNext().all { (a, b) -> a <= b })
            }
            require(song.translations.map { it.versionId }.distinct().size == song.translations.size)
            require(song.translations.all { t -> song.versions.any { TranslationRepository.valid(it, t) } })
            require(song.selected == null || song.versions.any { it.id == song.selected })
            require(song.adjustments.map { it.versionId }.distinct().size == song.adjustments.size)
            require(song.adjustments.all { a -> a.delayMs in -600_000..600_000 && song.versions.any { it.id == a.versionId } })
        }
    }
}
