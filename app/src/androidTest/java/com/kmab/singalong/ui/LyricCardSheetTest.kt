package com.kmab.singalong.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import com.kmab.singalong.cards.CardContent
import com.kmab.singalong.cards.CardRequest
import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.Recording
import com.kmab.singalong.ui.theme.SingalongTheme
import org.junit.Rule
import org.junit.Test

class LyricCardSheetTest {
    @get:Rule val compose = createComposeRule()

    private fun request() = CardRequest(
        CardContent(
            Recording("recording", "Original song", listOf("Original artist"), "Original album", null),
            listOf(LyricLine("first", null, "First original line"), LyricLine("second", null, "Second original line")),
            "Original source", initialLine = 0,
        ), id = "frozen-card",
    )

    @Test fun editorStepTemplateAndExcerptSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val request = request()
        restoration.setContent { SingalongTheme { LyricCardSheet(request, onDismiss = {}) } }

        compose.onNodeWithText("Share lyric card").performClick()
        compose.onNodeWithText("Margin").performScrollTo().performClick()
        compose.onNodeWithText("Lines").performClick()
        compose.onNodeWithText("Second original line").performClick()
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Your lyric card").assertExists()
        compose.onNodeWithText("Lines").assertIsSelected()
        compose.onNodeWithText("First original line").assertIsOn()
        compose.onNodeWithText("Second original line").assertIsOn()
        compose.onNodeWithText("2 selected · original wording and order").assertExists()
    }

    @Test fun emptyExcerptImmediatelyDisablesExportsAndExplainsHowToContinue() {
        compose.setContent { SingalongTheme { LyricCardSheet(request(), onDismiss = {}) } }
        compose.onNodeWithText("Share lyric card").performClick()
        compose.onNodeWithText("Lines").performClick()
        compose.onNodeWithText("First original line").performClick()

        compose.onNodeWithText("Share PNG").assertIsNotEnabled()
        compose.onNodeWithText("Save PNG").assertIsNotEnabled()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Select at least one lyric line.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun previewStaysVisibleWhileDetailsScrollAndMetadataCanBeHidden() {
        compose.setContent { SingalongTheme { LyricCardSheet(request(), onDismiss = {}) } }
        compose.onNodeWithText("Share lyric card").performClick()
        compose.onNodeWithText("Details").performClick()
        compose.onNodeWithText("Song title").performClick()
        compose.onNodeWithText("App name").performScrollTo().performClick()
        compose.onNodeWithTag("card-preview").assertIsDisplayed()
        compose.onNodeWithText("Share PNG").assertIsDisplayed()
    }

    @Test fun scrollingPastBothEndsDoesNotMoveThePreviewOrExportActions() {
        compose.setContent { SingalongTheme { LyricCardSheet(request(), onDismiss = {}) } }
        compose.onNodeWithText("Share lyric card").performClick()
        val preview = compose.onNodeWithTag("card-preview").fetchSemanticsNode().boundsInRoot
        val export = compose.onNodeWithText("Share PNG").fetchSemanticsNode().boundsInRoot
        repeat(5) { compose.onNodeWithTag("card-controls").performTouchInput { swipeUp() } }
        compose.waitForIdle()
        assertEquals(preview, compose.onNodeWithTag("card-preview").fetchSemanticsNode().boundsInRoot)
        assertEquals(export, compose.onNodeWithText("Share PNG").fetchSemanticsNode().boundsInRoot)
        repeat(5) { compose.onNodeWithTag("card-controls").performTouchInput { swipeDown() } }
        compose.waitForIdle()
        assertEquals(preview, compose.onNodeWithTag("card-preview").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test fun longLinePickerStartsNearThePressedLineAndKeepsPreviewVisible() {
        val base = request()
        val long = base.copy(content = base.content.copy(initialLine = 700,
            lines = List(1200) { LyricLine("$it", null, "Original row $it") }))
        compose.setContent { SingalongTheme { LyricCardSheet(long, onDismiss = {}) } }
        compose.onNodeWithText("Share lyric card").performClick()
        compose.onNodeWithText("Lines").performClick()
        compose.onNodeWithText("Original row 698").assertIsDisplayed()
        compose.onNodeWithTag("card-lines").performScrollToIndex(1199)
        compose.onNodeWithText("Original row 1199").assertIsDisplayed()
        compose.onNodeWithText("Original row 0").assertDoesNotExist()
        compose.onNodeWithTag("card-preview").assertIsDisplayed()
    }

    @Test fun changingTheIncomingSongDoesNotReplaceAnOpenDraft() {
        val incoming = mutableStateOf(request())
        compose.setContent { SingalongTheme { LyricCardSheet(incoming.value, onDismiss = {}) } }
        compose.onNodeWithText("Share lyric card").performClick()
        compose.runOnIdle {
            val original = incoming.value
            incoming.value = original.copy(content = original.content.copy(
                recording = original.content.recording.copy(title = "Next playing song"),
                lines = listOf(LyricLine("next", null, "Next song words")),
            ))
        }
        compose.onNodeWithText("Original song").assertExists()
        compose.onNodeWithText("Lines").performClick()
        compose.onNodeWithText("First original line").assertIsOn()
        compose.onNodeWithText("Next playing song").assertDoesNotExist()
        compose.onNodeWithText("Next song words").assertDoesNotExist()
    }
}
