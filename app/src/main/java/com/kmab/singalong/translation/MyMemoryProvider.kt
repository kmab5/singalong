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
    suspend fun reserve(characters: Int): Boolean
}

/** Two in-flight segments at most; incremental display is separate from complete-version persistence. */
class MyMemoryProvider(
    private val detector: LanguageDetector,
    private val cache: SegmentCache,
    private val http: JsonHttpClient = JsonHttpClient(),
    private val baseUrl: String = "https://api.mymemory.translated.net/get",
    private val permitsNetwork: () -> Boolean = { true },
    private val now: () -> Long = System::currentTimeMillis,
) : ProgressiveTranslationProvider {
    override val id = "MyMemory"
    private val mutex = Mutex()
    private val cacheMutex = Mutex()
    @Volatile private var blockedUntil = 0L
    override suspend fun translate(version: LyricVersion) = translateProgressively(version, true) {}
    override suspend fun translateProgressively(version: LyricVersion, online: Boolean,
        onProgress: suspend (TranslationRecord) -> Unit): TranslationResult = mutex.withLock {
        val originals = version.lines.filter { it.text.isNotBlank() }
        val language = detector.identify(originals.joinToString("\n") { it.text }.take(8000))
        if (language == null || language in setOf("und", "en") || !language.matches(Regex("[a-zA-Z-]{2,12}"))) return@withLock TranslationResult.Unsupported
        val groups = originals.groupBy { it.text }
        val translated = mutableMapOf<String, String>()
        fun record() = TranslationRecord(version.id, version.contentHash, id, language, translated.toMap(),
            "English translation: MyMemory / Translated · https://mymemory.translated.net")
        val publishMutex = Mutex()
        suspend fun publish(text: String, meaning: String) = publishMutex.withLock {
            groups.getValue(text).forEach { translated[it.id] = meaning }
            onProgress(record())
        }
        try {
            val missing = mutableListOf<String>()
            // Paint every cached segment first, including ones after a previously interrupted request.
            for (text in groups.keys) {
                val cached = cache.get(language, text)
                if (cached != null) groups.getValue(text).forEach { translated[it.id] = cached } else missing += text
            }
            if (translated.isNotEmpty()) onProgress(record())
            if (missing.isEmpty()) return@withLock TranslationResult.Available(record())
            if (!online || !permitsNetwork()) return@withLock TranslationResult.Unavailable
            for (chunk in missing.chunked(2)) {
                currentCoroutineContext().ensureActive()
                val outcomes = coroutineScope {
                    chunk.map { text -> async {
                        val outcome = translateSegment(language, text)
                        outcome.text?.let { publish(text, it) }
                        outcome
                    } }.awaitAll()
                }
                outcomes.firstNotNullOfOrNull { it.failure }?.let { return@withLock it }
                delay(150)
            }
            TranslationResult.Available(record())
        } catch (_: IOException) { currentCoroutineContext().ensureActive(); TranslationResult.Failed }
        catch (_: SerializationException) { TranslationResult.Failed }
    }

    private data class SegmentResult(val text: String? = null, val failure: TranslationResult? = null)
    private suspend fun translateSegment(language: String, text: String): SegmentResult {
        if (text.toByteArray(Charsets.UTF_8).size > 500) return SegmentResult(failure = TranslationResult.Unsupported)
        if (!permitsNetwork() || blockedUntil > now()) return SegmentResult(failure = TranslationResult.Unavailable)
        val reserved = cacheMutex.withLock { cache.reserve(text.codePointCount(0, text.length)) }
        if (!reserved) return SegmentResult(failure = TranslationResult.Unavailable)
        val url = baseUrl.toHttpUrl().newBuilder().addQueryParameter("q", text).addQueryParameter("langpair", "$language|en").build()
        return try {
            val response = http.get(url)
            val body = response.body
            if (response.code == 429 || (body?.get("quotaFinished") as? JsonPrimitive)?.booleanOrNull == true) {
                blockedUntil = now() + (response.retryAfterMs ?: 86_400_000).coerceAtMost(Long.MAX_VALUE - now())
                SegmentResult(failure = TranslationResult.Unavailable)
            } else if (response.code !in 200..299 || (body?.get("responseStatus") as? JsonPrimitive)?.intOrNull != 200) {
                SegmentResult(failure = TranslationResult.Failed)
            } else {
                val translated = ((body["responseData"] as? JsonObject)?.get("translatedText") as? JsonPrimitive)?.contentOrNull
                if (translated.isNullOrBlank() || translated.length > 10_000) SegmentResult(failure = TranslationResult.Failed)
                else {
                    cacheMutex.withLock { cache.put(language, text, translated) }
                    SegmentResult(text = translated)
                }
            }
        } catch (_: IOException) { currentCoroutineContext().ensureActive(); SegmentResult(failure = TranslationResult.Failed) }
        catch (_: SerializationException) { SegmentResult(failure = TranslationResult.Failed) }
    }
    fun cancelRequests() = http.cancel()
}
