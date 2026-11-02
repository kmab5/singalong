package com.kmab.singalong.app

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class FeedbackLinkTest {
    @Test fun feedbackPrefillsOnlyPublicAppContextAndEncodesVersion() {
        val url = FeedbackLink.url("0.3.1 test+1").toHttpUrl()
        assertEquals("https", url.scheme)
        assertEquals("kmab5.github.io", url.host)
        assertEquals("/kmab-feedback/", url.encodedPath)
        assertEquals("singalong", url.queryParameter("product"))
        assertEquals("android", url.queryParameter("source"))
        assertEquals("0.3.1 test+1", url.queryParameter("version"))
        assertEquals(setOf("product", "source", "version", "type", "ctx_screen"), url.queryParameterNames)
    }
}
