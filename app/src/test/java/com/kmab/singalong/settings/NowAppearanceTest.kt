package com.kmab.singalong.settings

import org.junit.Assert.*
import org.junit.Test

class NowAppearanceTest {
    @Test fun currentTimedSizeCanDifferFromOtherLinesAndIsSafelyBounded() {
        val appearance = NowAppearance(textSizeSp = 18f, timedTextSizeSp = 48f).sanitized()
        assertEquals(18f, appearance.textSizeSp, .001f)
        assertEquals(48f, appearance.timedTextSizeSp, .001f)
        assertEquals(56f, appearance.copy(timedTextSizeSp = 200f).sanitized().timedTextSizeSp, .001f)
        assertEquals(22f, appearance.copy(timedTextSizeSp = Float.NaN).sanitized().timedTextSizeSp, .001f)
    }
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
