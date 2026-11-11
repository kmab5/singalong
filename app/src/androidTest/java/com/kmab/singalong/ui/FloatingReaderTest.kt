package com.kmab.singalong.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kmab.singalong.lyrics.LyricMoment
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.*
import com.kmab.singalong.settings.FloatingAppearance
import com.kmab.singalong.translation.TranslationRecord
import com.kmab.singalong.ui.theme.SingalongTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FloatingReaderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disablingWordsOrManualReadingRemovesHighlightsAndKeepsTranslation() {
        var enabled by mutableStateOf(true)
        var following by mutableStateOf(true)
        val line = LyricLine("line", 0, "Original words", endMs = 2000,
            words = listOf(LyricWord("Original ", 0, 1000), LyricWord("words", 1000, 2000)))
        val version = LyricVersion("v", "r", "h", LyricOrigin.PERSONAL, listOf(line), null, null, MatchConfidence.CONFIDENT)
        val snapshot = PlaybackSnapshot("s", "p", "Player", Recording("r", "Title", listOf("Artist"), null, 10000),
            PlaybackStatus.PAUSED, 1500, 0, 1f)
        val translation = TranslationRecord("v", "h", "test", "es", mapOf("line" to "Translated words"), "test")
        compose.setContent { SingalongTheme {
            LyricsReader(ReaderState(version = version, snapshot = snapshot, translation = translation,
                following = following, moment = LyricMoment(index = 0)), Modifier.width(340.dp).height(500.dp),
                compact = true, floatingAppearance = FloatingAppearance(wordTrackingEnabled = enabled))
        } }
        fun spans() = compose.onNodeWithText("Original words").fetchSemanticsNode().config[SemanticsProperties.Text].single().spanStyles
        assertEquals(2, spans().size)
        compose.runOnIdle { enabled = false }
        compose.waitForIdle()
        assertTrue(spans().isEmpty())
        compose.onNodeWithText("Translated words").assertIsDisplayed()
        compose.runOnIdle { enabled = true; following = false }
        compose.waitForIdle()
        assertTrue(spans().isEmpty())
        compose.onNodeWithText("Follow lyrics").assertIsDisplayed()
        compose.onNodeWithText("Translated words").assertIsDisplayed()
    }

    @Test fun compactCurrentAndUpcomingSizesChangeIndependently() {
        var appearance by mutableStateOf(FloatingAppearance(currentTextSizeSp = 14f, otherTextSizeSp = 14f))
        val version = LyricVersion("v", "r", "h", LyricOrigin.PERSONAL, listOf(
            LyricLine("current", 0, "Current words"), LyricLine("next", 2000, "Upcoming words"),
        ), null, null, MatchConfidence.CONFIDENT)
        val snapshot = PlaybackSnapshot("s", "p", "Player", Recording("r", "Title", listOf("Artist"), null, 10000),
            PlaybackStatus.PAUSED, 1000, 0, 1f)
        compose.setContent { SingalongTheme {
            LyricsReader(ReaderState(version = version, snapshot = snapshot, moment = LyricMoment(index = 0)),
                Modifier.width(340.dp).height(500.dp), compact = true, floatingAppearance = appearance)
        } }
        fun height(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.height
        val currentBefore = height("Current words")
        val otherBefore = height("Upcoming words")
        compose.runOnIdle { appearance = appearance.copy(currentTextSizeSp = 30f) }
        compose.waitForIdle()
        assertTrue(height("Current words") > currentBefore)
        assertEquals(otherBefore, height("Upcoming words"), .1f)
        val currentAfter = height("Current words")
        compose.runOnIdle { appearance = appearance.copy(otherTextSizeSp = 26f) }
        compose.waitForIdle()
        assertTrue(height("Upcoming words") > otherBefore)
        assertEquals(currentAfter, height("Current words"), .1f)
    }
}
