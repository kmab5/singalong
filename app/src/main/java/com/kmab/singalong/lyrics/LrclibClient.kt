package com.kmab.singalong.lyrics

import com.kmab.singalong.model.Recording
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

sealed interface ProviderResult {
    data class Songs(val songs: List<ProviderSong>) : ProviderResult
    data class Failed(val reason: String, val retryAfterMs: Long? = null) : ProviderResult
}

interface LyricsProvider {
    suspend fun search(recording: Recording): ProviderResult
    fun cancelRequests() = Unit
}

/** One paced request at a time, cancellable at the socket and coroutine boundaries. */
class LrclibClient(
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build(),
    private val baseUrl: String = "https://lrclib.net/api/",
    private val now: () -> Long = System::currentTimeMillis,
    private val permitsNetwork: () -> Boolean = { true },
) : LyricsProvider {
    private val mutex = Mutex()
    private var nextRequestAt = 0L
    private var blockedUntil = 0L
    private var blockedReason = "rate_limited"
    @Volatile private var activeCall: Call? = null
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun search(recording: Recording): ProviderResult = mutex.withLock {
        if (blockedUntil > now()) return@withLock ProviderResult.Failed(blockedReason, blockedUntil - now())
        delay((nextRequestAt - now()).coerceIn(0, 350))
        if (!permitsNetwork()) return@withLock ProviderResult.Failed("offline")
        val url = (baseUrl + "search").toHttpUrl().newBuilder()
            .addQueryParameter("track_name", recording.title)
            .addQueryParameter("artist_name", recording.artists.joinToString(", ")).build()
        val call = client.newCall(Request.Builder().url(url)
            .header("User-Agent", "Singalong/0.1.0 (https://github.com/kmab5/singalong)").build())
        activeCall = call
        try {
            val result = call.readResponse {
                if (it.code == 429 || it.code == 503) {
                    val retry = retryAfterMillis(it.header("Retry-After"), now()) ?: if (it.code == 429) 60_000L else 2_000L
                    return@readResponse ProviderResult.Failed(if (it.code == 429) "rate_limited" else "unavailable", retry)
                }
                if (it.code == 404) return@readResponse ProviderResult.Songs(emptyList())
                if (!it.isSuccessful) return@readResponse ProviderResult.Failed("unavailable")
                val body = it.body ?: return@readResponse ProviderResult.Failed("invalid_response")
                val input = body.byteStream()
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 2_000_000) {
                    if (call.isCanceled()) throw IOException("Request canceled")
                    val count = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                if (bytes.size > 2_000_000) return@readResponse ProviderResult.Failed("invalid_response")
                ProviderResult.Songs(json.decodeFromString<List<ProviderSong>>(bytes.toString(Charsets.UTF_8)))
            }
            // Only the coroutine holding the mutex updates cooldown state. A canceled callback
            // may still be closing its socket after the next song has acquired the mutex.
            if (result is ProviderResult.Failed && result.retryAfterMs != null) {
                blockedUntil = now() + result.retryAfterMs.coerceAtMost(Long.MAX_VALUE - now())
                blockedReason = result.reason
            }
            result
        } catch (_: IOException) {
            currentCoroutineContext().ensureActive()
            ProviderResult.Failed("network_error")
        } catch (_: SerializationException) {
            ProviderResult.Failed("invalid_response")
        } finally {
            activeCall = null
            nextRequestAt = maxOf(nextRequestAt, now() + 350)
        }
    }

    override fun cancelRequests() { activeCall?.cancel(); client.dispatcher.cancelAll() }

}
