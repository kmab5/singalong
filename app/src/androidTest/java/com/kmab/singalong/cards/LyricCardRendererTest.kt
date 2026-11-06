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
                    CardStyle(aspect = CardAspect.STORY, font = font, alignment = alignment, textWidth = .55f), width = 480)
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
        val style = CardTemplate.ARTWORK.style().copy(imageShade = 0f)
        try {
            val custom = renderer.render(content, setOf(0), style, cover, background, width = 480)
            assertNull(custom.error)
            val customBitmap = requireNotNull(custom.bitmap)
            try { assertEquals(Color.RED, customBitmap.getPixel(0, 0)) } finally { customBitmap.recycle() }
            val fallback = renderer.render(content, setOf(0), style, cover = cover, width = 480)
            assertNull(fallback.error)
            val fallbackBitmap = requireNotNull(fallback.bitmap)
            try { assertEquals(Color.GREEN, fallbackBitmap.getPixel(0, 0)) } finally { fallbackBitmap.recycle() }
            assertFalse(cover.isRecycled)
            assertFalse(background.isRecycled)
            assertEquals(Color.GREEN, cover.getPixel(0, 0))
            assertEquals(Color.RED, background.getPixel(0, 0))
        } finally {
            cover.recycle()
            background.recycle()
        }
    }

    @Test fun imageShadeChangesOnlyRenderedOutputAndRecycledImagesAreIgnored() {
        val background = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val result = renderer.render(content, setOf(0), CardTemplate.ARTWORK.style().copy(imageShade = .8f),
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
        val recycled = renderer.render(content, setOf(0), CardTemplate.ARTWORK.style(), background = background, width = 480)
        assertNull(recycled.error)
        assertNotNull(recycled.bitmap)
        recycled.bitmap?.recycle()
    }

    @Test fun extremePositionsAndSanitizedControlsStillRenderWithinTheCard() {
        val cover = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            for (position in listOf(0f, 1f, Float.NaN)) {
                val result = renderer.render(content, setOf(0), CardStyle(
                    textX = position, textY = position, coverX = position, coverY = position,
                    coverSize = Float.POSITIVE_INFINITY, textWidth = Float.NaN,
                ), cover = cover, width = 480)
                assertNull(result.error)
                assertNotNull(result.bitmap)
                result.bitmap?.recycle()
            }
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
}
