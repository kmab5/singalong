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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import java.io.ByteArrayOutputStream

sealed interface ProviderResult {
    data class Songs(val songs: List<ProviderSong>) : ProviderResult
    data class Failed(val reason: String, val retryAfterMs: Long? = null) : ProviderResult
}

interface LyricsProvider { suspend fun search(recording: Recording): ProviderResult }

/** One paced request at a time, cancellable at the socket and coroutine boundaries. */
class LrclibClient(
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build(),
    private val baseUrl: String = "https://lrclib.net/api/",
    private val now: () -> Long = System::currentTimeMillis,
    private val permitsNetwork: () -> Boolean = { true },
) : LyricsProvider {
    private val mutex = Mutex()
    private var nextRequestAt = 0L
    @Volatile private var activeCall: Call? = null
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun search(recording: Recording): ProviderResult = mutex.withLock {
        delay((nextRequestAt - now()).coerceAtLeast(0))
        if (!permitsNetwork()) return@withLock ProviderResult.Failed("offline")
        val url = (baseUrl + "search").toHttpUrl().newBuilder()
            .addQueryParameter("track_name", recording.title)
            .addQueryParameter("artist_name", recording.artists.joinToString(", ")).build()
        val call = client.newCall(Request.Builder().url(url)
            .header("User-Agent", "Singalong/0.1.0 (https://github.com/kmab5/singalong)").build())
        activeCall = call
        try {
            call.await().use { response ->
                withContext(Dispatchers.IO) {
                    val it = response
                    if (it.code == 429 || it.code == 503) {
                        val retry = retryDelay(it.header("Retry-After")) ?: 60_000L
                        nextRequestAt = now() + retry.coerceAtMost(Long.MAX_VALUE - now())
                        return@withContext ProviderResult.Failed(if (it.code == 429) "rate_limited" else "unavailable", retry)
                    }
                    if (it.code == 404) return@withContext ProviderResult.Songs(emptyList())
                    if (!it.isSuccessful) return@withContext ProviderResult.Failed("unavailable")
                    val body = it.body ?: return@withContext ProviderResult.Failed("invalid_response")
                    val input = body.byteStream()
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= 2_000_000) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - output.size()))
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    val bytes = output.toByteArray()
                    if (bytes.size > 2_000_000) return@withContext ProviderResult.Failed("invalid_response")
                    ProviderResult.Songs(json.decodeFromString<List<ProviderSong>>(bytes.toString(Charsets.UTF_8)))
                }
            }
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

    fun cancelRequests() { activeCall?.cancel(); client.dispatcher.cancelAll() }

    private fun retryDelay(value: String?): Long? {
        value ?: return null
        value.toLongOrNull()?.let { return it.coerceIn(0, Long.MAX_VALUE / 1000) * 1000 }
        return try { (ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - now()).coerceAtLeast(0) }
        catch (_: DateTimeParseException) { null }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ -> value.close() }
            }
        })
    }
}
