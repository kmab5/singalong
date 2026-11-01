package com.kmab.singalong.editor

import com.kmab.singalong.model.*
import com.kmab.singalong.lyrics.LrcParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

object LyricsEditor {
    const val MAX_BYTES = 2 * 1024 * 1024
    fun hash(lines: List<LyricLine>) = MessageDigest.getInstance("SHA-256")
        .digest(Json.encodeToString(lines).toByteArray()).joinToString("") { "%02x".format(it) }
    fun edit(source: LyricVersion, text: String, keepTiming: Boolean): LyricVersion {
        require(text.toByteArray().size <= MAX_BYTES && text.isNotBlank()) { "Lyrics must contain text and fit within 2 MiB." }
        val rows = text.replace("\r\n", "\n").lines()
        require(!keepTiming || rows.size == source.lines.size) { "Timed edits must keep the same number of lines. Create an untimed copy to add or remove lines." }
        val lines = rows.mapIndexed { index, row -> if (keepTiming) source.lines[index].copy(text = row) else LyricLine("$index", null, row) }
        return source.copy(id = UUID.randomUUID().toString(), lines = lines, contentHash = hash(lines), origin = LyricOrigin.PERSONAL,
            sourceName = "Personal edit", providerId = null)
    }
    fun import(recordingId: String, text: String): LyricVersion {
        val clean = text.removePrefix("\uFEFF").replace("\r\n", "\n")
        require(clean.isNotBlank() && clean.toByteArray().size <= MAX_BYTES) { "Choose nonempty UTF-8 lyrics smaller than 2 MiB." }
        val lines = LrcParser.parse(clean, clean)
        require(lines.size <= 20_000) { "Too many lyric lines." }
        return LyricVersion(UUID.randomUUID().toString(), recordingId, hash(lines), LyricOrigin.IMPORT, lines, "Imported file", null, MatchConfidence.UNCERTAIN)
    }
}
