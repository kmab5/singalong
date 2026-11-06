package com.kmab.singalong.cards

import org.junit.Assert.*
import org.junit.Test

class ArtworkPaletteTest {
    @Test fun predominantColorWinsOverASmallBrightDetail() {
        val red = 0xffd02030.toInt(); val blue = 0xff3070d0.toInt()
        val pixels = IntArray(100) { if (it < 90) red else blue }
        val result = requireNotNull(ArtworkPalette.fromPixels(pixels))
        assertTrue((result.base ushr 16 and 255) > (result.base and 255))
        assertTrue((result.accent and 255) > (result.accent ushr 16 and 255))
        assertEquals(result, ArtworkPalette.fromPixels(pixels.reversedArray()))
        assertEquals(red, pixels.first())
    }
    @Test fun missingAndTransparentArtworkDoesNotInventAPalette() {
        assertNull(ArtworkPalette.fromPixels(intArrayOf()))
        assertNull(ArtworkPalette.fromPixels(intArrayOf(0x00ff0000, 0x7fffffff)))
        assertEquals(ArtworkPalette.fromPixels(intArrayOf(0xff00aa00.toInt())),
            ArtworkPalette.fromPixels(intArrayOf(0x00ff0000, 0xff00aa00.toInt())))
    }
    @Test fun grayscaleAndSaturatedImagesProduceReadableOpaqueColors() {
        for (red in 0..255 step 51) for (green in 0..255 step 51) for (blue in 0..255 step 51) {
            val result = requireNotNull(ArtworkPalette.fromPixels(intArrayOf((255 shl 24) or (red shl 16) or (green shl 8) or blue)))
            assertTrue(ArtworkPalette.contrast(result.ink, result.base) >= 7)
            assertTrue(ArtworkPalette.contrast(result.accent, result.base) >= 4.5)
            assertEquals(255, result.base ushr 24)
        }
    }
}
