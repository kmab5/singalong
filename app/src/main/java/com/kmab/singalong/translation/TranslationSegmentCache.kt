package com.kmab.singalong.translation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest

/** Private bounded working cache; completed translations also live with their lyric version in Room. */
class TranslationSegmentCache(context: Context, private val now: () -> Long = System::currentTimeMillis) : SegmentCache {
    private val prefs = context.getSharedPreferences("translation_segments", Context.MODE_PRIVATE)
    private fun key(language: String, text: String) = "line_" + MessageDigest.getInstance("SHA-256")
        .digest("$language:$text".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    override suspend fun get(language: String, text: String): String? = withContext(Dispatchers.IO) { prefs.getString(key(language, text), null) }
    override suspend fun put(language: String, text: String, translation: String) = withContext(Dispatchers.IO) {
        val edit = prefs.edit()
        if (prefs.all.size > 1000) prefs.all.keys.filter { it.startsWith("line_") }.take(200).forEach { edit.remove(it) }
        if (!edit.putString(key(language, text), translation).commit()) throw IOException("Translation cache unavailable")
    }
    @android.annotation.SuppressLint("UseKtx") // commit's boolean is needed to fail closed when quota persistence fails.
    override suspend fun reserve(characters: Int): Boolean = withContext(Dispatchers.IO) {
        val day = now() / 86_400_000
        val used = if (prefs.getLong("day", -1) == day) prefs.getInt("used", 0) else 0
        if (characters > 5000 - used) false else {
            if (!prefs.edit().putLong("day", day).putInt("used", used + characters).commit()) throw IOException("Translation quota unavailable")
            true
        }
    }
}
