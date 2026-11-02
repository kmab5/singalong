package com.kmab.singalong.lyrics

import com.kmab.singalong.library.*
import com.kmab.singalong.model.*
import com.kmab.singalong.translation.*
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
    @Test fun visibleReaderRetriesWithoutClickAndStopsWhenHidden() = runTest {
        val store = Store()
        var calls = 0
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording): ProviderResult {
            calls++; return ProviderResult.Failed("network_error")
        } }
        val reader = ReaderCoordinator(backgroundScope, MutableStateFlow(snapshot("A")), store,
            LyricsRepository(store, provider), { true }, { testScheduler.currentTime })
        reader.visible(true); runCurrent()
        assertEquals(1, calls)
        assertEquals(2, reader.state.value.retryInSeconds)
        advanceTimeBy(2000); runCurrent()
        assertEquals(2, calls)
        reader.visible(false)
        advanceTimeBy(30_000); runCurrent()
        assertEquals(2, calls)
    }
    @Test fun originalsPublishBeforeSlowTranslationAndLateTranslationCannotReplaceNewSong() = runTest {
        val store = Store()
        val current = MutableStateFlow<PlaybackSnapshot?>(snapshot("A"))
        val pending = CompletableDeferred<Unit>()
        val translations = TranslationRepository(object : TranslationStore {
            override suspend fun translation(versionId: String): TranslationRecord? = null
            override suspend fun saveTranslation(record: TranslationRecord) = Unit
        }, object : TranslationProvider {
            override val id = "fixture"
            override suspend fun translate(version: LyricVersion): TranslationResult {
                withContext(NonCancellable) { pending.await() }
                return TranslationResult.Available(TranslationRecord(version.id, version.contentHash, id, "ja",
                    version.lines.associate { it.id to "English ${version.recordingId}" }, "Fixture"))
            }
        })
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist", "Album", 120.0, plainLyrics = "Original ${recording.id}"))) }
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider), { true }, { 0 }, translations) { true }
        reader.visible(true); runCurrent()
        assertEquals("A", reader.state.value.version?.recordingId)
        assertNull(reader.state.value.translation)
        current.value = snapshot("B"); runCurrent()
        pending.complete(Unit); runCurrent()
        assertEquals("B", reader.state.value.version?.recordingId)
        assertEquals("English B", reader.state.value.translation?.lines?.values?.single())
    }
    @Test fun automaticRetriesHaveABoundedBudget() = runTest {
        val store = Store()
        var calls = 0
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording): ProviderResult {
            calls++; return ProviderResult.Failed("unavailable")
        } }
        val reader = ReaderCoordinator(backgroundScope, MutableStateFlow(snapshot("A")), store,
            LyricsRepository(store, provider), { true }, { testScheduler.currentTime })
        reader.visible(true); runCurrent()
        advanceTimeBy(60_000); runCurrent()
        assertEquals(4, calls)
        assertNull(reader.state.value.retryInSeconds)
    }
    @Test fun improvedMetadataRetriesACachedMissWithoutChangingRecordingIdentity() = runTest {
        val store = Store()
        val current = MutableStateFlow<PlaybackSnapshot?>(snapshot("A").let { it.copy(recording = it.recording!!.copy(album = null)) })
        var calls = 0
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording): ProviderResult {
            calls++
            return ProviderResult.Songs(if (recording.album == null) emptyList() else listOf(
                ProviderSong(1, recording.title, "Artist", "Album", 120.0, plainLyrics = "Original words")))
        } }
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider), { true }, { 0 })
        reader.visible(true); runCurrent()
        assertEquals("not_found", reader.state.value.status)
        current.value = snapshot("A"); runCurrent()
        assertEquals(2, calls)
        assertEquals("Original words", reader.state.value.version?.lines?.single()?.text)
    }
}
