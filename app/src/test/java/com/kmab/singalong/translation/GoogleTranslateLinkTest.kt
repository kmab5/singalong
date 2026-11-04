package com.kmab.singalong.translation

import com.kmab.singalong.model.LyricLine
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class GoogleTranslateLinkTest {
    @Test fun sendsEveryOriginalLineWithoutTimestampsAndPreservesUnicodeAndQueryCharacters() {
        val text = "İyi & güzel + #?\n\nمرحبا 世界"
        val lines = text.split('\n').mapIndexed { i, line -> LyricLine("$i", i * 1000L, line) }
        val link = GoogleTranslateLink.from(lines)!!
        val url = link.url.toHttpUrl()
        assertEquals("https", url.scheme)
        assertEquals("translate.google.com", url.host)
        assertEquals(text, url.queryParameter("text"))
        assertEquals("auto", url.queryParameter("sl"))
        assertEquals("en", url.queryParameter("tl"))
        assertEquals("translate", url.queryParameter("op"))
        assertFalse(link.exceedsInputLimit)
    }

    @Test fun longerLyricsAreFlaggedButNeverSilentlyTruncated() {
        val text = "a".repeat(5001)
        val link = GoogleTranslateLink.from(listOf(LyricLine("1", null, text)))!!
        assertTrue(link.exceedsInputLimit)
        assertEquals(text, link.url.toHttpUrl().queryParameter("text"))
    }

    @Test fun emptyLyricsDoNotOpenAnEmptyTranslation() {
        assertNull(GoogleTranslateLink.from(emptyList()))
        assertNull(GoogleTranslateLink.from(listOf(LyricLine("1", null, " \n"))))
    }

    @Test fun oversizedEncodedLinksAreRejectedInsteadOfTruncatedOrSentThroughBinder() {
        assertNull(GoogleTranslateLink.from(listOf(LyricLine("1", null, "世".repeat(8000)))))
    }
}
