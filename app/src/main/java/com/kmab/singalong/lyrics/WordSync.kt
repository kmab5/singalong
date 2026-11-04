package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.LyricVersion
import com.kmab.singalong.model.LyricWord

/** Saved timestamps stay untouched; missing ends are inferred for drawing only. */
object WordSync {
    fun valid(line: LyricLine): Boolean {
        if (line.startMs == null || line.words.isEmpty() || line.words.size > 10_000) return false
        var offset = 0
        for (word in line.words) {
            if (word.text.isEmpty() || word.startMs < 0 || (word.endMs != null && word.endMs < word.startMs)) return false
            if (!line.text.startsWith(word.text, offset)) return false
            offset += word.text.length
        }
        return offset == line.text.length
    }

    fun ranges(words: List<LyricWord>): List<WordSweep.Word> {
        var offset = 0
        return words.map { word -> WordSweep.Word(offset, offset + word.text.length).also { offset = it.end } }
    }

    fun displayEnd(lines: List<LyricLine>, index: Int, durationMs: Long?): Long? {
        val line = lines.getOrNull(index) ?: return null
        return line.endMs ?: lines.drop(index + 1).firstNotNullOfOrNull { it.startMs?.takeIf { start -> start > (line.startMs ?: -1) } }
            ?: durationMs
    }

    class Timeline(private val words: List<LyricWord>, lineEndMs: Long?) {
        // Resolve once, not once per word on every drawing frame. Overlapping segments
        // retain their individual ends; inferred ends never enter the persisted model.
        private val ends: List<Long?> = java.util.TreeSet(words.map { it.startMs }).let { starts ->
            words.map { word -> word.endMs ?: starts.higher(word.startMs) ?: lineEndMs?.takeIf { it >= word.startMs } }
        }
        fun emphasis(index: Int, positionMs: Long?, delayMs: Long): Float {
            val word = words.getOrNull(index) ?: return 0f
            val time = positionMs?.toDouble()?.minus(delayMs) ?: return 0f
            if (time < word.startMs) return 0f
            val end = ends[index]
            // A missing final end has no reliable duration: reveal at its known start.
            if (end == null || end == word.startMs) return 1f
            return ((time - word.startMs) / (end.toDouble() - word.startMs)).toFloat().coerceIn(0f, 1f)
        }
    }

    fun label(version: LyricVersion): String {
        val vocal = version.lines.filter { it.text.isNotBlank() }
        val wordLines = vocal.count(::valid)
        return when {
            wordLines > 0 && wordLines == vocal.size -> "Word-synced"
            wordLines > 0 -> "Word-synced · some lines"
            version.lines.any { it.startMs != null } -> "Line-synced"
            else -> "Untimed"
        }
    }
}
