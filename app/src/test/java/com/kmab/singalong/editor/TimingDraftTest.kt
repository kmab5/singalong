package com.kmab.singalong.editor

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class TimingDraftTest {
    private val original = LyricsEditor.import("song", "[00:01]First\n[00:02]Second\n[00:03]Third")
    @Test fun timestampsAcceptFinePrecisionAndRejectInvalidInput() {
        assertEquals(62_125L, LyricTimestamp.parse("01:02.125"))
        assertEquals(62_100L, LyricTimestamp.parse("1:02.1"))
        assertEquals("01:02.125", LyricTimestamp.format(62_125))
        assertNull(LyricTimestamp.parse(""))
        listOf("-01:00", "1:60", "NaN", "1:02.1234", "999999999999999999:00").forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) { LyricTimestamp.parse(bad) }
        }
    }
    @Test fun partialTimingCreatesANewVersionAndRetainsOriginalLineIdentity() {
        val edited = LyricsEditor.editTiming(original, "First changed\nSecond\nThird", listOf(1100L, null, 3500L))
        assertEquals(listOf(1100L, null, 3500L), edited.lines.map { it.startMs })
        assertEquals(original.lines.map { it.id }, edited.lines.map { it.id })
        assertNotEquals(original.id, edited.id)
        assertNotEquals(original.contentHash, edited.contentHash)
        assertEquals(listOf(1000L, 2000L, 3000L), original.lines.map { it.startMs })
        assertEquals("First", original.lines.first().text)
        assertEquals(MatchConfidence.UNCERTAIN, edited.confidence)
    }
    @Test fun invalidOrderAndStructuralChangesAreRejectedWithoutSortingLyrics() {
        assertThrows(IllegalArgumentException::class.java) { LyricsEditor.editTiming(original, "First\nSecond\nThird", listOf(2000L, null, 1000L)) }
        assertThrows(IllegalArgumentException::class.java) { LyricsEditor.editTiming(original, "First\nSecond", listOf(1000L, 2000L, 3000L)) }
        assertThrows(IllegalArgumentException::class.java) { LyricsEditor.editTiming(original, "First\nSecond\nThird", listOf(-1L, 2000L, 3000L)) }
        assertEquals(listOf(1000L, 1000L, 3000L), LyricsEditor.editTiming(original, "First\nSecond\nThird", listOf(1000L, 1000L, 3000L)).lines.map { it.startMs })
    }
    @Test fun stampingUsesMatchingPlaybackAndAdvancesWithoutMutatingPreviousDraft() {
        val recording = Recording("song", "Title", listOf("Artist"), null, 120_000)
        val snapshot = PlaybackSnapshot("session", "player", "Player", recording, PlaybackStatus.PLAYING, 10_000, 100, 1f)
        val draft = TimingDraft(listOf(null, null, null))
        val stamped = draft.stamp("song", snapshot, 600)
        assertEquals(listOf(10_500L, null, null), stamped.times)
        assertEquals(1, stamped.selectedIndex)
        assertTrue(draft.times.all { it == null })
        assertThrows(IllegalArgumentException::class.java) { draft.stamp("other", snapshot, 600) }
        assertThrows(IllegalArgumentException::class.java) { draft.stamp("song", snapshot.copy(positionMs = null), 600) }
        assertThrows(IllegalArgumentException::class.java) { draft.stamp("song", snapshot.copy(status = PlaybackStatus.STOPPED), 600) }
    }
    @Test fun unfinishedInputIsPreservedUntilValidAndCommittedWhenChangingLine() {
        val draft = TimingDraft(listOf(1000L, 2000L), timeInput = "00:")
        assertNotNull(draft.problem(2))
        assertEquals("00:", draft.input)
        assertThrows(IllegalArgumentException::class.java) { draft.select(1) }
        val next = draft.copy(timeInput = "00:01.200").select(1)
        assertEquals(listOf(1200L, 2000L), next.times)
        assertEquals("00:02.000", next.input)
        assertEquals(2100L, next.nudge(100).times[1])
        assertNull(next.setTime(null).times[1])
    }
}
