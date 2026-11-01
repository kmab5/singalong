package com.kmab.singalong.tracking

import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class PlayerSelectorTest {
    private fun player(id: String, status: PlaybackStatus) = PlaybackSnapshot(id, id, id, null, status, null, 0, 1f)
    @Test fun excludedPlayersCannotBeSelectedEvenByOverride() {
        val selector = PlayerSelector()
        assertNull(selector.select(listOf(player("browser", PlaybackStatus.PLAYING)), setOf("music"), "browser"))
    }
    @Test fun newPlayingTransitionWinsButOrdinaryPauseKeepsCurrentPlayer() {
        val selector = PlayerSelector()
        val allowed = setOf("a", "b")
        assertEquals("a", selector.select(listOf(player("a", PlaybackStatus.PLAYING), player("b", PlaybackStatus.PAUSED)), allowed, null)?.sessionId)
        assertEquals("b", selector.select(listOf(player("a", PlaybackStatus.PLAYING), player("b", PlaybackStatus.PLAYING)), allowed, null)?.sessionId)
        assertEquals("b", selector.select(listOf(player("a", PlaybackStatus.PLAYING), player("b", PlaybackStatus.PAUSED)), allowed, null)?.sessionId)
    }
    @Test fun initialTieUsesPlatformOrderAndOverrideIsHonored() {
        val selector = PlayerSelector()
        val sessions = listOf(player("a", PlaybackStatus.PLAYING), player("b", PlaybackStatus.PLAYING))
        assertEquals("a", selector.select(sessions, setOf("a", "b"), null)?.sessionId)
        assertEquals("b", selector.select(sessions, setOf("a", "b"), "b")?.sessionId)
    }
    @Test fun returningToAutomaticUsesNewestPlayerObservedDuringOverride() {
        val selector = PlayerSelector()
        val allowed = setOf("a", "b")
        selector.select(listOf(player("a", PlaybackStatus.PLAYING), player("b", PlaybackStatus.PAUSED)), allowed, "a")
        val both = listOf(player("a", PlaybackStatus.PLAYING), player("b", PlaybackStatus.PLAYING))
        assertEquals("a", selector.select(both, allowed, "a")?.sessionId)
        assertEquals("b", selector.select(both, allowed, null)?.sessionId)
    }
}
