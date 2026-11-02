package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test

class FallbackLyricsProviderTest {
    private val recording = Recording("r", "A / B", listOf("Artist & Co"), "Album", 120_000)
    @Test fun unavailablePrimaryFallsBackAndPreservesSourceWithoutInventingTiming() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"lyrics":"Synthetic original words"}"""))
            val primary = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Failed("unavailable", 60_000) }
            val result = FallbackLyricsProvider(primary, LyricsOvhClient(baseUrl = server.url("/v1/").toString())).search(recording) as ProviderResult.Songs
            val version = LyricsMatcher.versions(recording, result.songs).single()
            assertEquals("lyrics.ovh", version.sourceName)
            assertEquals(MatchConfidence.UNCERTAIN, version.confidence)
            assertTrue(version.lines.all { it.startMs == null })
            assertEquals(listOf("v1", "Artist & Co", "A / B"), server.takeRequest().requestUrl?.pathSegments)
        }
    }
    @Test fun usablePrimaryDoesNotFetchFallbackAndFailedFallbackDoesNotMeanNotFound() = runBlocking {
        var fallbackCalls = 0
        val primary = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist & Co", plainLyrics = "Synthetic words"))) }
        val fallback = object : LyricsProvider { override suspend fun search(recording: Recording): ProviderResult { fallbackCalls++; return ProviderResult.Failed("network_error") } }
        assertTrue(FallbackLyricsProvider(primary, fallback).search(recording) is ProviderResult.Songs)
        assertEquals(0, fallbackCalls)
        val missing = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(emptyList()) }
        assertEquals(ProviderResult.Failed("network_error"), FallbackLyricsProvider(missing, fallback).search(recording))
    }
}
