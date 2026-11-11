package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.LyricWord
import org.junit.Assert.*
import org.junit.Test

class FloatingWordTrackingTest {
    private val line = LyricLine("line", 1000, "One two", words = listOf(
        LyricWord("One ", 1000, 2000), LyricWord("two", 3000),
    ))

    @Test fun disabledManualAndInvalidLinesDoNotCreateWordTracking() {
        assertNull(FloatingWordTracking.create(line, 5000, enabled = false, following = true))
        assertNull(FloatingWordTracking.create(line, 5000, enabled = true, following = false))
        assertNull(FloatingWordTracking.create(line.copy(words = emptyList()), 5000, true, true))
        assertNull(FloatingWordTracking.create(line.copy(text = "Different"), 5000, true, true))
    }

    @Test fun originalRangesAndActualWordTimingHonorGapsDelayAndSeek() {
        val tracking = FloatingWordTracking.create(line, 5000, true, true)!!
        assertEquals(listOf("One ", "two"), tracking.ranges.map { line.text.substring(it.start, it.end) })
        assertEquals(.5f, tracking.emphasis(0, 1500, 0), .001f)
        assertEquals(1f, tracking.emphasis(0, 2500, 0), .001f)
        assertEquals(0f, tracking.emphasis(1, 2500, 0), .001f)
        assertEquals(.5f, tracking.emphasis(1, 4500, 500), .001f)
        assertEquals(0f, tracking.emphasis(1, 1500, 0), .001f)
        assertEquals(0f, tracking.emphasis(0, null, 0), .001f)
        assertNull(line.words.last().endMs)
    }

    @Test fun oversizedProviderLinesFallBackToReadableLineSync() {
        val words = List(129) { LyricWord("a", it * 100L) }
        assertNull(FloatingWordTracking.create(LyricLine("large", 0, "a".repeat(129), words = words), 13000, true, true))
    }
}
