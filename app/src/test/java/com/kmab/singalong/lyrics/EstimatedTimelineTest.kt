package com.kmab.singalong.lyrics

import org.junit.Assert.*
import org.junit.Test

class EstimatedTimelineTest {
    @Test fun estimatesSkipBlankLinesAndRespectOffsetsAndSeekingWithoutChangingOriginals() {
        val lines = LrcParser.parse(null, "One\n\nTwo words")
        val starts = EstimatedTimeline.starts(lines, 11_000)
        assertEquals(mapOf(0 to 0L, 2 to 3000L), starts)
        assertEquals(0, EstimatedTimeline.at(lines, 2999, 11_000, 0).index)
        assertEquals(2, EstimatedTimeline.at(lines, 3000, 11_000, 0).index)
        assertEquals(0, EstimatedTimeline.at(lines, 3000, 11_000, 50).index)
        assertEquals(-1, EstimatedTimeline.at(lines, 0, 11_000, 50).index)
        assertEquals(0, EstimatedTimeline.at(lines, 1000, 11_000, 0).index)
        assertTrue(lines.all { it.startMs == null })
        assertTrue(EstimatedTimeline.starts(lines, 0).isEmpty())
    }
}
