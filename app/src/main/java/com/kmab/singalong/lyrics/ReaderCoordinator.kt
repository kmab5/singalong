package com.kmab.singalong.lyrics

import com.kmab.singalong.library.ReaderStore
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReaderState(
    val snapshot: PlaybackSnapshot? = null,
    val status: String = "idle",
    val version: LyricVersion? = null,
    val candidates: List<LyricVersion> = emptyList(),
    val delayMs: Long = 0,
    val allowUncertainSync: Boolean = false,
    val following: Boolean = true,
    val moment: LyricMoment = LyricMoment(),
) {
    val canSync: Boolean get() {
        val lyrics = version ?: return false
        return snapshot?.positionMs != null && lyrics.lines.any { it.startMs != null } &&
            (lyrics.confidence == MatchConfidence.CONFIDENT || allowUncertainSync)
    }
}

/** The live reader owns song generations; a late fetch can save locally but never replace another song. */
class ReaderCoordinator(
    private val scope: CoroutineScope,
    current: StateFlow<PlaybackSnapshot?>,
    private val library: ReaderStore,
    private val repository: LyricsRepository,
    private val networkAllowed: () -> Boolean,
    private val elapsedMs: () -> Long,
) {
    private val mutableState = MutableStateFlow(ReaderState())
    val state = mutableState.asStateFlow()
    private var fetch: Job? = null
    private val demand = MutableStateFlow(false)
    private var tracking = false

    init {
        scope.launch {
            current.collect { snapshot ->
                val changed = snapshot?.recording?.id != state.value.snapshot?.recording?.id
                if (changed) {
                    fetch?.cancel()
                    mutableState.value = ReaderState(snapshot = snapshot, following = snapshot?.positionMs != null)
                    if (demand.value) request()
                } else {
                    val lostTiming = state.value.snapshot?.positionMs != null && snapshot?.positionMs == null
                    mutableState.update { it.copy(snapshot = snapshot, following = it.following && !lostTiming) }
                }
            }
        }
        scope.launch {
            demand.collectLatest { visible ->
                if (!visible) return@collectLatest
                while (currentCoroutineContext().isActive) {
                    mutableState.update { s ->
                        val position = PlaybackClock.position(s.snapshot, elapsedMs())
                        s.copy(moment = if (position != null && s.canSync) LyricTimeline.at(s.version!!.lines, position, s.delayMs) else LyricMoment())
                    }
                    delay(200)
                }
            }
        }
    }
    fun tracking(active: Boolean) { tracking = active; if (!active) { fetch?.cancel(); fetch = null } }
    fun visible(visible: Boolean) { demand.value = visible; if (visible) request() }
    fun request() {
        val recording = state.value.snapshot?.recording ?: return
        if (fetch?.isActive == true) return
        fetch = scope.launch {
            mutableState.update { it.copy(status = "loading") }
            val result = repository.lookup(recording, networkAllowed)
            if (state.value.snapshot?.recording?.id != recording.id) return@launch
            load(result)
        }
    }
    fun collected(recording: Recording) {
        if (state.value.snapshot?.recording?.id == recording.id) request()
    }
    fun networkChanged() { if (tracking && demand.value && state.value.status in retryable) request() }
    private suspend fun load(result: LookupResult) {
        val id = state.value.snapshot?.recording?.id ?: return
        val version = result.version
        val adjustment = version?.let { library.adjustment(it.id) }
        val candidates = library.versions(id)
        if (state.value.snapshot?.recording?.id == id) mutableState.update { it.copy(status = result.status, version = version,
            candidates = candidates, delayMs = adjustment?.delayMs ?: 0, allowUncertainSync = adjustment?.allowUncertainSync ?: false) }
    }
    fun select(version: LyricVersion) { scope.launch {
        val id = state.value.snapshot?.recording?.id ?: return@launch
        if (version.recordingId != id) return@launch
        library.selectVersion(id, version.id)
        if (state.value.snapshot?.recording?.id == id) load(LookupResult("available", version))
    } }
    fun follow(enabled: Boolean) { mutableState.update { it.copy(following = enabled) } }
    fun adjust(delta: Long?, trySync: Boolean = false) {
        val version = state.value.version ?: return
        val delay = if (delta == null) 0 else (state.value.delayMs + delta).coerceIn(-600_000, 600_000)
        val allow = state.value.allowUncertainSync || trySync
        mutableState.update { it.copy(delayMs = delay, allowUncertainSync = allow, following = true) }
        scope.launch { library.adjust(com.kmab.singalong.library.AdjustmentEntity(version.id, delay, allow)) }
    }
    companion object { val retryable = setOf("offline", "network_error", "unavailable", "rate_limited") }
}
