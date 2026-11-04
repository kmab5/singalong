package com.kmab.singalong.lyrics

import com.kmab.singalong.library.ReaderStore
import com.kmab.singalong.model.*
import com.kmab.singalong.translation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReaderState(
    val snapshot: PlaybackSnapshot? = null,
    val translation: TranslationRecord? = null,
    val status: String = "idle",
    val version: LyricVersion? = null,
    val candidates: List<LyricVersion> = emptyList(),
    val delayMs: Long = 0,
    val allowUncertainSync: Boolean = false,
    val following: Boolean = true,
    val moment: LyricMoment = LyricMoment(),
    val retryInSeconds: Int? = null,
    val translationStatus: String? = null,
    val wordProgress: Float? = null,
) {
    val hasTiming: Boolean get() = version?.lines?.any { it.startMs != null } == true
    val experimental: Boolean get() = version != null && (!hasTiming || version.confidence == MatchConfidence.UNCERTAIN)
    val canTrySync: Boolean get() = (snapshot?.positionMs ?: -1) >= 0 && version?.lines?.any { it.text.isNotBlank() } == true &&
        (hasTiming || (snapshot?.recording?.durationMs ?: 0) > 0)
    val estimated: Boolean get() = !hasTiming && allowUncertainSync && canTrySync
    val canSync: Boolean get() {
        val lyrics = version ?: return false
        return canTrySync && (estimated || (hasTiming && (lyrics.confidence == MatchConfidence.CONFIDENT || allowUncertainSync)))
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
    private val translations: TranslationRepository? = null,
    private val translationEnabled: () -> Boolean = { false },
) {
    private val mutableState = MutableStateFlow(ReaderState())
    val state = mutableState.asStateFlow()
    private var fetch: Job? = null
    private var translate: Job? = null
    private val demand = MutableStateFlow(false)

    init {
        scope.launch {
            current.collect { snapshot ->
                val changed = snapshot?.recording?.id != state.value.snapshot?.recording?.id
                if (changed) {
                    fetch?.cancel()
                    translate?.cancel()
                    mutableState.value = ReaderState(snapshot = snapshot, following = snapshot?.positionMs != null)
                    if (demand.value) startRequest(false)
                } else {
                    val metadataChanged = snapshot?.recording != state.value.snapshot?.recording
                    val lostTiming = state.value.snapshot?.positionMs != null && snapshot?.positionMs == null
                    mutableState.update { it.copy(snapshot = snapshot, following = it.following && !lostTiming) }
                    if (metadataChanged && demand.value && state.value.version == null) request()
                }
            }
        }
        scope.launch {
            demand.collectLatest { visible ->
                if (!visible) return@collectLatest
                while (currentCoroutineContext().isActive) {
                    mutableState.update { s ->
                        val position = PlaybackClock.position(s.snapshot, elapsedMs())
                        val moment = when {
                            position == null || !s.canSync -> LyricMoment()
                            s.estimated -> EstimatedTimeline.at(s.version!!.lines, position, s.snapshot?.recording?.durationMs ?: 0, s.delayMs)
                            else -> LyricTimeline.at(s.version!!.lines, position, s.delayMs)
                        }
                        s.copy(moment = moment, wordProgress = if (position != null && s.canSync && s.hasTiming)
                            WordSweep.progress(s.version!!.lines, moment.index, position, s.delayMs, s.snapshot?.recording?.durationMs) else null)
                    }
                    delay(200)
                }
            }
        }
    }
    fun tracking(active: Boolean) { if (!active) { fetch?.cancel(); fetch = null; translate?.cancel() } }
    fun visible(visible: Boolean) {
        demand.value = visible
        if (visible) startRequest(false) else { fetch?.cancel(); translate?.cancel(); mutableState.update { it.copy(retryInSeconds = null) } }
    }
    fun refreshLocal() {
        fetch?.cancel()
        val id = state.value.snapshot?.recording?.id ?: return
        fetch = scope.launch {
            val version = library.selectedVersion(id)
            if (state.value.snapshot?.recording?.id == id) load(LookupResult(if (version == null) "not_found" else "available", version))
        }
    }
    fun request() { fetch?.cancel(); startRequest(true) }
    private fun startRequest(force: Boolean) {
        val recording = state.value.snapshot?.recording ?: return
        if (fetch?.isActive == true) return
        fetch = scope.launch {
            repeat(4) { attempt ->
                mutableState.update { it.copy(status = if (it.version == null) "loading" else it.status, retryInSeconds = null) }
                val result = repository.lookup(recording, networkAllowed, forceRefresh = force || attempt > 0)
                ensureActive()
                if (state.value.snapshot?.recording?.id != recording.id) return@launch
                load(result)
                if (!demand.value || result.status !in retryable || result.status == "offline" || attempt == 3) return@launch
                var remaining = maxOf(result.retryAfterMs ?: 0, RetryPolicy.delayMs(attempt))
                while (remaining > 0) {
                    mutableState.update { it.copy(retryInSeconds = (remaining / 1000 + if (remaining % 1000 > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
                    val step = minOf(remaining, 1000)
                    delay(step); remaining -= step
                }
            }
        }
    }
    fun collected(recording: Recording) {
        if (state.value.snapshot?.recording?.id == recording.id && demand.value) startRequest(false)
    }
    fun networkChanged() {
        if (!networkAllowed()) {
            translate?.cancel()
            if (translationEnabled() && state.value.version != null && !translationComplete())
                mutableState.update { it.copy(translationStatus = "Waiting for connection") }
        } else if (demand.value && (state.value.status in retryable ||
                (translationEnabled() && state.value.version != null && !translationComplete() && state.value.translationStatus == "Waiting for connection"))) request()
    }
    private fun translationComplete(): Boolean {
        val version = state.value.version ?: return false
        val translation = state.value.translation ?: return false
        return TranslationRepository.valid(version, translation)
    }
    private suspend fun load(result: LookupResult) {
        val id = state.value.snapshot?.recording?.id ?: return
        val version = result.version
        val adjustment = version?.let { library.adjustment(it.id) }
        val candidates = library.versions(id)
        currentCoroutineContext().ensureActive()
        if (state.value.snapshot?.recording?.id != id) return
        val sameVersion = version != null && version.id == state.value.version?.id && version.contentHash == state.value.version?.contentHash
        val keepTranslation = sameVersion && translationEnabled()
        val alreadyTranslating = keepTranslation && translate?.isActive == true
        if (!alreadyTranslating) translate?.cancel()
        if (state.value.snapshot?.recording?.id == id) mutableState.update { it.copy(status = result.status, version = version,
            translation = if (keepTranslation) it.translation else null,
            translationStatus = if (keepTranslation) it.translationStatus else null, retryInSeconds = null,
            candidates = candidates, delayMs = adjustment?.delayMs ?: 0, allowUncertainSync = adjustment?.allowUncertainSync ?: false) }
        if (version != null && translationEnabled() && demand.value && !alreadyTranslating) translate = scope.launch {
            mutableState.update { it.copy(translationStatus = "Translating to English…") }
            val result = translations?.resolve(version, true, true, networkAllowed()) { partial ->
                ensureActive()
                if (state.value.version?.id == version.id && translationEnabled() && demand.value) mutableState.update { it.copy(
                    translation = partial,
                    translationStatus = "English · ${partial.lines.size}/${version.lines.count { line -> line.text.isNotBlank() }} lines",
                ) }
            }
            ensureActive()
            if (state.value.version?.id == version.id && translationEnabled()) mutableState.update { it.copy(
                translation = (result as? TranslationResult.Available)?.record ?: it.translation,
                translationStatus = when (result) {
                    is TranslationResult.Available -> "English · ${result.record.provider}"
                    TranslationResult.Unsupported -> null
                    else -> if (it.translation != null) "Partial English translation · remaining lines unavailable" else "English translation unavailable"
                },
            ) }
        }
    }
    fun select(version: LyricVersion) {
        if (state.value.snapshot?.recording?.id != version.recordingId) return
        fetch?.cancel()
        fetch = scope.launch {
        val id = state.value.snapshot?.recording?.id ?: return@launch
        if (version.recordingId != id) return@launch
        library.selectVersion(id, version.id)
        if (state.value.snapshot?.recording?.id == id) load(LookupResult("available", version))
    } }
    fun follow(enabled: Boolean) { mutableState.update { it.copy(following = enabled) } }
    fun toggleSync() {
        if (state.value.experimental) experimentalSync(!state.value.allowUncertainSync)
        else follow(!state.value.following)
    }
    fun experimentalSync(enabled: Boolean) {
        val s = state.value
        val version = s.version ?: return
        if (enabled && !s.canTrySync) return
        mutableState.update { it.copy(allowUncertainSync = enabled, following = enabled, moment = LyricMoment()) }
        scope.launch { library.adjust(com.kmab.singalong.library.AdjustmentEntity(version.id, s.delayMs, enabled)) }
    }
    fun setDelay(value: Long) { adjust(value.coerceIn(-600_000, 600_000) - state.value.delayMs) }
    fun alignLine(index: Int) {
        val s = state.value
        val version = s.version ?: return
        val position = PlaybackClock.position(s.snapshot, elapsedMs()) ?: return
        val start = if (s.estimated) EstimatedTimeline.starts(version.lines, s.snapshot?.recording?.durationMs ?: 0)[index]
            else version.lines.getOrNull(index)?.startMs
        start?.let { setDelay(position - it) }
    }
    fun adjust(delta: Long?, trySync: Boolean = false) {
        val version = state.value.version ?: return
        val delay = if (delta == null) 0 else (state.value.delayMs + delta).coerceIn(-600_000, 600_000)
        val allow = state.value.allowUncertainSync || trySync
        mutableState.update { it.copy(delayMs = delay, allowUncertainSync = allow, following = true) }
        scope.launch { library.adjust(com.kmab.singalong.library.AdjustmentEntity(version.id, delay, allow)) }
    }
    companion object { val retryable = setOf("offline", "network_error", "unavailable", "rate_limited") }
}

object RetryPolicy {
    fun delayMs(attempt: Int): Long = when (attempt) { 0 -> 2_000; 1 -> 5_000; else -> 10_000 }
}
