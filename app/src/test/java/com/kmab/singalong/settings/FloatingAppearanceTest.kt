package com.kmab.singalong.settings

import org.junit.Assert.*
import org.junit.Test

class FloatingAppearanceTest {
    @Test fun sizesSanitizeIndependentlyWithoutChangingOtherChoices() {
        val settings = FloatingAppearance(Float.NaN, 100f, LyricAlignment.END, false).sanitized()
        assertEquals(22f, settings.currentTextSizeSp, 0f)
        assertEquals(32f, settings.otherTextSizeSp, 0f)
        assertEquals(LyricAlignment.END, settings.alignment)
        assertFalse(settings.wordTrackingEnabled)
        val small = FloatingAppearance(1f, Float.POSITIVE_INFINITY).sanitized()
        assertEquals(12f, small.currentTextSizeSp, 0f)
        assertEquals(14f, small.otherTextSizeSp, 0f)
    }
}
