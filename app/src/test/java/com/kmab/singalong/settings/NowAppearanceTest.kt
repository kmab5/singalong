package com.kmab.singalong.settings

import org.junit.Assert.*
import org.junit.Test

class NowAppearanceTest {
    @Test fun invalidImportedOrStoredSizesCannotBreakTheReader() {
        val invalid = NowAppearance(Float.NaN, LyricAlignment.END, LyricAnimation.NONE, Float.POSITIVE_INFINITY, false, false).sanitized()
        assertEquals(22f, invalid.textSizeSp, .001f)
        assertEquals(1.45f, invalid.lineSpacing, .001f)
        assertEquals(LyricAlignment.END, invalid.alignment)
        assertEquals(LyricAnimation.NONE, invalid.animation)
        assertEquals(40f, NowAppearance(textSizeSp = 100f).sanitized().textSizeSp, .001f)
        assertEquals(1.1f, NowAppearance(lineSpacing = 0f).sanitized().lineSpacing, .001f)
    }
}
