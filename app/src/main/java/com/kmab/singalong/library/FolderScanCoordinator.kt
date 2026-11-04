package com.kmab.singalong.library

import com.kmab.singalong.lyrics.LyricSearchResult
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

data class AudioCandidate(val name: String, val recording: Recording?)
interface FolderImportStore {
    suspend fun hasLyrics(recording: Recording): Boolean
    /** Atomic recheck/save. False means a saved match already exists. */
    suspend fun save(recording: Recording, versions: List<LyricVersion>): Boolean
}
enum class ScanPhase { IDLE, SCANNING, WAITING_NETWORK, COOLDOWN, STOPPING, COMPLETE, CANCELLED, FAILED }
data class FolderScanState(
    val phase: ScanPhase = ScanPhase.IDLE,
    val checked: Int = 0, val saved: Int = 0, val skipped: Int = 0,
    val notFound: Int = 0, val failed: Int = 0,
    val currentFile: String? = null, val retrySeconds: Long? = null, val message: String? = null,
) {
    val running get() = phase in setOf(ScanPhase.SCANNING, ScanPhase.WAITING_NETWORK, ScanPhase.COOLDOWN, ScanPhase.STOPPING)
}

/** Session-owned, serial and cancellable. No audio bytes are passed to the search provider. */
class FolderScanCoordinator(
    private val scope: CoroutineScope,
    private val store: FolderImportStore,
    private val search: suspend (Recording) -> LyricSearchResult,
    private val networkAllowed: () -> Boolean,
    private val awaitNetwork: suspend () -> Unit,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutableState = MutableStateFlow(FolderScanState())
    val state = mutableState.asStateFlow()
    @Volatile private var job: Job? = null

    fun start(files: Flow<AudioCandidate>) {
        if (job?.isCompleted == false) return
        val scanJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                files.collect { file ->
                    currentCoroutineContext().ensureActive()
                    mutableState.update { it.copy(phase = ScanPhase.SCANNING, currentFile = file.name, retrySeconds = null) }
                    val recording = file.recording
                    var lookedUp = false
                    when {
                        recording == null -> mutableState.update { it.copy(failed = it.failed + 1) }
                        store.hasLyrics(recording) -> mutableState.update { it.copy(skipped = it.skipped + 1) }
                        else -> {
                            lookedUp = true
                            val result = lookup(recording)
                            currentCoroutineContext().ensureActive()
                            when {
                                result.versions.isNotEmpty() -> {
                                    val added = store.save(recording, result.versions)
                                    mutableState.update { if (added) it.copy(saved = it.saved + 1) else it.copy(skipped = it.skipped + 1) }
                                }
                                result.status in setOf("not_found", "instrumental") -> mutableState.update { it.copy(notFound = it.notFound + 1) }
                                else -> mutableState.update { it.copy(failed = it.failed + 1) }
                            }
                        }
                    }
                    mutableState.update { it.copy(checked = it.checked + 1) }
                    if (lookedUp) pause(1_000)
                }
                mutableState.update { it.copy(phase = ScanPhase.COMPLETE, currentFile = null, retrySeconds = null) }
            } catch (_: SecurityException) {
                mutableState.update { it.copy(phase = ScanPhase.FAILED, message = "Folder access was removed. Choose the folder again.") }
            } catch (_: IOException) {
                mutableState.update { it.copy(phase = ScanPhase.FAILED, message = "The folder or local storage could not be read. Saved lyrics are kept; try again.") }
            }
        }
        job = scanJob
        mutableState.value = FolderScanState(phase = ScanPhase.SCANNING)
        // Register before dispatch: cancellation can happen before the coroutine enters its body.
        scanJob.invokeOnCompletion { cause ->
            if (cause is CancellationException) mutableState.update {
                if (job === scanJob) it.copy(phase = ScanPhase.CANCELLED, currentFile = null, retrySeconds = null) else it
            }
        }
        scanJob.start()
    }

    fun cancel() {
        if (!state.value.running) return
        mutableState.update { it.copy(phase = ScanPhase.STOPPING) }
        job?.cancel()
    }

    private suspend fun lookup(recording: Recording): LyricSearchResult {
        var result = LyricSearchResult("not_found")
        repeat(3) { attempt ->
            if (!networkAllowed()) {
                mutableState.update { it.copy(phase = ScanPhase.WAITING_NETWORK, retrySeconds = null) }
                awaitNetwork()
            }
            currentCoroutineContext().ensureActive()
            mutableState.update { it.copy(phase = ScanPhase.SCANNING, retrySeconds = null) }
            result = search(recording)
            if (result.versions.isNotEmpty() || result.status !in setOf("offline", "rate_limited", "network_error", "unavailable") || attempt == 2) return result
            if (result.status != "offline") {
                var remaining = maxOf(result.retryAfterMs ?: 0, 2_000L * (attempt + 1))
                while (remaining > 0) {
                    mutableState.update { it.copy(phase = ScanPhase.COOLDOWN, retrySeconds = (remaining + 999) / 1000) }
                    val step = minOf(remaining, 1_000)
                    pause(step); remaining -= step
                }
            }
        }
        return result
    }
}
