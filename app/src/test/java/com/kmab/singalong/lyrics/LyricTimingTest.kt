package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class LyricTimingTest {
    @Test fun parsesMultipleTagsOffsetsUnicodeAndBlankIntervals() {
        val lines = LrcParser.parse("[offset:-500]\n[00:01.00][00:03.00]世界\n[00:04.00]\n[00:08.00]next", null)
        assertEquals(listOf(500L, 2500L, 3500L, 7500L), lines.map { it.startMs })
        assertEquals("世界", lines[1].text)
        assertEquals(4L, LyricTimeline.at(lines, 3500, 0).countdownSeconds)
        assertNull(LyricTimeline.at(lines, 3000, 0).countdownSeconds)
    }
    @Test fun positiveDelayMakesWordsLaterAndUnknownPositionStaysUnknown() {
        val lines = LrcParser.parse("[00:01.00]one\n[00:02.00]two", null)
        assertEquals(0, LyricTimeline.at(lines, 2100, 250).index)
        assertNull(PlaybackClock.position(null, 2000))
        val playing = PlaybackSnapshot("s", "p", "p", null, PlaybackStatus.PLAYING, 500, 1000, 2f)
        assertEquals(2500L, PlaybackClock.position(playing, 2000))
        assertEquals(500L, PlaybackClock.position(playing.copy(status = PlaybackStatus.BUFFERING), 2000))
    }
    @Test fun malformedTimestampsFallBackToPlainTextAndRepeatedTimesRemainStable() {
        assertEquals(listOf("Plain", "世界"), LrcParser.parse("[00:99]bad", "Plain\n世界").map { it.text })
        val repeated = LrcParser.parse("[00:01]one\n[00:01]two", null)
        assertEquals(listOf("one", "two"), repeated.map { it.text })
        assertEquals(1, LyricTimeline.at(repeated, 1000, 0).index)
    }
}
