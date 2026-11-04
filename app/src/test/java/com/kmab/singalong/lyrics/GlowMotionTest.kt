package com.kmab.singalong.lyrics

import org.junit.Assert.*
import org.junit.Test

class GlowMotionTest {
    @Test fun unhighlightedWordsStayStillAndLayoutDoesNotDependOnTheCurrentLine() {
        val still = GlowMotion.transform(3, 5f, 0f)
        assertEquals(0f, still.xDp, 0f); assertEquals(0f, still.yDp, 0f); assertEquals(1f, still.scale, 0f)
        assertEquals(GlowMotion.reservedSize(14f, 20f), GlowMotion.reservedSize(20f, 14f), .001f)
    }
    @Test fun highlightedWordsLiftAndGrowWithoutAbruptFrameChanges() {
        for (frame in 0..600) {
            val before = GlowMotion.transform(2, frame / 60f, 1f)
            val after = GlowMotion.transform(2, (frame + 1) / 60f, 1f)
            assertTrue(before.yDp in -3.2f..-1.2f)
            assertTrue(before.scale in 1f..1.06f)
            assertTrue(kotlin.math.abs(after.xDp - before.xDp) < .04f)
            assertTrue(kotlin.math.abs(after.yDp - before.yDp) < .04f)
        }
        assertTrue(GlowMotion.transform(2, 0f, .5f).scale < GlowMotion.transform(2, 0f, 1f).scale)
    }
}
