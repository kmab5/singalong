package com.kmab.singalong.lyrics

import com.kmab.singalong.library.LyricsStore
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SmartLyricsSearchTest {
    private val original = Recording("file", "03 - Test_Artist - Synthetic_Song (Official Audio).mp3", listOf("<unknown>"), null, 120_000)
    private val song = ProviderSong(1, "Synthetic Song", "Test Artist", "Album", 120.0, plainLyrics = "Synthetic words")
    private class Store : LyricsStore {
        var savedRecording: Recording? = null
        var selected: LyricVersion? = null
        override suspend fun selectedVersion(recordingId: String) = selected
        override suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>) {
            savedRecording = recording; selected = versions.firstOrNull()
        }
        override suspend fun setLookupStatus(recordingId: String, status: String) = Unit
    }
    @Test fun filenameSearchFindsLyricsWithoutReplacingOriginalMetadata() = runTest {
        val store = Store()
        val queries = mutableListOf<Recording>()
        val repository = LyricsRepository(store, object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult {
                queries += recording
                return if (recording.title == song.trackName && recording.artists == listOf(song.artistName))
                    ProviderResult.Songs(listOf(song)) else ProviderResult.Songs(emptyList())
            }
        })
        val result = repository.lookup(original, { true })
        assertEquals("available", result.status)
        assertEquals(original, store.savedRecording)
        assertEquals(original.id, result.version?.recordingId)
        assertEquals(MatchConfidence.UNCERTAIN, result.version?.confidence)
        assertEquals(1, queries.size)
        assertEquals(result.version, repository.lookup(original, { false }).version)
    }
    @Test fun serverCooldownStopsFilenameAlternatives() = runTest {
        var calls = 0
        val repository = LyricsRepository(Store(), object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult {
                calls++; return ProviderResult.Failed("rate_limited", 30_000)
            }
        })
        assertEquals("rate_limited", repository.lookup(original, { true }, { 0 }).status)
        assertEquals(29_000L, repository.lookup(original, { true }, { 1000 }, true).retryAfterMs)
        assertEquals(1, calls)
    }
    @Test fun cancellingSongSearchCannotPersistLateResults() = runTest {
        val store = Store()
        val started = CompletableDeferred<Unit>()
        val repository = LyricsRepository(store, object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult {
                started.complete(Unit)
                withContext(NonCancellable) { delay(100) }
                return ProviderResult.Songs(listOf(song))
            }
        })
        val job = launch { repository.lookup(original.copy(title = song.trackName, artists = listOf(song.artistName)), { true }) }
        started.await(); job.cancelAndJoin()
        assertNull(store.selected)
    }
}
