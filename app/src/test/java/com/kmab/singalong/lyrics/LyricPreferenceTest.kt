package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class LyricPreferenceTest {
    private val recording = Recording("r", "Song", listOf("Artist"), "Album", 120_000)
    private fun candidate(id: Long, duration: Double, timed: Boolean, album: String = "Album") =
        ProviderSong(id, "Song", "Artist", album, duration, plainLyrics = "Synthetic line",
            syncedLyrics = if (timed) "[00:01.00]Synthetic line" else null)

    @Test fun timedMatchesComeFirstAndClosestKnownDurationWinsWithinThem() {
        val versions = LyricsMatcher.versions(recording, listOf(
            candidate(1, 120.0, false), candidate(2, 126.0, true), candidate(3, 120.4, true), candidate(4, 0.0, true),
        ))
        assertEquals(listOf("3", "2", "4", "1"), versions.map { it.providerId })
    }

    @Test fun closestTimeWinsBeforeAlbumTieBreakerAndInputOrderDoesNotMatter() {
        val candidates = listOf(candidate(2, 121.9, true), candidate(1, 120.1, true, "Other album"))
        assertEquals("1", LyricsMatcher.versions(recording, candidates).first().providerId)
        assertEquals("1", LyricsMatcher.versions(recording, candidates.reversed()).first().providerId)
        assertTrue(LyricsMatcher.versions(recording, listOf(candidate(3, 190.0, true))).isEmpty())
    }

    @Test fun timestampsCanFollowWithoutPretendingAnUncertainMatchIsConfident() {
        val version = LyricsMatcher.versions(recording, listOf(candidate(1, 120.0, true, "Unknown album"))).single()
        val snapshot = PlaybackSnapshot("s", "player", "Player", recording, PlaybackStatus.PLAYING, 1000, 0, 1f)
        val reader = ReaderState(snapshot = snapshot, version = version)
        assertEquals(MatchConfidence.UNCERTAIN, version.confidence)
        assertTrue(reader.canSync)
        assertFalse(reader.experimental)
    }
}
