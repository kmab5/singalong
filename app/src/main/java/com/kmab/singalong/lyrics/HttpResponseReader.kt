package com.kmab.singalong.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A response never crosses into the caller's dispatcher. Read AND close on OkHttp's worker. */
internal suspend fun <T> Call.readResponse(read: (Response) -> T): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { continuation.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) {
            val value = try {
                response.use {
                    if (!continuation.isActive) throw CancellationException("Request no longer needed")
                    read(it)
                }
            } catch (error: Exception) {
                // Transfer failures to the owning coroutine; never swallow a parser/programming error
                // or let it escape on OkHttp's callback thread. Providers classify expected failures.
                continuation.resumeWithException(error)
                return
            }
            continuation.resume(value)
        }
    })
}
