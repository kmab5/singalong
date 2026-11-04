package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine

/** Visual estimates only. Never creates or modifies stored word/line timestamps. */
object WordSweep {
    data class Word(val start: Int, val end: Int)
    fun words(text: String): List<Word> = Regex("\\S+").findAll(text).map { Word(it.range.first, it.range.last + 1) }.toList()

    fun progress(lines: List<LyricLine>, index: Int, positionMs: Long, delayMs: Long, durationMs: Long?): Float? {
        val start = lines.getOrNull(index)?.startMs ?: return null
        if (lines[index].text.isBlank()) return null
        val end = lines.drop(index + 1).firstNotNullOfOrNull { it.startMs?.takeIf { time -> time > start } }
            ?: durationMs?.takeIf { it > start } ?: return null
        return ((positionMs.toDouble() - delayMs - start) / (end - start)).toFloat().coerceIn(0f, 1f)
    }

    fun emphasis(wordIndex: Int, wordCount: Int, progress: Float): Float =
        (progress.coerceIn(0f, 1f) * wordCount - wordIndex).coerceIn(0f, 1f)
}
