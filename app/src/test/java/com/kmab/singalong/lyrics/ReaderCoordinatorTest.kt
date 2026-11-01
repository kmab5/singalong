package com.kmab.singalong.lyrics

import com.kmab.singalong.library.*
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderCoordinatorTest {
    private class Store : LyricsStore, ReaderStore {
        val saved = mutableMapOf<String, List<LyricVersion>>()
        override suspend fun selectedVersion(recordingId: String) = saved[recordingId]?.firstOrNull()
        override suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>) { saved[recording.id] = versions }
        override suspend fun setLookupStatus(recordingId: String, status: String) = Unit
        override suspend fun versions(recordingId: String) = saved[recordingId].orEmpty()
        override suspend fun selectVersion(recordingId: String, versionId: String) = Unit
        override suspend fun adjustment(versionId: String) = AdjustmentEntity(versionId)
        override suspend fun adjust(value: AdjustmentEntity) = Unit
    }
    private fun snapshot(id: String, position: Long? = 0) = PlaybackSnapshot("session", "player", "Player",
        Recording(id, id, listOf("Artist"), "Album", 120_000), PlaybackStatus.PLAYING, position, 0, 1f)

    @Test fun lateResultCannotReplaceNewSongAndNewSongClearsOldWords() = runTest {
        val releaseA = CompletableDeferred<Unit>()
        val current = MutableStateFlow<PlaybackSnapshot?>(snapshot("A"))
        val store = Store()
        val provider = object : LyricsProvider {
            override suspend fun search(recording: Recording): ProviderResult {
                if (recording.id == "A") withContext(NonCancellable) { releaseA.await() }
                return ProviderResult.Songs(listOf(ProviderSong(1, recording.title, "Artist", "Album", 120.0, plainLyrics = "Synthetic ${recording.id}")))
            }
        }
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider), { true }, { testScheduler.currentTime })
        reader.visible(true)
        runCurrent()
        current.value = snapshot("B")
        runCurrent()
        assertEquals("B", reader.state.value.version?.recordingId)
        releaseA.complete(Unit)
        runCurrent()
        assertEquals("B", reader.state.value.version?.recordingId)
    }
    @Test fun timingRecoveryDoesNotOverrideManualReading() = runTest {
        val current = MutableStateFlow<PlaybackSnapshot?>(snapshot("A"))
        val store = Store()
        val repository = LyricsRepository(store, object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(emptyList()) })
        val reader = ReaderCoordinator(backgroundScope, current, store, repository, { false }, { 0 })
        runCurrent()
        current.value = snapshot("A", null)
        runCurrent()
        current.value = snapshot("A", 1000)
        runCurrent()
        assertFalse(reader.state.value.following)
        reader.follow(true)
        assertTrue(reader.state.value.following)
    }
}
