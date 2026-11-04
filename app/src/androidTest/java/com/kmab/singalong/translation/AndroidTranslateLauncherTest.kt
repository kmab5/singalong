package com.kmab.singalong.translation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test

class AndroidTranslateLauncherTest {
    private val url = "https://translate.google.com/?sl=auto&tl=en&text=Bonjour%0AMerci&op=translate"

    @Test fun prefersBrowserButRetriesTheSameFullUrlWhenNoBrowserSelectorResolves() {
        val attempts = mutableListOf<Intent>()
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun startActivity(intent: Intent) {
                attempts += intent
                if (intent.selector != null) throw ActivityNotFoundException()
            }
        }
        assertTrue(AndroidTranslateLauncher(context).openUrl(url))
        assertEquals(2, attempts.size)
        assertTrue(attempts.first().selector!!.hasCategory(Intent.CATEGORY_APP_BROWSER))
        assertEquals(Intent.ACTION_VIEW, attempts.first().action)
        assertNull(attempts.last().selector)
        attempts.forEach {
            assertEquals("Bonjour\nMerci", it.data!!.getQueryParameter("text"))
            assertEquals("en", it.data!!.getQueryParameter("tl"))
        }
    }

    @Test fun noAllowedHandlerReturnsFailureInsteadOfCrashing() {
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun startActivity(intent: Intent) { throw SecurityException("Blocked by test") }
        }
        assertFalse(AndroidTranslateLauncher(context).openUrl(url))
    }
}
