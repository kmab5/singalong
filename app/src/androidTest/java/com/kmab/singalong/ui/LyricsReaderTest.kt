package com.kmab.singalong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.*
import com.kmab.singalong.overlay.*
import com.kmab.singalong.settings.AppSettings
import com.kmab.singalong.ui.theme.SingalongTheme
import org.junit.Rule
import org.junit.Test

class LyricsReaderTest {
    @get:Rule val compose = createComposeRule()
    @Test fun timedSizeSettingDoesNotChangeAnUntimedReadersWrapping() {
        var timedSize by mutableFloatStateOf(20f)
        val words = "Synthetic untimed words that should keep their normal wrapping"
        val version = LyricVersion("v", "r", "h", LyricOrigin.PERSONAL,
            listOf(LyricLine("line", null, words)), null, null, MatchConfidence.CONFIDENT)
        compose.setContent { SingalongTheme {
            LyricsReader(ReaderState(version = version), Modifier.width(250.dp).height(600.dp),
                appearance = com.kmab.singalong.settings.NowAppearance(textSizeSp = 14f, timedTextSizeSp = timedSize))
        } }
        val before = compose.onNodeWithText(words).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { timedSize = 56f }
        compose.waitForIdle()
        org.junit.Assert.assertEquals(before, compose.onNodeWithText(words).fetchSemanticsNode().boundsInRoot)
    }
    @Test fun changingCurrentLineSizeDoesNotReflowItsLayoutOrLoseAccessibleText() {
        var current by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        val words = "Synthetic words long enough to wrap across lines"
        compose.setContent { SingalongTheme {
            Box(Modifier.width(220.dp)) {
                AnimatedLyricText(words, 14f, 30f, current, glow = true, gentle = false, animate = true,
                    progress = .6f, playing = false, style = androidx.compose.ui.text.TextStyle(color = com.kmab.singalong.ui.theme.SingalongColors.Text),
                    lineSpacing = 1.45f, alignment = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } }
        val before = compose.onNodeWithText(words).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { current = true }
        compose.mainClock.advanceTimeBy(320)
        val during = compose.onNodeWithText(words).fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(600)
        val after = compose.onNodeWithText(words).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(before, during)
        org.junit.Assert.assertEquals(before, after)
    }
    @Test fun narrowFloatingControlsAreAllVisibleWithoutHorizontalScrolling() {
        compose.setContent { SingalongTheme { Box(Modifier.width(160.dp).height(280.dp)) {
            FloatingLyricsPanel(OverlayState().show(), AppSettings(widthDp = 160f), ReaderState(),
                onOpen = {}, onDock = {}, onClose = {}, onExpand = {}, onFollow = {}, onRetry = {},
                onMinimal = {}, onReveal = {}, onSync = {}, onDragStart = {}, onMove = { _, _ -> }, onDragEnd = {})
        } } }
        listOf("Dock lyrics", "Try lyric following", "Minimal lyrics", "Expanded view", "Open Singalong").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed()
        }
    }
    @Test fun draggingDockedHandleMovesItWithoutOpeningAndTapStillOpens() {
        var opens = 0; var moved = 0f
        compose.setContent { SingalongTheme { Box(Modifier.width(48.dp).height(96.dp)) {
            FloatingLyricsPanel(OverlayState().show().minimize(), AppSettings(), ReaderState(),
                onOpen = { opens++ }, onDock = {}, onClose = {}, onExpand = {}, onFollow = {}, onRetry = {},
                onMinimal = {}, onReveal = {}, onSync = {}, onDragStart = {}, onMove = { x, y -> moved += kotlin.math.abs(x) + kotlin.math.abs(y) }, onDragEnd = {})
        } } }
        compose.onNodeWithContentDescription("Move floating lyrics").performTouchInput {
            swipe(center, center + Offset(150f, 80f), 500)
        }
        compose.runOnIdle { org.junit.Assert.assertTrue(moved > 0); org.junit.Assert.assertEquals(0, opens) }
        compose.onNodeWithContentDescription("Move floating lyrics").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, opens) }
    }
    @Test fun cachedOriginalIsReadableWithoutPlaybackTiming() {
        val version = LyricVersion("v", "r", "hash", LyricOrigin.PERSONAL,
            listOf(LyricLine("1", null, "Synthetic original words")), null, null, MatchConfidence.CONFIDENT)
        compose.setContent { SingalongTheme { LyricsReader(ReaderState(version = version), Modifier.fillMaxSize()) } }
        compose.onNodeWithText("Synthetic original words").assertIsDisplayed()
        compose.onNodeWithText("Scroll to read").assertIsDisplayed()
    }
    @Test fun unavailableStateOffersRetryWithoutInventingLyrics() {
        var retried = false
        compose.setContent { SingalongTheme { LyricsReader(ReaderState(status = "unavailable"), onRetry = { retried = true }) } }
        compose.onNodeWithText("Try again").performClick()
        org.junit.Assert.assertTrue(retried)
    }
    @Test fun minimalLyricsRevealAccessibleControlsWhenTapped() {
        val version = LyricVersion("v", "r", "hash", LyricOrigin.PERSONAL,
            listOf(LyricLine("1", null, "Synthetic original words")), null, null, MatchConfidence.CONFIDENT)
        compose.setContent {
            var panel by remember { mutableStateOf(OverlayState().show()) }
            SingalongTheme {
                FloatingLyricsPanel(panel, AppSettings(minimalMode = true), ReaderState(version = version),
                    onOpen = {}, onDock = {}, onClose = {}, onExpand = {}, onFollow = {}, onRetry = {},
                    onMinimal = { panel = panel.copy(controlsVisible = false) }, onReveal = { panel = panel.copy(controlsVisible = true) },
                    onSync = {}, onDragStart = {}, onMove = { _, _ -> }, onDragEnd = {})
            }
        }
        compose.onNodeWithContentDescription("Close floating lyrics").assertDoesNotExist()
        compose.onNodeWithText("Synthetic original words").performClick()
        compose.onNodeWithContentDescription("Close floating lyrics").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dock lyrics").assertIsDisplayed()
    }
    @Test fun expandedLyricsOpenAppAndOptionsAreDirectIcons() {
        var opened = false
        val version = LyricVersion("v", "r", "hash", LyricOrigin.PERSONAL,
            listOf(LyricLine("1", null, "Tap these synthetic words")), null, null, MatchConfidence.CONFIDENT)
        compose.setContent { SingalongTheme {
            FloatingLyricsPanel(OverlayState(expanded = true).show(), AppSettings(), ReaderState(version = version),
                onOpen = {}, onDock = {}, onClose = {}, onExpand = {}, onFollow = {}, onRetry = {},
                onMinimal = {}, onReveal = {}, onSync = {}, onDragStart = {}, onMove = { _, _ -> }, onDragEnd = {},
                onOpenApp = { opened = true })
        } }
        compose.onNodeWithContentDescription("Floating display options").assertDoesNotExist()
        compose.onNodeWithContentDescription("Minimal lyrics").assertExists()
        compose.onNodeWithContentDescription("Compact view").assertExists()
        compose.onNodeWithText("Tap these synthetic words").performClick()
        org.junit.Assert.assertTrue(opened)
    }
}
