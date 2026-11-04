package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine
import org.junit.Assert.*
import org.junit.Test

class WordSweepTest {
    private val lines = listOf(LyricLine("a", 1_000, "one two"), LyricLine("b", 5_000, "three"))
    @Test fun progressTracksOffsetsSeeksAndKnownEndOfLastLine() {
        assertEquals(.5f, WordSweep.progress(lines, 0, 3_500, 500, 8_000)!!, .001f)
        assertEquals(0f, WordSweep.progress(lines, 0, 0, 0, 8_000)!!, .001f)
        assertEquals(1f, WordSweep.progress(lines, 0, 8_000, 0, 8_000)!!, .001f)
        assertEquals(.5f, WordSweep.progress(lines, 1, 6_500, 0, 8_000)!!, .001f)
        assertNull(WordSweep.progress(lines, 1, 6_500, 0, null))
    }
    @Test fun noEstimatedWordProgressForUntimedOrBlankLines() {
        assertNull(WordSweep.progress(listOf(LyricLine("a", null, "untimed")), 0, 10, 0, 100))
        assertNull(WordSweep.progress(listOf(LyricLine("a", 0, "")), 0, 10, 0, 100))
    }
    @Test fun wordSpansPreserveOriginalSpacingPunctuationAndUnicode() {
        val text = "  Hello,  世界!\n🎶 end"
        assertEquals(listOf("Hello,", "世界!", "🎶", "end"), WordSweep.words(text).map { text.substring(it.start, it.end) })
        assertEquals(.5f, WordSweep.emphasis(1, 3, .5f), .001f)
        assertEquals(1f, WordSweep.emphasis(0, 3, .5f), .001f)
        assertEquals(0f, WordSweep.emphasis(2, 3, .5f), .001f)
    }
}
