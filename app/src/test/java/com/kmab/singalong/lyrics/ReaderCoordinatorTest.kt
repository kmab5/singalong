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
        val adjustments = mutableMapOf<String, AdjustmentEntity>()
        override suspend fun selectedVersion(recordingId: String) = saved[recordingId]?.firstOrNull()
        override suspend fun saveCandidates(recording: Recording, versions: List<LyricVersion>) { saved[recording.id] = versions }
        override suspend fun setLookupStatus(recordingId: String, status: String) = Unit
        override suspend fun versions(recordingId: String) = saved[recordingId].orEmpty()
        override suspend fun selectVersion(recordingId: String, versionId: String) = Unit
        override suspend fun adjustment(versionId: String) = adjustments[versionId] ?: AdjustmentEntity(versionId)
        override suspend fun adjust(value: AdjustmentEntity) { adjustments[value.versionId] = value }
    }
    private fun snapshot(id: String, position: Long? = 0) = PlaybackSnapshot("session", "player", "Player",
        Recording(id, id, listOf("Artist"), "Album", 120_000), PlaybackStatus.PLAYING, position, 0, 1f)

    @Test fun disablingWordTrackingKeepsLineSyncAndReenablingResumesWordProgress() = runTest {
        val store = Store()
        var wordsEnabled = true
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist", "Album", 120.0,
                syncedLyrics = "[00:00.00]First line\n[00:01.00]Second line"))) }
        val reader = ReaderCoordinator(backgroundScope, MutableStateFlow(snapshot("A")), store,
            LyricsRepository(store, provider, UnconfinedTestDispatcher(testScheduler)), { true },
            { testScheduler.currentTime }, wordTrackingEnabled = { wordsEnabled })
        reader.visible(true); runCurrent()
        advanceTimeBy(400); runCurrent()
        assertNotNull(reader.state.value.wordProgress)
        val original = reader.state.value.version
        wordsEnabled = false
        advanceTimeBy(1000); runCurrent()
        assertNull(reader.state.value.wordProgress)
        assertEquals(1, reader.state.value.moment.index)
        assertTrue(reader.state.value.following)
        assertEquals(original, reader.state.value.version)
        wordsEnabled = true
        advanceTimeBy(200); runCurrent()
        assertNotNull(reader.state.value.wordProgress)
    }

    @Test fun uncertainTimedLyricsFollowAutomaticallyButRefreshingDoesNotUndoManualPause() = runTest {
        val store = Store()
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist", "Different album", 120.0, syncedLyrics = "[00:01.00]Synthetic first\n[00:02.00]Synthetic second"))) }
        val reader = ReaderCoordinator(backgroundScope, MutableStateFlow(snapshot("A", 1500)), store, LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { 0 })
        reader.visible(true); runCurrent()
        assertTrue(reader.state.value.following)
        assertTrue(reader.state.value.canSync)
        assertEquals(MatchConfidence.UNCERTAIN, reader.state.value.version!!.confidence)
        reader.toggleSync(); runCurrent()
        assertFalse(reader.state.value.following)
        reader.refreshLocal(); runCurrent()
        assertFalse(reader.state.value.following)
        reader.toggleSync(); runCurrent()
        assertTrue(reader.state.value.following)
    }

    @Test fun aSingleUntimedVersionOffersOptionalPersistentSyncAndFineAdjustment() = runTest {
        val store = Store()
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist", "Album", 120.0, plainLyrics = "First\nOther"))) }
        val current = MutableStateFlow<PlaybackSnapshot?>(snapshot("A", 20_000).copy(status = PlaybackStatus.PAUSED))
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { 0 })
        reader.visible(true); runCurrent()
        assertEquals(1, reader.state.value.candidates.size)
        assertTrue(reader.state.value.canTrySync)
        assertFalse(reader.state.value.canSync)
        val original = reader.state.value.version!!
        reader.toggleSync(); runCurrent()
        assertTrue(reader.state.value.estimated)
        assertTrue(store.adjustments[original.id]!!.allowUncertainSync)
        reader.adjust(50); runCurrent()
        assertEquals(50, reader.state.value.delayMs)
        reader.alignLine(1); runCurrent()
        assertEquals(-40_000, reader.state.value.delayMs)
        assertEquals(-40_000, store.adjustments[original.id]!!.delayMs)
        reader.refreshLocal(); runCurrent()
        assertTrue(reader.state.value.estimated)
        reader.toggleSync(); runCurrent()
        assertFalse(reader.state.value.canSync)
        assertFalse(store.adjustments[original.id]!!.allowUncertainSync)
        assertEquals(original, reader.state.value.version)
        assertTrue(original.lines.all { it.startMs == null })
        current.value = current.value!!.let { it.copy(recording = it.recording!!.copy(durationMs = null)) }; runCurrent()
        reader.experimentalSync(true)
        assertFalse(reader.state.value.canTrySync)
        assertFalse(reader.state.value.canSync)
    }

    @Test fun refreshingSameVersionKeepsProgressAndDoesNotRestartTranslation() = runTest {
        val store = Store()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val translations = TranslationRepository(object : TranslationStore {
            override suspend fun translation(versionId: String): TranslationRecord? = null
            override suspend fun saveTranslation(record: TranslationRecord) = Unit
        }, object : ProgressiveTranslationProvider {
            override val id = "fixture"
            override suspend fun translate(version: LyricVersion) = error("Use progressive translation")
            override suspend fun translateProgressively(version: LyricVersion, online: Boolean, onProgress: suspend (TranslationRecord) -> Unit): TranslationResult {
                calls++
                onProgress(TranslationRecord(version.id, version.contentHash, id, "tr", mapOf(version.lines.first().id to "First"), "Fixture"))
                finish.await()
                return TranslationResult.Unavailable
            }
        })
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist", "Album", 120.0, plainLyrics = "Bir\nİki"))) }
        val reader = ReaderCoordinator(backgroundScope, MutableStateFlow(snapshot("A")), store, LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { 0 }, translations) { true }
        reader.visible(true); runCurrent()
        assertEquals(1, reader.state.value.translation!!.lines.size)
        reader.refreshLocal(); runCurrent()
        assertEquals(1, calls)
        assertEquals(1, reader.state.value.translation!!.lines.size)
        finish.complete(Unit); runCurrent()
        assertEquals(1, reader.state.value.translation!!.lines.size)
    }

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
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { testScheduler.currentTime })
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
            LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { testScheduler.currentTime })
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
        }, object : ProgressiveTranslationProvider {
            override val id = "fixture"
            override suspend fun translate(version: LyricVersion) = error("Use progressive translation")
            override suspend fun translateProgressively(version: LyricVersion, online: Boolean, onProgress: suspend (TranslationRecord) -> Unit): TranslationResult {
                withContext(NonCancellable) { pending.await() }
                val translated = TranslationRecord(version.id, version.contentHash, id, "ja",
                    version.lines.associate { it.id to "English ${version.recordingId}" }, "Fixture")
                onProgress(translated)
                return TranslationResult.Available(translated)
            }
        })
        val provider = object : LyricsProvider { override suspend fun search(recording: Recording) = ProviderResult.Songs(listOf(
            ProviderSong(1, recording.title, "Artist", "Album", 120.0, plainLyrics = "Original ${recording.id}"))) }
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { 0 }, translations) { true }
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
            LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { testScheduler.currentTime })
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
        val reader = ReaderCoordinator(backgroundScope, current, store, LyricsRepository(store, provider, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), { true }, { 0 })
        reader.visible(true); runCurrent()
        assertEquals("not_found", reader.state.value.status)
        current.value = snapshot("A"); runCurrent()
        assertEquals(2, calls)
        assertEquals("Original words", reader.state.value.version?.lines?.single()?.text)
    }
}
