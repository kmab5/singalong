package com.kmab.singalong.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kmab.singalong.editor.TimingDraft
import com.kmab.singalong.model.*
import com.kmab.singalong.ui.theme.SingalongTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TimingEditorTest {
    @get:Rule val compose = createComposeRule()
    @Test fun manualTimingCanBeEnteredWithoutPlayback() {
        var result = TimingDraft(listOf(null, null))
        compose.setContent {
            var draft by remember { mutableStateOf(result) }
            val playback = remember { MutableStateFlow<PlaybackSnapshot?>(null) }
            SingalongTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                TimingEditor("First line\nSecond line", draft, "song", playback, true) { draft = it; result = it }
            } }
        }
        compose.onNodeWithText("Start time · mm:ss.mmm").performScrollTo().performTextInput("00:01.250")
        compose.onNodeWithContentDescription("Next lyric line").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1250L, result.times[0]); assertEquals(1, result.selectedIndex) }
        compose.onNodeWithText("stamp last line").assertIsNotEnabled()
    }
    @Test fun playbackStampAdvancesAndSongChangeDisablesStamping() {
        val recording = Recording("song", "Title", listOf("Artist"), null, 120_000)
        val snapshot = PlaybackSnapshot("session", "player", "Player", recording, PlaybackStatus.PAUSED, 12_000, 0, 1f)
        val playback = MutableStateFlow<PlaybackSnapshot?>(snapshot)
        var result = TimingDraft(listOf(null, null))
        compose.setContent {
            var draft by remember { mutableStateOf(result) }
            SingalongTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                TimingEditor("First line\nSecond line", draft, "song", playback, true) { draft = it; result = it }
            } }
        }
        compose.onNodeWithText("stamp & next line").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(12_000L, result.times[0]); assertEquals(1, result.selectedIndex)
            playback.value = snapshot.copy(recording = recording.copy(id = "other"))
        }
        compose.onNodeWithText("stamp last line").assertIsNotEnabled()
    }
}
