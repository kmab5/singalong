package com.kmab.singalong.lyrics

import com.kmab.singalong.model.Recording
import com.kmab.singalong.model.MatchConfidence
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

/** Keep LRCLIB's timing and recording evidence when available; broaden plain-text coverage otherwise. */
class FallbackLyricsProvider(private val primary: LyricsProvider, private val fallback: LyricsProvider) : LyricsProvider {
    override suspend fun search(recording: Recording): ProviderResult {
        val first = primary.search(recording)
        if (first is ProviderResult.Songs && (LyricsMatcher.versions(recording, first.songs).isNotEmpty() ||
                first.songs.any { it.instrumental && LyricsMatcher.confidence(recording, it) == MatchConfidence.CONFIDENT })) return first
        val second = fallback.search(recording)
        if (second is ProviderResult.Songs && LyricsMatcher.versions(recording, second.songs).isNotEmpty()) return second
        // A failed source cannot establish that no lyrics exist. Retry only after both cooldowns permit it.
        val failures = listOf(first, second).filterIsInstance<ProviderResult.Failed>()
        return if (failures.isEmpty()) ProviderResult.Songs(emptyList()) else ProviderResult.Failed(
            failures.first().reason, failures.mapNotNull { it.retryAfterMs }.maxOrNull())
    }
    override fun cancelRequests() { primary.cancelRequests(); fallback.cancelRequests() }
}

class LyricsOvhClient(
    private val http: JsonHttpClient = JsonHttpClient(),
    private val baseUrl: String = "https://api.lyrics.ovh/v1/",
    private val permitsNetwork: () -> Boolean = { true },
    private val now: () -> Long = System::currentTimeMillis,
) : LyricsProvider {
    private val mutex = Mutex()
    private var blockedUntil = 0L
    override suspend fun search(recording: Recording): ProviderResult = mutex.withLock {
        if (!permitsNetwork()) return@withLock ProviderResult.Failed("offline")
        if (blockedUntil > now()) return@withLock ProviderResult.Failed("rate_limited", blockedUntil - now())
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegment(recording.artists.joinToString(", ")).addPathSegment(recording.title).build()
        try {
            val response = http.get(url)
            when {
                response.code == 404 -> ProviderResult.Songs(emptyList())
                response.code == 429 || response.code == 503 -> {
                    val wait = response.retryAfterMs ?: if (response.code == 429) 60_000L else 2_000L
                    blockedUntil = now() + wait.coerceAtMost(Long.MAX_VALUE - now())
                    ProviderResult.Failed(if (response.code == 429) "rate_limited" else "unavailable", wait)
                }
                response.code !in 200..299 -> ProviderResult.Failed("unavailable")
                else -> {
                    val lyrics = (response.body?.get("lyrics") as? JsonPrimitive)?.contentOrNull
                    if (lyrics.isNullOrBlank()) ProviderResult.Songs(emptyList()) else ProviderResult.Songs(listOf(
                        ProviderSong(0, recording.title, recording.artists.joinToString(", "), plainLyrics = lyrics,
                            source = "lyrics.ovh", sourceUrl = url.toString()),
                    ))
                }
            }
        } catch (_: IOException) { currentCoroutineContext().ensureActive(); ProviderResult.Failed("network_error") }
        catch (_: SerializationException) { ProviderResult.Failed("invalid_response") }
    }
    override fun cancelRequests() = http.cancel()
}
