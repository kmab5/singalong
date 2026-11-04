package com.kmab.singalong.translation

import com.kmab.singalong.model.LyricLine
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class GoogleTranslateHandoffTest {
    private fun lines(text: String) = text.split('\n').mapIndexed { i, line -> LyricLine("$i", i * 1000L, line) }

    @Test fun clipboardIsReadyBeforeAReceiverOpensAndSilentlyDiscardsTheText() {
        val original = "İyi & güzel + #?\n\nمرحبا 世界"
        var clipboard: String? = null
        val result = GoogleTranslateHandoff.open(lines(original), copy = { clipboard = it; true }, launch = { url ->
            assertEquals(original, clipboard)
            assertEquals(original, url.toHttpUrl().queryParameter("text"))
            assertEquals("en", url.toHttpUrl().queryParameter("tl"))
            // A receiver can accept the intent but ignore its input, as reported on the phone.
            true
        })
        assertTrue(result.copied)
        assertTrue(result.opened)
        assertEquals(original, clipboard)
    }

    @Test fun missingBrowserLeavesCompleteLyricsAvailableForManualPaste() {
        var clipboard = ""
        val result = GoogleTranslateHandoff.open(lines("One\n\nTwo"), { clipboard = it; true }, { false })
        assertTrue(result.copied)
        assertFalse(result.opened)
        assertEquals("One\n\nTwo", clipboard)
    }

    @Test fun clipboardFailureStillAttemptsFullTextTransportAndReportsFailure() {
        var input: String? = null
        val result = GoogleTranslateHandoff.open(lines("One\nTwo"), { false }, {
            input = it.toHttpUrl().queryParameter("text"); true
        })
        assertEquals("One\nTwo", input)
        assertFalse(result.copied)
        assertTrue(result.opened)
    }

    @Test fun oversizedUrlCopiesEveryCharacterAndOpensEnglishTargetPageForPasting() {
        val original = "世".repeat(8000)
        var clipboard = ""
        val result = GoogleTranslateHandoff.open(lines(original), { clipboard = it; true }, {
            assertNull(it.toHttpUrl().queryParameter("text"))
            assertEquals("en", it.toHttpUrl().queryParameter("tl"))
            true
        })
        assertTrue(result.copied)
        assertTrue(result.opened)
        assertEquals(original, clipboard)
    }

    @Test fun blankLyricsLeaveClipboardAndOtherAppsAlone() {
        val result = GoogleTranslateHandoff.open(lines(" \n"), { error("Must not copy") }, { error("Must not open") })
        assertFalse(result.copied)
        assertFalse(result.opened)
    }

    @Test fun contentTooLargeForEitherTransportIsNotTruncatedOrSentToBinder() {
        val result = GoogleTranslateHandoff.open(lines("x".repeat(200_001)),
            { error("Must not risk an oversized clipboard transaction") }, { error("No text can be sent") })
        assertFalse(result.copied)
        assertFalse(result.opened)
    }

    @Test fun noUsableUrlAndFailedClipboardDoesNotOpenAnEmptyPage() {
        val result = GoogleTranslateHandoff.open(lines("世".repeat(8000)), { false }, { error("No text can be sent") })
        assertFalse(result.copied)
        assertFalse(result.opened)
    }
}
