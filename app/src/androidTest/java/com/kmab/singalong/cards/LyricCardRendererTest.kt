package com.kmab.singalong.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.Recording
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class LyricCardRendererTest {
    @Test fun albumPaletteUsesCoverEvenWhenHiddenAndIgnoresIndependentBackgroundPhoto() {
        val album = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val background = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            val style = CardStyle(palette = CardPalette.ARTWORK, background = CardBackground.SOLID, showCover = false)
            val colors = requireNotNull(CardArtworkColors.extract(album))
            val result = renderer.render(content, setOf(0), style, album, background, width = 240, artworkColors = colors)
            val bitmap = requireNotNull(result.bitmap)
            try { assertEquals(colors.base, bitmap.getPixel(0, 0)) } finally { bitmap.recycle() }
            album.eraseColor(Color.GREEN)
            val changed = requireNotNull(CardArtworkColors.extract(album))
            assertFalse(colors == changed)
            assertNotNull(renderer.render(content, setOf(0), style, width = 240).error)
        } finally { album.recycle(); background.recycle() }
    }
    private val renderer = LyricCardRenderer(ApplicationProvider.getApplicationContext<Context>())
    private val content = CardContent(
        Recording("card-test", "A song title", listOf("An artist"), "An album", null),
        listOf(LyricLine("first", null, "The first original line"), LyricLine("second", null, "The second original line")),
        "Original lyric source", 0,
    )

    @Test fun everyTemplateAndAspectRendersAtPreviewAndExportSize() {
        for (template in CardTemplate.entries) {
            for (aspect in CardAspect.entries) {
                val style = template.style().copy(aspect = aspect)
                for (width in listOf(480, aspect.width)) {
                    val result = renderer.render(content, setOf(0, 1), style, width = width)
                    assertNull("$template / $aspect / $width: ${result.error}", result.error)
                    val bitmap = requireNotNull(result.bitmap)
                    try {
                        assertEquals(width, bitmap.width)
                        assertEquals((width.toFloat() * aspect.height / aspect.width).roundToInt(), bitmap.height)
                        assertEquals(255, Color.alpha(bitmap.getPixel(0, 0)))
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test fun mixedDirectionsEmojiCombiningCharactersAndWrapsRenderWithEveryTypeface() {
        val lines = listOf(
            "مرحبا بالعالم — שיר חדש",
            "हिन्दी · 日本語 · café · e\u0301 · 🎵",
            "An original line that wraps naturally across the available card width without ellipsis.",
        )
        val multilingual = content.copy(lines = lines.mapIndexed { index, line -> LyricLine("$index", null, line) })
        for (font in CardFont.entries) {
            for (alignment in CardAlignment.entries) {
                val result = renderer.render(multilingual, setOf(0, 1, 2),
                    CardStyle(aspect = CardAspect.STORY, font = font, alignment = alignment), width = 480)
                assertNull("$font / $alignment: ${result.error}", result.error)
                assertNotNull(result.bitmap)
                result.bitmap?.recycle()
            }
        }
        assertEquals(lines.joinToString("\n"), multilingual.excerpt(setOf(2, 0, 1)))
    }

    @Test fun selectedTextThatCannotFitReturnsAnErrorAtBothResolutions() {
        val longContent = content.copy(lines = listOf(LyricLine("long", null, "Unchanged original text. ".repeat(150))))
        for (width in listOf(480, 1080)) {
            val result = renderer.render(longContent, setOf(0), CardStyle(fontSize = 112f), width = width)
            assertNull(result.bitmap)
            assertTrue(requireNotNull(result.error).contains("do not fit"))
        }
    }

    @Test fun unselectedLongLinesDoNotPreventRenderingTheSelectedExcerpt() {
        val selection = content.copy(lines = content.lines + LyricLine("unselected", null, "Too long ".repeat(1000)))
        val result = renderer.render(selection, setOf(0), CardStyle(), width = 480)
        assertNull(result.error)
        assertNotNull(result.bitmap)
        result.bitmap?.recycle()
        assertEquals("Too long ".repeat(1000), selection.lines.last().text)
    }

    @Test fun oversizedMetadataIsReportedInsteadOfDroppingCredits() {
        val result = renderer.render(content.copy(recording = content.recording.copy(title = "Original title ".repeat(100))),
            setOf(0), CardStyle(), width = 480)
        assertNull(result.bitmap)
        assertNotNull(result.error)
    }

    @Test fun customBackgroundTakesPriorityAndMissingBackgroundFallsBackToCover() {
        val cover = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val background = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val style = CardTemplate.ARTWORK.style().copy(background = CardBackground.IMAGE)
        try {
            val custom = renderer.render(content, setOf(0), style, cover, background, width = 480)
            assertNull(custom.error)
            val customBitmap = requireNotNull(custom.bitmap)
            try { assertTrue(Color.red(customBitmap.getPixel(0, 0)) > Color.green(customBitmap.getPixel(0, 0))) } finally { customBitmap.recycle() }
            val fallback = renderer.render(content, setOf(0), style, cover = cover, width = 480)
            assertNull(fallback.error)
            val fallbackBitmap = requireNotNull(fallback.bitmap)
            try { assertTrue(Color.green(fallbackBitmap.getPixel(0, 0)) > Color.red(fallbackBitmap.getPixel(0, 0))) } finally { fallbackBitmap.recycle() }
            assertFalse(cover.isRecycled)
            assertFalse(background.isRecycled)
            assertEquals(Color.GREEN, cover.getPixel(0, 0))
            assertEquals(Color.RED, background.getPixel(0, 0))
        } finally {
            cover.recycle()
            background.recycle()
        }
    }

    @Test fun photoVeilChangesOnlyRenderedOutputAndRecycledImagesAreIgnored() {
        val background = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val result = renderer.render(content, setOf(0), CardTemplate.ARTWORK.style().copy(background = CardBackground.IMAGE),
            background = background, width = 480)
        assertNull(result.error)
        val bitmap = requireNotNull(result.bitmap)
        try {
            assertTrue(Color.red(bitmap.getPixel(0, 0)) < 100)
            assertEquals(Color.WHITE, background.getPixel(0, 0))
        } finally {
            bitmap.recycle()
            background.recycle()
        }
        val recycled = renderer.render(content, setOf(0), CardTemplate.ARTWORK.style().copy(background = CardBackground.IMAGE), background = background, width = 480)
        assertNull(recycled.error)
        assertNotNull(recycled.bitmap)
        recycled.bitmap?.recycle()
    }

    @Test fun hiddenMetadataDoesNotCauseOverflowAndCoverCanBeOmitted() {
        val hidden = content.copy(recording = content.recording.copy(title = "Long title ".repeat(2000)))
        val result = renderer.render(hidden, setOf(0), CardStyle(details = CardDetails(title = false), showCover = false), width = 480)
        assertNull(result.error)
        assertNotNull(result.bitmap)
        result.bitmap?.recycle()
    }

    @Test fun largerArtworkAndNewTypeControlsRenderWithoutMutatingSourceImages() {
        val cover = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            for (template in CardTemplate.entries) for (aspect in CardAspect.entries) {
                val result = renderer.render(content, setOf(0), template.style().copy(aspect = aspect,
                    artScale = .42f, corners = CardCorners.ROUND, weight = CardWeight.BOLD,
                    spacing = CardSpacing.AIRY, placement = CardPlacement.BOTTOM, showAppName = true), cover, width = 480)
                assertNull("$template / $aspect: ${result.error}", result.error)
                assertNotNull(result.bitmap)
                result.bitmap?.recycle()
            }
            assertEquals(Color.BLUE, cover.getPixel(0, 0))
        } finally { cover.recycle() }
    }

    @Test fun invalidSelectionsAndWidthsDoNotAllocateAndExcessiveWidthsAreBounded() {
        assertNull(renderer.render(content, emptySet(), CardStyle()).bitmap)
        assertNotNull(renderer.render(content, setOf(0), CardStyle(), width = 0).error)
        val result = renderer.render(content, setOf(0), CardStyle(), width = Int.MAX_VALUE)
        assertNull(result.error)
        val bitmap = requireNotNull(result.bitmap)
        try { assertEquals(1080, bitmap.width) } finally { bitmap.recycle() }
    }

    @Test fun hidingTheForegroundCoverKeepsTheAlbumPhotoUntilACustomBackgroundIsChosen() {
        val cover = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val photo = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        try {
            val style = CardStyle(background = CardBackground.IMAGE)
            val visible = requireNotNull(renderer.render(content, setOf(0), style, cover, width = 240).bitmap)
            val hidden = requireNotNull(renderer.render(content, setOf(0), style.copy(showCover = false), cover, width = 240).bitmap)
            val custom = requireNotNull(renderer.render(content, setOf(0), style.copy(showCover = false), cover, photo, width = 240).bitmap)
            try {
                assertEquals(visible.getPixel(0, 0), hidden.getPixel(0, 0))
                assertFalse(hidden.getPixel(0, 0) == custom.getPixel(0, 0))
            } finally { visible.recycle(); hidden.recycle(); custom.recycle() }
        } finally { cover.recycle(); photo.recycle() }
    }

    @Test fun emphasisChangesOnlyTheSelectedExcerptAndSurvivesAllBackgroundChoices() {
        val style = CardStyle(details = CardDetails(false, false, false))
        val plain = requireNotNull(renderer.render(content, setOf(0), style, width = 240).bitmap)
        val emphasized = requireNotNull(renderer.render(content, setOf(0), style, width = 240, emphasized = setOf(0)).bitmap)
        val unselected = requireNotNull(renderer.render(content, setOf(0), style, width = 240, emphasized = setOf(1)).bitmap)
        try {
            assertFalse(plain.sameAs(emphasized))
            assertTrue(plain.sameAs(unselected))
            assertEquals("The first original line", content.excerpt(setOf(0)))
        } finally { plain.recycle(); emphasized.recycle(); unselected.recycle() }
        for (background in CardBackground.entries) {
            val result = renderer.render(content, setOf(0), style.copy(background = background), width = 240, emphasized = setOf(0))
            assertNull(result.error)
            result.bitmap?.recycle()
        }
    }
}
