package com.kmab.singalong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnimatedLyricTextTest {
    @get:Rule val compose = createComposeRule()

    @Test fun glowDoesNotPaintHiddenWordsOrTheirShadows() {
        val sentence = mutableStateOf("Alpha beta gamma delta epsilon\nzeta eta theta")
        val progress = mutableFloatStateOf(.15f)
        val current = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.width(260.dp)) {
                AnimatedLyricText(
                    original = sentence.value, normalSize = 20f, currentSize = 28f,
                    current = current.value, glow = true, gentle = false, animate = true,
                    progress = progress.floatValue, playing = false,
                    style = TextStyle(color = Color.White), lineSpacing = 1.45f,
                    alignment = TextAlign.Center,
                    modifier = Modifier.background(Color(0xFF404040)),
                )
            }
        }
        // Exercise multiple masks, soft wrapping, an explicit newline, and RTL shaping.
        for (text in listOf(sentence.value, "مرحبا بالعالم هذه كلمات\nللتجربة فقط")) {
            compose.runOnIdle { sentence.value = text; current.value = true }
            compose.mainClock.advanceTimeBy(700)
            for (fraction in listOf(.15f, .6f, 1f)) {
                compose.runOnIdle { progress.floatValue = fraction }
                compose.mainClock.advanceTimeByFrame()
                assertVisibleTextWithoutBlackCopies(text, expectGlow = true)
            }
            compose.runOnIdle { current.value = false }
            compose.mainClock.advanceTimeBy(320)
            assertVisibleTextWithoutBlackCopies(text, expectGlow = false)
            compose.mainClock.advanceTimeBy(400)
            assertVisibleTextWithoutBlackCopies(text, expectGlow = false)
        }
    }

    private fun assertVisibleTextWithoutBlackCopies(text: String, expectGlow: Boolean) {
        val pixels = compose.onNodeWithText(text).captureToImage().toPixelMap()
        var brightPixels = 0
        var purplePixels = 0
        var darkPixels = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val pixel = pixels[x, y]
            // White glyphs and purple glow can only brighten this neutral background.
            if (minOf(pixel.red, pixel.green, pixel.blue) < 62f / 255f) darkPixels++
            if (minOf(pixel.red, pixel.green, pixel.blue) > .85f) brightPixels++
            if (pixel.blue - pixel.green > .08f) purplePixels++
        }
        assertTrue("Hidden glyphs/shadows painted $darkPixels dark pixels", darkPixels == 0)
        assertTrue("Visible words must still be drawn", brightPixels > 20)
        if (expectGlow) assertTrue("The visible words must retain their purple glow", purplePixels > 20)
    }
}
