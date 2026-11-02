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
    @Test fun rotationAndLargeFontsStayWithinAvailableSpace() {
        val safe = PanelBounds(40, 30, 500, 250)
        val actual = OverlayGeometry.bounds(OverlayState().show().copy(expanded = true),
            AppSettings(widthDp = 560f, heightDp = 640f, xFraction = 1f, yFraction = 1f), safe, 3f, 2f)
        assertEquals(safe, actual)
    }
}
