package com.kmab.singalong.lyrics

import com.kmab.singalong.library.LyricsStore
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LyricsRepositoryTest {
    private val recording = Recording("r", "Synthetic Song", listOf("Test Artist"), "Test Album", 120_000)
    private val candidate = ProviderSong(1, recording.title, "Test Artist", "Test Album", 120.0, plainLyrics = "Synthetic words")
    private class Store : LyricsStore {
        var selected: LyricVersion? = null
        override suspend fun selectedVersion(recordingId: String) = selected
        override suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>) { if (selected == null) selected = versions.firstOrNull() }
        override suspend fun setLookupStatus(recordingId: String, status: String) = Unit
    }
    @Test fun concurrentRequestsFetchOnceAndOfflineUsesSavedOriginal() = runTest {
        var calls = 0
        val store = Store()
        val repository = LyricsRepository(store, object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult { calls++; delay(100); return ProviderResult.Songs(listOf(candidate)) }
        })
        val results = List(4) { async { repository.lookup(recording, { true }) } }.awaitAll()
        assertEquals(1, calls)
        assertTrue(results.all { it.version != null })
        assertEquals(store.selected, repository.lookup(recording, { false }).version)
    }
    @Test fun providerFailureIsNotConfusedWithNotFoundAndRespectsCooldown() = runTest {
        var calls = 0
        val repository = LyricsRepository(Store(), object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult { calls++; return ProviderResult.Failed("rate_limited", 60_000) }
        })
        assertEquals("rate_limited", repository.lookup(recording, { true }, { 0 }).status)
        repository.lookup(recording, { true }, { 1000 })
        assertEquals(1, calls)
    }
    @Test fun wrongArtistAndVersionAreRejectedAndMissingAlbumIsUncertain() {
        assertNull(LyricsMatcher.confidence(recording, candidate.copy(artistName = "Other Artist")))
        assertNull(LyricsMatcher.confidence(recording, candidate.copy(trackName = "Synthetic Song (Live)")))
        assertEquals(MatchConfidence.UNCERTAIN, LyricsMatcher.confidence(recording.copy(album = null), candidate))
        assertEquals(MatchConfidence.CONFIDENT, LyricsMatcher.confidence(recording, candidate))
        assertEquals(MatchConfidence.CONFIDENT, LyricsMatcher.confidence(recording, candidate.copy(trackName = "Synthetic: Song")))
        assertNull(LyricsMatcher.confidence(recording, candidate.copy(duration = 220.0)))
    }
}
