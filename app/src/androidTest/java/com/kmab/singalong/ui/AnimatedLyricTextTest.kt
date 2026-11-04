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

    @Test fun disablingGlowStopsWordClockReadsAndEstimatedWordMotion() {
        val glow = mutableStateOf(true)
        val progress = mutableFloatStateOf(.1f)
        val useWordTimes = mutableStateOf(true)
        val clockReads = java.util.concurrent.atomic.AtomicInteger()
        val clock: () -> Long = { clockReads.incrementAndGet(); 2000L }
        val words = listOf(com.kmab.singalong.model.LyricWord("Alpha ", 0, 3000),
            com.kmab.singalong.model.LyricWord("beta", 3000, 6000))
        val playback = com.kmab.singalong.model.PlaybackSnapshot("s", "p", "p", null,
            com.kmab.singalong.model.PlaybackStatus.PLAYING, 0, 0, 1f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.width(260.dp)) {
                AnimatedLyricText("Alpha beta", 20f, 28f, current = true, glow = glow.value,
                    gentle = false, animate = true, progress = progress.floatValue, playing = true,
                    style = TextStyle(color = Color.White), lineSpacing = 1.45f, alignment = TextAlign.Center,
                    modifier = Modifier.background(Color(0xFF404040)),
                    wordTimings = if (useWordTimes.value) words else emptyList(),
                    playback = playback, wordLineEndMs = 6000, elapsedMs = clock)
            }
        }
        compose.mainClock.advanceTimeBy(100)
        assertTrue(clockReads.get() > 0)
        compose.runOnIdle { glow.value = false }
        compose.mainClock.advanceTimeBy(800)
        val stoppedReads = clockReads.get()
        val before = compose.onNodeWithText("Alpha beta").captureToImage().toPixelMap()
        compose.runOnIdle { useWordTimes.value = false; progress.floatValue = .9f }
        compose.mainClock.advanceTimeBy(1000)
        org.junit.Assert.assertEquals(stoppedReads, clockReads.get())
        val after = compose.onNodeWithText("Alpha beta").captureToImage().toPixelMap()
        for (y in 0 until before.height) for (x in 0 until before.width) {
            org.junit.Assert.assertEquals("Disabled word tracking must leave text still", before[x, y], after[x, y])
        }
        compose.runOnIdle { useWordTimes.value = true; glow.value = true }
        compose.mainClock.advanceTimeBy(100)
        assertTrue(clockReads.get() > stoppedReads)
    }

    @Test fun suppliedWordTimesOverrideEstimatedProgressAndFollowPausedSeeks() {
        val progress = mutableFloatStateOf(0f)
        val playback = mutableStateOf(com.kmab.singalong.model.PlaybackSnapshot("s", "p", "p", null,
            com.kmab.singalong.model.PlaybackStatus.PAUSED, 1000, 0, 1f))
        val words = listOf(com.kmab.singalong.model.LyricWord("Alpha ", 1000, 1500),
            com.kmab.singalong.model.LyricWord("beta", 4000, 6000))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.width(260.dp)) {
                AnimatedLyricText("Alpha beta", 20f, 28f, current = true, glow = true,
                    gentle = false, animate = true, progress = progress.floatValue, playing = false,
                    style = TextStyle(color = Color.White), lineSpacing = 1.45f, alignment = TextAlign.Center,
                    modifier = Modifier.background(Color(0xFF404040)), wordTimings = words,
                    playback = playback.value, wordLineEndMs = 6000, elapsedMs = { 0 })
            }
        }
        val before = compose.onNodeWithText("Alpha beta").captureToImage().toPixelMap()
        compose.runOnIdle { progress.floatValue = 1f }
        compose.mainClock.advanceTimeBy(500)
        val after = compose.onNodeWithText("Alpha beta").captureToImage().toPixelMap()
        for (y in 0 until before.height) for (x in 0 until before.width) {
            org.junit.Assert.assertEquals("Estimated progress must not move word-synced highlights", before[x, y], after[x, y])
        }
        compose.runOnIdle { playback.value = playback.value.copy(positionMs = 5000) }
        compose.mainClock.advanceTimeByFrame()
        assertVisibleTextWithoutBlackCopies("Alpha beta", expectGlow = true)
    }

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
