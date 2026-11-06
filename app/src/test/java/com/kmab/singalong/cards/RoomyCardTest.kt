package com.kmab.singalong.cards

import org.junit.Assert.*
import org.junit.Test

class RoomyCardTest {
    @Test fun artIsLargeAndAllVisibleRegionsRemainSeparateAcrossChoices() {
        for (template in CardTemplate.entries) for (aspect in CardAspect.entries) for (art in CardArtSize.entries) {
            val style = template.style().copy(aspect = aspect, artSize = art)
            val layout = CardComposition.create(style, hasCover = true)
            val cover = requireNotNull(layout.cover)
            val short = minOf(aspect.width, aspect.height)
            assertTrue("$template / $aspect: cover too small", minOf(cover.width * aspect.width, cover.height * aspect.height) >= short * .30f)
            val regions = listOf(layout.lyrics, layout.credits, cover, layout.brand)
            assertTrue(regions.all { it.x >= 0 && it.y >= 0 && it.right <= 1.001f && it.bottom <= 1.001f })
            regions.forEachIndexed { i, region -> regions.drop(i + 1).forEach { other ->
                assertFalse("$template / $aspect / $art: $region overlaps $other", region.overlaps(other))
            } }
            val without = CardComposition.create(style, hasCover = false)
            assertNull(without.cover)
            assertTrue(without.lyrics.width * without.lyrics.height >= layout.lyrics.width * layout.lyrics.height)
        }
    }

    @Test fun footerAlignmentUsesTheSameInsetAndNeverChangesTheMainLayout() {
        val start = CardComposition.create(CardStyle(footer = CardFooter.START), true)
        val end = CardComposition.create(CardStyle(footer = CardFooter.END), true)
        assertEquals(start.lyrics, end.lyrics)
        assertEquals(start.cover, end.cover)
        assertEquals(start.brand.x, 1f - end.brand.right, .0001f)
        assertEquals(start.brand.y, end.brand.y)
    }

    @Test fun sizeAndPhotoControlsAreBoundedEvenWhenRestoredWithInvalidValues() {
        val style = CardStyle(fontSize = Float.NaN, photoShade = Float.POSITIVE_INFINITY).sanitized()
        assertTrue(style.fontSize.isFinite())
        assertTrue(style.photoShade.isFinite())
        assertEquals(112f, style.copy(fontSize = 999f).sanitized().fontSize)
        assertEquals(.65f, style.copy(photoShade = -5f).sanitized().photoShade)
    }
}
