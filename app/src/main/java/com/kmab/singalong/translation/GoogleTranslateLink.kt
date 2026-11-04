package com.kmab.singalong.translation

import com.kmab.singalong.model.LyricLine
import okhttp3.HttpUrl.Companion.toHttpUrl

/** An explicit external handoff; this never changes the saved lyrics or translations. */
data class GoogleTranslateLink(val url: String, val exceedsInputLimit: Boolean) {
    companion object {
        fun from(lines: List<LyricLine>): GoogleTranslateLink? {
            // Bound the intent before allocating an encoded URL. Never send a partial song.
            if (lines.sumOf { it.text.length.toLong() + 1 } > 64_000) return null
            val text = lines.joinToString("\n") { it.text }
            if (text.isBlank()) return null
            val url = "https://translate.google.com/".toHttpUrl().newBuilder()
                .addQueryParameter("sl", "auto").addQueryParameter("tl", "en")
                .addQueryParameter("text", text).addQueryParameter("op", "translate")
                .build().toString()
            // Conservative bound for browser/Android intent transport, including UTF-8 escaping.
            if (url.length > 64_000) return null
            return GoogleTranslateLink(url, text.codePointCount(0, text.length) > 5_000)
        }
    }
}
