package com.kmab.singalong.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kmab.singalong.app.FoundationState
import com.kmab.singalong.ui.theme.SingalongTheme
import org.junit.Rule
import org.junit.Test

class FoundationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun browsingLibraryDoesNotOfferFabricatedSavedSongs() {
        compose.setContent { SingalongTheme { FoundationScreen(FoundationState(), emptyList(), PermissionStatus(), FoundationActions()) } }
        compose.onNodeWithText("library", useUnmergedTree = true).performClick()
        compose.onNodeWithText("nothing saved yet").assertIsDisplayed()
    }

    @Test fun missingMusicAccessCannotStartTracking() {
        compose.setContent { SingalongTheme { FoundationScreen(FoundationState(), emptyList(), PermissionStatus(), FoundationActions()) } }
        compose.onNodeWithText("start tracking").performScrollTo().assertIsNotEnabled()
    }
}
