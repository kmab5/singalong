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
        require(rows.size <= 20_000) { "Too many lyric lines." }
        val unchanged = text == source.lines.joinToString("\n") { it.text }
        require(!keepTiming || unchanged || source.lines.none { '\n' in it.text || '\r' in it.text }) {
            "This version has multiline timing segments. Create an untimed copy before editing their text."
        }
        require(!keepTiming || unchanged || rows.size == source.lines.size) { "Timed edits must keep the same number of lines. Create an untimed copy to add or remove lines." }
        val lines = if (keepTiming && unchanged) source.lines else rows.mapIndexed { index, row ->
            if (keepTiming) source.lines[index].let { it.copy(text = row, words = if (it.text == row) it.words else emptyList()) }
            else LyricLine("$index", null, row)
        }
        return source.copy(id = UUID.randomUUID().toString(), lines = lines, contentHash = hash(lines), origin = LyricOrigin.PERSONAL,
            sourceName = "Personal edit", providerId = null)
    }
    fun editTiming(source: LyricVersion, text: String, times: List<Long?>): LyricVersion {
        LyricTimestamp.validate(times)
        val edited = edit(source, text, keepTiming = false)
        require(edited.lines.size == times.size) { "Line count changed. Restore the lines or create an untimed version before retiming." }
        val lines = edited.lines.mapIndexed { i, line ->
            val original = source.lines.getOrNull(i)?.takeIf { source.lines.size == times.size }
            val unchanged = original != null && original.text == line.text && original.startMs == times[i]
            line.copy(id = original?.id ?: line.id, startMs = times[i],
                endMs = if (unchanged) original?.endMs else null,
                words = if (unchanged) original!!.words else emptyList())
        }
        return edited.copy(lines = lines, contentHash = hash(lines), confidence = MatchConfidence.UNCERTAIN)
    }
    fun import(recordingId: String, text: String): LyricVersion {
        val clean = text.removePrefix("\uFEFF").replace("\r\n", "\n")
        require(clean.isNotBlank() && clean.toByteArray().size <= MAX_BYTES) { "Choose nonempty UTF-8 lyrics smaller than 2 MiB." }
        val lines = LrcParser.parse(clean, clean)
        require(lines.size <= 20_000) { "Too many lyric lines." }
        return LyricVersion(UUID.randomUUID().toString(), recordingId, hash(lines), LyricOrigin.IMPORT, lines, "Imported file", null, MatchConfidence.UNCERTAIN)
    }
}
