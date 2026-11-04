package com.kmab.singalong.translation

import com.kmab.singalong.model.LyricLine

/** External apps acknowledge launch, not whether they used our text. Copy before leaving. */
object GoogleTranslateHandoff {
    data class Result(val copied: Boolean, val opened: Boolean, val message: String)

    fun open(lines: List<LyricLine>, copy: (String) -> Boolean, launch: (String) -> Boolean): Result {
        val text = lines.joinToString("\n") { it.text }
        if (text.isBlank()) return Result(false, false, "No lyrics to translate yet.")
        // Bound UTF-16 clipboard transport well below Binder's shared transaction limit.
        // Refuse oversized input instead of silently copying only part of a song.
        val copied = text.length <= 200_000 && copy(text)
        val link = GoogleTranslateLink.from(lines)
        if (!copied && link == null) return Result(false, false,
            "Could not copy these lyrics. Export the text to translate it in sections.")
        val opened = launch(link?.url ?: "https://translate.google.com/?sl=auto&tl=en&op=translate")
        val message = when {
            copied && opened -> "Lyrics copied. Paste if Google Translate opens empty."
            copied -> "Lyrics copied. No app could open Translate; paste them in a translator."
            opened -> "Translate opened, but copying failed. If empty, return and export the lyrics."
            else -> "Could not open Translate or copy lyrics. Export the text to translate it."
        }
        return Result(copied, opened, message)
    }
}
