package com.kmab.singalong.tracking

import com.kmab.singalong.library.LibraryRepository
import com.kmab.singalong.model.*
import com.kmab.singalong.settings.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/** One serialized event loop owns selection, identity resolution and play qualification. */
class TrackingCoordinator(
    private val scope: CoroutineScope,
    private val observer: PlaybackObserver,
    private val settings: StateFlow<AppSettings>,
    private val library: LibraryRepository,
    private val elapsedMs: () -> Long,
    private val wallMs: () -> Long,
    private val onQualified: (Recording) -> Unit,
) {
    private val selector = PlayerSelector()
    private val qualifier = CollectionQualifier()
    private val mutableCurrent = MutableStateFlow<PlaybackSnapshot?>(null)
    val current = mutableCurrent.asStateFlow()
    private val override = MutableStateFlow<String?>(null)
    val overrideSession = override.asStateFlow()
    private var job: Job? = null

    fun chooseSession(id: String?) { override.value = id }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            coroutineScope {
                val events = Channel<Unit>(Channel.CONFLATED)
                launch { combine(observer.sessions, settings, override) { _, _, _ -> Unit }.collect { events.send(Unit) } }
                launch { while (isActive) { events.send(Unit); delay(500) } }
                var rawRecording: Recording? = null
                var canonical: Recording? = null
                var rawSession: String? = null
                for (event in events) {
                    val sessions = observer.sessions.value
                    val allowed = settings.value.allowedPlayers
                    if (override.value != null && sessions.none { it.sessionId == override.value && it.packageName in allowed }) override.value = null
                    val selected = selector.select(sessions, allowed, override.value)
                    val incoming = selected?.recording
                    if (incoming == null) {
                        rawRecording = null; canonical = null; rawSession = null
                    } else if (incoming != rawRecording || selected.sessionId != rawSession) {
                        val previousId = canonical?.id.takeIf { incoming.id == rawRecording?.id && selected.sessionId == rawSession }
                        canonical = library.resolveRecording(incoming, previousId)
                        rawRecording = incoming
                        rawSession = selected.sessionId
                    }
                    val resolved = selected?.copy(recording = canonical)
                    mutableCurrent.value = resolved
                    qualifier.observe(resolved, elapsedMs())?.let { qualified ->
                        library.recordQualifiedPlay(qualified.recordingId, qualified.occurrenceId, wallMs())
                        resolved?.recording?.let(onQualified)
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        mutableCurrent.value = null
        override.value = null
        selector.reset(); qualifier.reset()
    }
}
