package com.kmab.singalong.translation

import com.kmab.singalong.lyrics.JsonHttpClient
import com.kmab.singalong.model.LyricVersion
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

fun interface LanguageDetector { suspend fun identify(text: String): String? }
interface SegmentCache {
    suspend fun get(language: String, text: String): String?
    suspend fun put(language: String, text: String, translation: String)
    /** Reserve before sending, including failed calls, so retries cannot exceed the anonymous allowance. */
    suspend fun reserve(characters: Int): Boolean
}

class MyMemoryProvider(
    private val detector: LanguageDetector,
    private val cache: SegmentCache,
    private val http: JsonHttpClient = JsonHttpClient(),
    private val baseUrl: String = "https://api.mymemory.translated.net/get",
    private val permitsNetwork: () -> Boolean = { true },
    private val now: () -> Long = System::currentTimeMillis,
) : TranslationProvider {
    override val id = "MyMemory"
    private val mutex = Mutex()
    private var blockedUntil = 0L
    override suspend fun translate(version: LyricVersion): TranslationResult = mutex.withLock {
        val originals = version.lines.filter { it.text.isNotBlank() }
        val language = detector.identify(originals.joinToString("\n") { it.text }.take(8000))
        if (language == null || language in setOf("und", "en") || !language.matches(Regex("[a-zA-Z-]{2,12}"))) return@withLock TranslationResult.Unsupported
        if (originals.any { it.text.toByteArray(Charsets.UTF_8).size > 500 }) return@withLock TranslationResult.Unsupported
        val translated = mutableMapOf<String, String>()
        try {
            for (line in originals) {
                currentCoroutineContext().ensureActive()
                val existing = cache.get(language, line.text)
                if (existing != null) { translated[line.id] = existing; continue }
                if (!permitsNetwork() || blockedUntil > now()) return@withLock TranslationResult.Unavailable
                if (!cache.reserve(line.text.codePointCount(0, line.text.length))) return@withLock TranslationResult.Unavailable
                val url = baseUrl.toHttpUrl().newBuilder().addQueryParameter("q", line.text)
                    .addQueryParameter("langpair", "$language|en").build()
                val response = http.get(url)
                val body = response.body
                if (response.code == 429 || (body?.get("quotaFinished") as? JsonPrimitive)?.booleanOrNull == true) {
                    blockedUntil = now() + (response.retryAfterMs ?: 86_400_000).coerceAtMost(Long.MAX_VALUE - now())
                    return@withLock TranslationResult.Unavailable
                }
                if (response.code !in 200..299 || (body?.get("responseStatus") as? JsonPrimitive)?.intOrNull != 200)
                    return@withLock TranslationResult.Failed
                val text = ((body["responseData"] as? JsonObject)?.get("translatedText") as? JsonPrimitive)?.contentOrNull
                if (text.isNullOrBlank() || text.length > 10_000) return@withLock TranslationResult.Failed
                // Store each completed segment, so interrupted songs and repeated choruses reuse work.
                cache.put(language, line.text, text)
                translated[line.id] = text
                delay(150)
            }
            TranslationResult.Available(TranslationRecord(version.id, version.contentHash, id, language, translated,
                "English translation: MyMemory / Translated · https://mymemory.translated.net"))
        } catch (_: IOException) { currentCoroutineContext().ensureActive(); TranslationResult.Failed }
        catch (_: SerializationException) { TranslationResult.Failed }
    }
    fun cancelRequests() = http.cancel()
}
