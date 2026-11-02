package com.kmab.singalong.lyrics

import com.kmab.singalong.model.Recording
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class LrclibClientTest {
    private val song = Recording("r", "A & B", listOf("Artist"), null, null)
    @Test fun identifiesClientAndEncodesMetadata() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[]"))
            val result = LrclibClient(baseUrl = server.url("/api/").toString()).search(song)
            assertEquals(ProviderResult.Songs(emptyList()), result)
            val request = server.takeRequest()
            assertEquals("A & B", request.requestUrl?.queryParameter("track_name"))
            assertTrue(request.getHeader("User-Agent").orEmpty().startsWith("Singalong/"))
        }
    }
    @Test fun rateLimitAndMalformedResponsesAreDistinct() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "12"))
            val client = LrclibClient(baseUrl = server.url("/api/").toString())
            assertEquals(ProviderResult.Failed("rate_limited", 12_000), client.search(song))
        }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("not json"))
            assertEquals(ProviderResult.Failed("invalid_response"), LrclibClient(baseUrl = server.url("/api/").toString()).search(song))
        }
    }
    @Test fun cooldownReturnsImmediatelyWithoutQueueingAnotherNetworkCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "12"))
            var now = 0L
            val client = LrclibClient(baseUrl = server.url("/api/").toString(), now = { now })
            client.search(song)
            now = 3000
            assertEquals(ProviderResult.Failed("rate_limited", 9000), client.search(song))
            assertEquals(1, server.requestCount)
        }
        val epoch = java.time.Instant.parse("2026-11-02T10:00:00Z").toEpochMilli()
        assertEquals(12_000L, retryAfterMillis("Mon, 2 Nov 2026 10:00:12 GMT", epoch))
    }
}
