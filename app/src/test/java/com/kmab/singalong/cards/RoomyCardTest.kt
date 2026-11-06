package com.kmab.singalong.cards

import org.junit.Assert.*
import org.junit.Test

class RoomyCardTest {
    @Test fun artIsLargeAndAllVisibleRegionsRemainSeparateAcrossChoices() {
        for (template in CardTemplate.entries) for (aspect in CardAspect.entries) for (art in listOf(.20f, .34f, .387f, .46f)) {
            val style = template.style().copy(aspect = aspect, artScale = art)
            val layout = CardComposition.create(style, hasCover = true)
            val cover = requireNotNull(layout.cover)
            val short = minOf(aspect.width, aspect.height)
            assertTrue("$template / $aspect: cover too small", minOf(cover.width * aspect.width, cover.height * aspect.height) >= short * .16f)
            val regions = listOf(layout.lyrics, layout.credits, cover)
            assertTrue(regions.all { it.x >= 0 && it.y >= 0 && it.right <= 1.001f && it.bottom <= 1.001f })
            regions.forEachIndexed { i, region -> regions.drop(i + 1).forEach { other ->
                assertFalse("$template / $aspect / $art: $region overlaps $other", region.overlaps(other))
            } }
            val without = CardComposition.create(style, hasCover = false)
            assertNull(without.cover)
            // Removing art switches to full-width text; a small side cover can have taller text
            // than that stacked layout, so total area need not increase for every aspect ratio.
            assertTrue(without.lyrics.width >= layout.lyrics.width)
        }
    }

    @Test fun footerAlignmentUsesTheSameInsetAndNeverChangesTheMainLayout() {
        val start = CardComposition.create(CardStyle(logoPosition = CardLogoPosition.BOTTOM_LEFT), true)
        val end = CardComposition.create(CardStyle(logoPosition = CardLogoPosition.BOTTOM_RIGHT), true)
        assertEquals(start.lyrics, end.lyrics)
        assertEquals(start.cover, end.cover)
        assertEquals(start.brand.x, 1f - end.brand.right, .0001f)
        assertEquals(start.brand.y, end.brand.y)
    }

    @Test fun sizeAndPhotoControlsAreBoundedEvenWhenRestoredWithInvalidValues() {
        val style = CardStyle(fontSize = Float.NaN, photoShade = Float.POSITIVE_INFINITY).sanitized()
        assertTrue(style.fontSize.isFinite())
        assertTrue(style.photoShade.isFinite())
        assertEquals(180f, style.copy(fontSize = 999f).sanitized().fontSize)
        assertEquals(.65f, style.copy(photoShade = -5f).sanitized().photoShade)
    }
}
