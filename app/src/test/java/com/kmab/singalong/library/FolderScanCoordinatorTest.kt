package com.kmab.singalong.library

import com.kmab.singalong.lyrics.LyricSearchResult
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderScanCoordinatorTest {
    private val song = Recording("file", "Song", listOf("Artist"), null, 120_000)
    private fun found(recording: Recording) = LyricSearchResult("available", listOf(LyricVersion("v", recording.id, "hash", LyricOrigin.PROVIDER,
        listOf(LyricLine("1", 0, "Synthetic test words")), "Test", null, MatchConfidence.CONFIDENT)))
    private class Store : FolderImportStore {
        val saved = mutableListOf<Recording>()
        override suspend fun hasLyrics(recording: Recording) = saved.any { FolderIdentity.same(it, recording) }
        override suspend fun save(recording: Recording, versions: List<LyricVersion>): Boolean {
            if (hasLyrics(recording)) return false
            saved.add(recording); return true
        }
    }
    @Test fun duplicateFilesAndRepeatedScansSaveOnlyOneSongAndDoNotLookupAgain() = runTest {
        val store = Store(); var requests = 0
        val worker = FolderScanCoordinator(this, store, { requests++; found(it) }, { true }, {})
        val files = flowOf(AudioCandidate("first", song), AudioCandidate("copy", song.copy(id = "copy")))
        worker.start(files); advanceUntilIdle()
        assertEquals(1, requests); assertEquals(1, worker.state.value.saved); assertEquals(1, worker.state.value.skipped)
        worker.start(files); advanceUntilIdle()
        assertEquals(1, requests); assertEquals(2, worker.state.value.skipped); assertEquals(1, store.saved.size)
    }
    @Test fun missesAndUnreadableFilesAreCountedWithoutEmptySavedRecords() = runTest {
        val store = Store()
        val worker = FolderScanCoordinator(this, store, { LyricSearchResult("not_found") }, { true }, {})
        worker.start(flowOf(AudioCandidate("missing", song), AudioCandidate("unreadable", null))); advanceUntilIdle()
        assertTrue(store.saved.isEmpty()); assertEquals(2, worker.state.value.checked)
        assertEquals(1, worker.state.value.notFound); assertEquals(1, worker.state.value.failed)
        assertEquals(ScanPhase.COMPLETE, worker.state.value.phase)
    }
    @Test fun cancellationStopsPendingRequestAndDoesNotSaveItsLateResult() = runTest {
        val store = Store(); val entered = CompletableDeferred<Unit>(); var cancelled = false
        val worker = FolderScanCoordinator(this, store, {
            entered.complete(Unit)
            try { awaitCancellation() } finally { cancelled = true }
        }, { true }, {})
        worker.start(flowOf(AudioCandidate("song", song))); runCurrent(); entered.await()
        worker.cancel(); advanceUntilIdle()
        assertTrue(cancelled); assertTrue(store.saved.isEmpty()); assertEquals(ScanPhase.CANCELLED, worker.state.value.phase)
    }
    @Test fun immediateCancellationBeforeDispatchDoesNotLeaveTheScanStuck() = runTest {
        val store = Store(); var requests = 0
        val worker = FolderScanCoordinator(this, store, { requests++; found(it) }, { true }, {})
        worker.start(flowOf(AudioCandidate("song", song)))
        worker.cancel(); advanceUntilIdle()
        assertEquals(0, requests)
        assertEquals(ScanPhase.CANCELLED, worker.state.value.phase)
        worker.start(flowOf(AudioCandidate("song", song))); advanceUntilIdle()
        assertEquals(ScanPhase.COMPLETE, worker.state.value.phase)
        assertEquals(1, requests)
    }
    @Test fun policyChangeDuringRequestWaitsForAnAllowedConnectionBeforeRetrying() = runTest {
        val store = Store(); var allowed = true; var requests = 0
        val connected = CompletableDeferred<Unit>()
        val worker = FolderScanCoordinator(this, store, {
            requests++
            if (requests == 1) { allowed = false; LyricSearchResult("network_error") } else found(it)
        }, { allowed }, { connected.await() })
        worker.start(flowOf(AudioCandidate("song", song))); advanceUntilIdle()
        assertEquals(ScanPhase.WAITING_NETWORK, worker.state.value.phase)
        assertEquals(1, requests); assertTrue(store.saved.isEmpty())
        allowed = true; connected.complete(Unit); advanceUntilIdle()
        assertEquals(2, requests); assertEquals(1, worker.state.value.saved)
    }
    @Test fun networkPolicyPausesAndCooldownDelaysRetryBeforeSaving() = runTest {
        val store = Store(); var online = false; var requests = 0
        val connected = CompletableDeferred<Unit>(); val requestTimes = mutableListOf<Long>()
        val worker = FolderScanCoordinator(this, store, {
            requests++; requestTimes.add(testScheduler.currentTime)
            if (requests == 1) LyricSearchResult("rate_limited", retryAfterMs = 5_000) else found(it)
        }, { online }, { connected.await() })
        worker.start(flowOf(AudioCandidate("song", song))); runCurrent()
        assertEquals(ScanPhase.WAITING_NETWORK, worker.state.value.phase); assertEquals(0, requests)
        online = true; connected.complete(Unit); advanceUntilIdle()
        assertEquals(listOf(0L, 5_000L), requestTimes); assertEquals(1, worker.state.value.saved)
    }
    @Test fun anExistingSaveThatArrivesDuringLookupIsSkippedAndSelectionIsNotReplaced() = runTest {
        val store = Store()
        val worker = FolderScanCoordinator(this, store, { store.saved.add(song); found(it) }, { true }, {})
        worker.start(flowOf(AudioCandidate("song", song))); advanceUntilIdle()
        assertEquals(0, worker.state.value.saved); assertEquals(1, worker.state.value.skipped); assertEquals(1, store.saved.size)
    }

    @Test fun repeatedProviderFailureStopsRetryingAndContinuesWithTheNextFile() = runTest {
        val store = Store(); var requests = 0
        val next = song.copy(id = "next", title = "Next song")
        val worker = FolderScanCoordinator(this, store, {
            requests++
            if (it.id == song.id) LyricSearchResult("network_error") else found(it)
        }, { true }, {})
        worker.start(flowOf(AudioCandidate("fails", song), AudioCandidate("works", next))); advanceUntilIdle()
        assertEquals(4, requests)
        assertEquals(1, worker.state.value.failed); assertEquals(1, worker.state.value.saved)
        assertEquals(ScanPhase.COMPLETE, worker.state.value.phase)
    }

    @Test fun losingFolderAccessKeepsCompletedSavesAndCanBeRestarted() = runTest {
        val store = Store()
        val worker = FolderScanCoordinator(this, store, ::found, { true }, {})
        worker.start(flow {
            emit(AudioCandidate("works", song))
            throw SecurityException("access revoked")
        }); advanceUntilIdle()
        assertEquals(ScanPhase.FAILED, worker.state.value.phase)
        assertEquals(1, store.saved.size)
        worker.start(flowOf(AudioCandidate("same", song))); advanceUntilIdle()
        assertEquals(ScanPhase.COMPLETE, worker.state.value.phase)
        assertEquals(1, worker.state.value.skipped)
    }
}
