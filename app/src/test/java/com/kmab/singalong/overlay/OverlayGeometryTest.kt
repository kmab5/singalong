package com.kmab.singalong.overlay

import com.kmab.singalong.settings.AppSettings
import org.junit.Assert.*
import org.junit.Test

class OverlayGeometryTest {
    @Test fun firstOpenHasUsableBoundsWithoutASecondRender() {
        val safe = PanelBounds(0, 72, 1080, 2100)
        val actual = OverlayGeometry.bounds(OverlayState().show(), AppSettings(), safe, 3f)
        assertTrue(actual.width >= 780)
        assertTrue(actual.height >= 600)
        assertTrue(actual.x + actual.width <= safe.width)
        assertTrue(actual.y >= safe.y)
    }
    @Test fun dockingKeepsAFullTouchTargetAtEitherSafeEdge() {
        val safe = PanelBounds(20, 40, 1000, 1500)
        val panel = OverlayState().show().minimize()
        val left = OverlayGeometry.bounds(panel, AppSettings(xFraction = .2f), safe, 2f)
        val right = OverlayGeometry.bounds(panel, AppSettings(xFraction = .8f), safe, 2f)
        assertEquals(safe.x, left.x)
        assertEquals(safe.x + safe.width, right.x + right.width)
        assertTrue(left.width >= 96)
        assertTrue(left.height > left.width)
    }
    @Test fun rotationStaysWithinAvailableSpace() {
        val safe = PanelBounds(40, 30, 500, 250)
        val actual = OverlayGeometry.bounds(OverlayState().show().copy(expanded = true),
            AppSettings(widthDp = 560f, heightDp = 640f, xFraction = 1f, yFraction = 1f), safe, 3f)
        assertEquals(safe, actual)
    }
    @Test fun minimalModeHonorsSmallBoundsAndRevealingControlsDoesNotOverwriteThem() {
        val safe = PanelBounds(0, 40, 1080, 2200)
        val settings = AppSettings(minimalMode = true, widthDp = 160f, minimalHeightDp = 56f, heightDp = 120f)
        val panel = OverlayState().show()
        val minimal = OverlayGeometry.bounds(panel, settings, safe, 3f)
        assertEquals(480, minimal.width)
        assertEquals(168, minimal.height)
        val controls = OverlayGeometry.bounds(panel.copy(controlsVisible = true), settings, safe, 3f)
        assertEquals(minimal.width, controls.width)
        assertTrue(controls.height >= 660)
        assertEquals(minimal, OverlayGeometry.bounds(panel, settings, safe, 3f))
    }
    @Test fun normalSmallPanelLeavesRoomForHeaderToolbarAndRetryTouchTarget() {
        val bounds = OverlayGeometry.bounds(OverlayState().show(), AppSettings(widthDp = 160f, heightDp = 120f), PanelBounds(0, 0, 600, 1200), 1f)
        assertTrue(bounds.height - 48 - 48 >= 96)
        assertEquals(160, bounds.width)
    }
}
