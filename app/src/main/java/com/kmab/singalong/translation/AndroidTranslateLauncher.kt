package com.kmab.singalong.translation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.TransactionTooLargeException
import androidx.core.net.toUri
import com.kmab.singalong.model.LyricLine

/** Called by the foreground activity, so clipboard access precedes the external launch. */
class AndroidTranslateLauncher(private val context: Context) {
    fun open(lines: List<LyricLine>): GoogleTranslateHandoff.Result =
        GoogleTranslateHandoff.open(lines, ::copy, ::openUrl)

    private fun copy(text: String): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        return try {
            clipboard.setPrimaryClip(ClipData.newPlainText("Singalong lyrics", text))
            true
        } catch (_: SecurityException) {
            false
        } catch (failure: RuntimeException) {
            // ClipboardManager wraps a failed Binder transaction in a runtime exception.
            if (failure.cause is TransactionTooLargeException) false else throw failure
        }
    }

    internal fun openUrl(url: String): Boolean {
        val browser = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            // Resolve a browser rather than a domain-associated app that may discard text.
            // The selected browser receives the outer ACTION_VIEW and the complete URL.
            selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        }
        return launch(browser) || launch(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
