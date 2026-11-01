package com.kmab.singalong.tracking

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class CollectionQualifierTest {
    private fun snapshot(status: PlaybackStatus, at: Long, position: Long = at, id: String = "song") = PlaybackSnapshot(
        "session", "player", "Player", Recording(id, "Song", listOf("Artist"), null, 120_000),
        status, position, at, 1f,
    )

    @Test fun onlyPlayingTimeQualifiesAndPauseDoesNotResetIt() {
        val subject = CollectionQualifier { "occurrence" }
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 0), 0))
        assertNull(subject.observe(snapshot(PlaybackStatus.PAUSED, 6_000), 6_000))
        assertNull(subject.observe(snapshot(PlaybackStatus.PAUSED, 6_000), 20_000))
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 20_000), 20_000))
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 20_000), 23_999))
        assertEquals("occurrence", subject.observe(snapshot(PlaybackStatus.PLAYING, 20_000), 24_000)?.occurrenceId)
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 20_000), 25_000))
    }

    @Test fun seekingForwardDoesNotAddListenedTime() {
        val subject = CollectionQualifier()
        subject.observe(snapshot(PlaybackStatus.PLAYING, 0, 0), 0)
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 500, 90_000), 500))
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 500, 90_000), 9_999))
        assertNotNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 500, 90_000), 10_000))
    }

    @Test fun changingSongsOrLosingSessionResetsQualification() {
        val subject = CollectionQualifier()
        subject.observe(snapshot(PlaybackStatus.PLAYING, 0), 0)
        subject.observe(snapshot(PlaybackStatus.PLAYING, 9_000, id = "other"), 9_000)
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 9_000, id = "other"), 10_000))
        subject.observe(null, 11_000)
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 12_000, id = "other"), 12_000))
    }

    @Test fun bufferingIsNotListeningAndDuplicateCallbacksDoNotCountTwice() {
        val subject = CollectionQualifier()
        subject.observe(snapshot(PlaybackStatus.BUFFERING, 0), 0)
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 40_000), 40_000))
        assertNotNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 40_000), 50_000))
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 40_000), 50_000))
    }

    @Test fun credibleEndToStartRepeatStartsNewOccurrence() {
        var sequence = 0
        val subject = CollectionQualifier { "play-${++sequence}" }
        subject.observe(snapshot(PlaybackStatus.PLAYING, 0, 108_000), 0)
        assertEquals("play-1", subject.observe(snapshot(PlaybackStatus.PLAYING, 10_000, 119_000), 10_000)?.occurrenceId)
        assertNull(subject.observe(snapshot(PlaybackStatus.PLAYING, 11_000, 0), 11_000))
        assertEquals("play-2", subject.observe(snapshot(PlaybackStatus.PLAYING, 21_000, 10_000), 21_000)?.occurrenceId)
    }
}
