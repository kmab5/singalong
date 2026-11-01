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
}
