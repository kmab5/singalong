package com.kmab.singalong.lyrics

import com.kmab.singalong.model.Recording
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

class HttpThreadingTest {
    // Models Android's main-thread network guard at the body cleanup boundary.
    // Even a 404 body may need socket reads when OkHttp closes an unread stream.
    private fun client(caller: Thread, code: Int, body: String, closed: AtomicBoolean): OkHttpClient =
        OkHttpClient.Builder().addInterceptor { chain ->
            val input = Buffer().writeUtf8(body)
            val guarded = object : ForwardingSource(input) {
                override fun close() {
                    check(Thread.currentThread() !== caller) { "Response body closed on the caller/UI thread" }
                    input.close()
                    closed.set(true)
                }
            }.buffer()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
                .body(object : ResponseBody() {
                    override fun contentType(): MediaType? = null
                    override fun contentLength() = body.length.toLong()
                    override fun source() = guarded
                }).build()
        }.build()

    @Test fun missingLyricsAndRetryResponsesCloseOffTheCallerThread() = runBlocking {
        for (code in listOf(404, 429, 503, 200)) {
            val closed = AtomicBoolean()
            val provider = LrclibClient(client(Thread.currentThread(), code, "[]", closed))
            val result = provider.search(Recording("r", "Synthetic missing song", listOf("Fixture"), null, null))
            assertTrue(result is ProviderResult.Songs || result is ProviderResult.Failed)
            assertTrue(closed.get())
        }
    }

    @Test fun jsonResponsesAlsoCloseOffTheCallerThread() = runBlocking {
        val closed = AtomicBoolean()
        val http = JsonHttpClient(client(Thread.currentThread(), 200, "{}", closed))
        assertEquals(200, http.get("https://fixture.invalid/".toHttpUrl()).code)
        assertTrue(closed.get())
    }

    @Test fun changingSongsCancelsBlockedBodyReadClosesOnWorkerAndAllowsNextLookup() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[]").setBodyDelay(2, TimeUnit.SECONDS))
            server.enqueue(MockResponse().setBody("[]"))
            val caller = Thread.currentThread()
            val reading = CompletableDeferred<Unit>()
            val closed = CompletableDeferred<Thread>()
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val original = response.body!!
                val guarded = object : ForwardingSource(original.source()) {
                    override fun read(sink: Buffer, byteCount: Long): Long {
                        reading.complete(Unit)
                        return super.read(sink, byteCount)
                    }
                    override fun close() { try { super.close() } finally { closed.complete(Thread.currentThread()) } }
                }.buffer()
                response.newBuilder().body(object : ResponseBody() {
                    override fun contentType() = original.contentType()
                    override fun contentLength() = original.contentLength()
                    override fun source() = guarded
                }).build()
            }.build()
            val provider = LrclibClient(client = http, baseUrl = server.url("/api/").toString())
            val song = Recording("a", "Synthetic first", listOf("Fixture"), null, null)
            val old = async { provider.search(song) }
            withTimeout(5000) { reading.await() }
            withTimeout(5000) { old.cancelAndJoin() }
            assertNotSame(caller, withTimeout(5000) { closed.await() })
            val next = withTimeout(5000) { provider.search(song.copy(id = "b", title = "Synthetic next")) }
            assertEquals(ProviderResult.Songs(emptyList()), next)
        }
    }
}
