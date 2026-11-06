package com.kmab.singalong.cards

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.Recording
import org.junit.Assert.*
import org.junit.Test

class CardControlsTest {
    private val content = CardContent(Recording("r", "A song title", listOf("Artist"), null, null),
        List(150) { LyricLine("$it", null, "Line $it") }, null, 0)

    @Test fun filenamesUseSourceLineNumbersAndPreserveDisjointSelections() {
        assertEquals("A-song-title-3-singalong.png", content.pngFileName(setOf(2)))
        assertEquals("A-song-title-3-5,8-singalong.png", content.pngFileName(setOf(7, 3, 2, 4, -1, 999)))
        assertEquals("A-song-title-1-singalong.png", content.pngFileName(emptySet()))
    }

    @Test fun filenamesCannotEscapeTheDestinationAndAreBoundedInUtf8() {
        val unsafe = content.copy(recording = content.recording.copy(title = "../a/b\\c:\"*?<>|\n"))
        val name = unsafe.pngFileName(setOf(0))
        assertFalse(name.contains('/'))
        assertFalse(name.contains('\\'))
        assertFalse(name.startsWith('.'))
        val long = content.copy(recording = content.recording.copy(title = "🎵曲".repeat(200)))
            .pngFileName((0 until 150 step 2).toSet())
        assertTrue(long.toByteArray(Charsets.UTF_8).size < 255)
        assertTrue(long.endsWith("-singalong.png"))
        assertFalse(long.contains('\uFFFD'))
    }

    @Test fun onlyRowsBesideTheLogoLoseWidthOnEitherPhysicalSide() {
        assertEquals(0f to 0f, CardTextExclusion.insets(10f, 100f, 20f, 30f, 75f, 80f, 100f, 100f))
        assertEquals(0f to 25f, CardTextExclusion.insets(10f, 100f, 75f, 85f, 75f, 80f, 100f, 100f))
        assertEquals(20f to 0f, CardTextExclusion.insets(10f, 100f, 75f, 85f, 10f, 80f, 30f, 100f))
        assertEquals(0f to 0f, CardTextExclusion.insets(40f, 100f, 75f, 85f, 10f, 80f, 30f, 100f))
    }

    @Test fun logoUsesACornerFootprintWithoutReservingAFooterAndSizingIsContinuous() {
        for (position in CardLogoPosition.entries) {
            val style = CardStyle(logoPosition = position, showAppName = true, logoScale = .074f,
                details = CardDetails(false, false, false))
            val result = CardComposition.create(style, false)
            assertEquals(.92f, result.lyrics.bottom, .001f)
            assertEquals(if (position.left) .08f else .92f - result.brand.width, result.brand.x, .001f)
            assertEquals(if (position.top) .08f else .92f - result.brand.height, result.brand.y, .001f)
            assertTrue(result.lyrics.overlaps(result.brand))
        }
        val smaller = CardComposition.create(CardStyle(artScale = .371f), true).cover!!
        val bigger = CardComposition.create(CardStyle(artScale = .372f), true).cover!!
        assertTrue(bigger.width > smaller.width)
        assertTrue(bigger.height > smaller.height)
    }

    @Test fun invalidRestoredSizesAreFiniteAndBounded() {
        val restored = CardStyle(artScale = Float.NaN, logoScale = Float.POSITIVE_INFINITY).sanitized()
        assertTrue(restored.artScale.isFinite() && restored.logoScale.isFinite())
        assertEquals(.20f, restored.copy(artScale = -1f).sanitized().artScale)
        assertEquals(.10f, restored.copy(logoScale = 30f).sanitized().logoScale)
    }

    @Test fun previousDraftFieldsDoNotCrashRestorationOrLeakRemovedDetails() {
        val restored = CardStyleState.decode("""{"fontSize":110.0,"artSize":"LARGE","footer":"START",
            "details":{"title":false,"artist":true,"album":false,"fileName":true,"location":true}}""")
        assertEquals(110f, restored.fontSize)
        assertEquals(listOf("Artist"), content.metadata(restored.details).map { it.second })
        assertEquals(restored, CardStyleState.decode(CardStyleState.encode(restored)))
    }
}
