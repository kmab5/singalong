package com.kmab.singalong.overlay

import org.junit.Assert.*
import org.junit.Test

class OverlayStateTest {
    @Test fun onlyAutomaticMinimizationRestoresOnPlay() {
        val paused = OverlayState().show().playback(false, 100).playback(false, 15_100)
        assertEquals(PanelMode.AUTO_MINIMIZED, paused.mode)
        assertEquals(PanelMode.VISIBLE, paused.playback(true, 20_000).mode)
        assertEquals(PanelMode.MANUAL_MINIMIZED, paused.minimize().playback(true, 20_000).mode)
        assertEquals(PanelMode.HIDDEN, paused.close().playback(true, 20_000).mode)
    }
    @Test fun shortPauseDoesNotMinimize() {
        assertEquals(PanelMode.VISIBLE, OverlayState().show().playback(false, 0).playback(true, 14_999).mode)
    }
    @Test fun missingLyricsDockAfterGracePeriodAndWakeOnlyWithLyrics() {
        val missing = OverlayState().show().observe("one", true, "not_found", false, false, 100)
        assertEquals(PanelMode.VISIBLE, missing.observe("one", true, "not_found", false, false, 8099).mode)
        val docked = missing.observe("one", true, "not_found", false, false, 8100)
        assertEquals(PanelMode.AUTO_MINIMIZED, docked.mode)
        assertTrue(docked.needsLyrics)
        assertEquals(PanelMode.AUTO_MINIMIZED, docked.observe("two", true, "loading", false, false, 9000).mode)
        assertEquals(PanelMode.VISIBLE, docked.observe("two", true, "available", true, false, 9500).mode)
        assertEquals(PanelMode.MANUAL_MINIMIZED, docked.minimize().observe("two", true, "available", true, false, 9500).mode)
        assertEquals(PanelMode.HIDDEN, docked.close().observe("two", true, "available", true, false, 9500).mode)
    }
    @Test fun retryAndSongChangeResetMissingGracePeriodAndUnknownPlaybackIsNotAPause() {
        val missing = OverlayState().show().observe("one", true, "not_found", false, false, 0)
        val retry = missing.observe("one", true, "loading", false, true, 7000)
        assertNull(retry.missingSince)
        assertEquals(PanelMode.VISIBLE, retry.observe("two", true, "not_found", false, false, 9000).mode)
        assertNull(OverlayState().show().observe(null, null, "idle", false, false, 0).pauseSince)
    }
}
