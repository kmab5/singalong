package com.kmab.singalong.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsBackupTest {
    @Test fun `round trip retains reading preferences but not access or consent`() {
        val source = AppSettings(ready = true, trackingEnabled = true, translationEnabled = true,
            allowedPlayers = setOf("player"), wifiOnly = true, minimalMode = true,
            now = NowAppearance(alignment = LyricAlignment.END, timedTextSizeSp = 42f, wordTrackingEnabled = false))
        val restored = SettingsBackup.merge(AppSettings(), SettingsBackup.export(source))
        assertEquals(source.now, restored.now)
        assertTrue(restored.wifiOnly)
        assertTrue(restored.minimalMode)
        assertFalse(restored.trackingEnabled)
        assertFalse(restored.translationEnabled)
        assertTrue(restored.allowedPlayers.isEmpty())
    }

    @Test fun `partial legacy and malformed preferences preserve unrelated current values`() {
        val current = AppSettings(opacity = .7f, wifiOnly = true, now = NowAppearance(alignment = LyricAlignment.CENTER))
        val restored = SettingsBackup.merge(current, mapOf("textSize" to "28", "opacity" to "NaN",
            "wifiOnly" to "yes", "nowAlignment" to "unknown", "trackingEnabled" to "true"))
        assertEquals(28f, restored.floating.currentTextSizeSp, 0f)
        assertEquals(.7f, restored.opacity, 0f)
        assertTrue(restored.wifiOnly)
        assertEquals(LyricAlignment.CENTER, restored.now.alignment)
        assertFalse(restored.trackingEnabled)
    }
}
