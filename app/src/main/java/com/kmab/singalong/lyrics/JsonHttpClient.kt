package com.kmab.singalong.lyrics

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/** Bounded response bodies and socket cancellation, shared by the small public API adapters. */
class JsonHttpClient(private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()) {
    data class Result(val code: Int, val body: JsonObject?, val retryAfterMs: Long?)
    suspend fun get(url: HttpUrl): Result {
        val call = client.newCall(Request.Builder().url(url).header("User-Agent", "Singalong/0.3 (https://github.com/kmab5/singalong)").build())
        val response = suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (!continuation.isCancelled) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) { continuation.resume(response) { _, value, _ -> value.close() } }
            })
        }
        return response.use {
            withContext(Dispatchers.IO) {
                val bytes = it.body?.byteStream()?.let { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= 1_000_000) {
                        ensureActive()
                        val count = input.read(buffer, 0, minOf(buffer.size, 1_000_001 - output.size()))
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                ensureActive()
                val json = if (bytes != null && bytes.size <= 1_000_000 && it.isSuccessful)
                    Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)) as? JsonObject else null
                Result(it.code, json, retryAfterMillis(it.header("Retry-After"), System.currentTimeMillis()))
            }
        }
    }
    fun cancel() = client.dispatcher.cancelAll()
}
