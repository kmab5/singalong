package com.kmab.singalong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
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
}
