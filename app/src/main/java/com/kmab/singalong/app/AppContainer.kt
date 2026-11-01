package com.kmab.singalong.app

import android.content.*
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kmab.singalong.library.*
import com.kmab.singalong.lyrics.*
import com.kmab.singalong.model.*
import com.kmab.singalong.overlay.*
import com.kmab.singalong.settings.SettingsStore
import com.kmab.singalong.tracking.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FoundationState(val tracking: Boolean = false, val listenerConnected: Boolean = false,
    val overlayVisible: Boolean = false, val message: String? = null)

class AppContainer(private val context: Context) {
    private val mutableState = MutableStateFlow(FoundationState())
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
        if (error is java.io.IOException || error is android.database.sqlite.SQLiteException) {
            serviceStopped()
            context.stopService(Intent(context, TrackingService::class.java))
            message("Local storage is unavailable. Free some space and reopen Singalong.")
        } else throw error
    })
    val settings = SettingsStore(context, scope)
    val library = LibraryRepository(LibraryDatabase.open(context))
    val entries = library.entries.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val gate = TrackingGate()
    val observer = AndroidPlaybackObserver(context) { accessLost() }
    private val connection = NetworkMonitor(context).changes.stateIn(scope, SharingStarted.Eagerly, Connectivity())
    private val provider = LrclibClient(permitsNetwork = ::networkAllowed)
    private val repository = LyricsRepository(library, provider)
    private val collectionJobs = mutableMapOf<String, Job>()
    val tracking = TrackingCoordinator(scope, observer, settings.state, library, SystemClock::elapsedRealtime, System::currentTimeMillis, ::collectLyrics)
    val translations = com.kmab.singalong.translation.TranslationRepository(library)
    val reader = ReaderCoordinator(scope, tracking.current, library, repository, ::networkAllowed, SystemClock::elapsedRealtime, translations) { settings.state.value.translationEnabled }
    private val mutablePanel = MutableStateFlow(OverlayState())
    val panel = mutablePanel.asStateFlow()
    private val mutablePlayers = MutableStateFlow<List<PlayerApp>>(emptyList())
    val players = mutablePlayers.asStateFlow()
    var appOpened = false; private set
    private var startPending = false
    private var generation = 0
    private var readerVisible = false
    private var activityVisible = false
    private var screenAllowed = true

    init {
        scope.launch { combine(connection, settings.state) { _, _ -> Unit }.collect {
            if (!networkAllowed()) provider.cancelRequests()
            reader.networkChanged()
        } }
        scope.launch {
            combine(panel, tracking.current) { panel, current -> panel to (current?.status == PlaybackStatus.PLAYING) }
                .distinctUntilChanged().collectLatest { (panel, playing) ->
                    updateDemand()
                    val now = SystemClock.elapsedRealtime()
                    if (playing || panel.pauseSince == null) mutablePanel.update { it.playback(playing, now) }
                    if (!playing && panel.mode == PanelMode.VISIBLE && panel.pauseSince != null) {
                        delay((15_000 - (now - panel.pauseSince)).coerceAtLeast(0))
                        mutablePanel.update { it.playback(false, SystemClock.elapsedRealtime()) }
                    }
                }
        }
    }
    fun hasMusicAccess() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    private fun networkAllowed() = connection.value.permits(settings.state.value.wifiOnly)
    fun onAppOpened() {
        appOpened = true
        val expected = generation
        scope.launch {
            val prefs = settings.state.first { it.ready }
            if (expected == generation && prefs.trackingEnabled && !state.value.tracking && !startPending) beginTracking()
            mutablePlayers.value = PlayerCatalog(context).installed()
        }
    }
    fun enableTracking() {
        val expected = ++generation
        scope.launch {
            settings.state.first { it.ready }
            if (expected != generation || !canStart()) return@launch
            settings.tracking(true)
            if (expected == generation) beginTracking()
        }
    }
    private fun canStart(): Boolean {
        if (!hasMusicAccess()) { message("Allow music access first."); return false }
        if (settings.state.value.allowedPlayers.isEmpty()) { message("Select at least one music app in settings."); return false }
        return true
    }
    private fun beginTracking() {
        if (!canStart()) return
        gate.onAppOpened(true); startPending = true
        startService(TrackingService.ACTION_START)
    }
    fun disableTracking() {
        generation++; serviceStopped()
        scope.launch { settings.tracking(false) }
        context.stopService(Intent(context, TrackingService::class.java))
    }
    fun allowPlayer(packageName: String, allowed: Boolean) { scope.launch { settings.allowPlayer(packageName, allowed) } }
    fun wifiOnly(enabled: Boolean) { scope.launch { settings.wifiOnly(enabled) } }
    fun notificationRequested() { scope.launch { settings.notificationRequested() } }
    fun translation(enabled: Boolean) { scope.launch { settings.translation(enabled); reader.request() } }
    fun translationSize(value: Float) { scope.launch { settings.translationSize(value) } }
    fun textSize(value: Float) { scope.launch { settings.textSize(value) } }
    fun opacity(value: Float) { scope.launch { settings.opacity(value) } }
    fun geometry(x: Float, y: Float, width: Float, height: Float) { scope.launch { settings.geometry(x, y, width, height) } }
    fun activityVisible(visible: Boolean) { activityVisible = visible; updateDemand() }
    fun readerVisible(visible: Boolean) { readerVisible = visible; updateDemand() }
    fun screenAllowed(allowed: Boolean) { screenAllowed = allowed; updateDemand() }
    private var previousDemand = false
    private fun updateDemand() {
        val demand = screenAllowed && ((activityVisible && readerVisible) || panel.value.mode == PanelMode.VISIBLE)
        if (demand != previousDemand) { previousDemand = demand; reader.visible(demand) }
    }
    fun showLyrics() { if (appOpened) startService(TrackingService.ACTION_SHOW_OVERLAY) }
    fun showPanel() { mutablePanel.update { it.show() }; updateDemand() }
    fun closePanel() {
        mutablePanel.update { it.close() }; updateDemand()
        if (!gate.active) context.stopService(Intent(context, TrackingService::class.java))
    }
    fun minimizePanel() { mutablePanel.update { it.minimize() }; updateDemand() }
    fun expandPanel() { mutablePanel.update { it.copy(expanded = !it.expanded) } }
    fun chooseSession(id: String?) = tracking.chooseSession(id)
    suspend fun lookupSaved(id: String): LookupResult? = library.recording(id)?.let { repository.lookup(it, ::networkAllowed) }
    suspend fun manualLookup(id: String, title: String, artist: String, album: String): String {
        val recording = requireNotNull(library.recording(id))
        if (!networkAllowed()) return "offline"
        val query = recording.copy(title = title.trim(), artists = listOf(artist.trim()), album = album.ifBlank { null }, durationMs = null)
        require(query.title.isNotBlank() && artist.isNotBlank()) { "Enter title and artist to search." }
        return when (val result = provider.search(query)) {
            is ProviderResult.Failed -> result.reason
            is ProviderResult.Songs -> {
                val candidates = LyricsMatcher.versions(query, result.songs).map { it.copy(id = it.id + ":manual", confidence = MatchConfidence.UNCERTAIN) }
                if (candidates.isEmpty()) "not_found" else { library.saveCandidates(recording, candidates); "available" }
            }
        }
    }
    private fun startService(action: String) {
        try { ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java).setAction(action)) }
        catch (_: IllegalStateException) { startFailed("Open Singalong and try again to start the background session.") }
        catch (_: SecurityException) { startFailed("Check music and notification access, then try again.") }
    }
    private fun startFailed(reason: String) {
        serviceStopped(); context.stopService(Intent(context, TrackingService::class.java)); message(reason)
    }
    fun serviceStarted() {
        startPending = false
        mutableState.update { it.copy(tracking = gate.active, message = null) }
        reader.tracking(gate.active)
        if (gate.active) {
            tracking.start()
            if (state.value.listenerConnected) observer.start()
            else NotificationListenerService.requestRebind(ComponentName(context, MediaNotificationListener::class.java))
        }
    }
    fun serviceStopped() {
        startPending = false; gate.disable(); tracking.stop(); observer.stop(); reader.tracking(false)
        collectionJobs.values.toList().forEach { it.cancel() }; collectionJobs.clear()
        provider.cancelRequests()
        closePanel()
        mutableState.update { it.copy(tracking = false, overlayVisible = false) }
    }
    fun listenerConnected(connected: Boolean) {
        mutableState.update { it.copy(listenerConnected = connected) }
        if (connected && gate.active && state.value.tracking) observer.start()
        if (!connected) { observer.stop(); if (state.value.tracking && !hasMusicAccess()) accessLost() }
    }
    private fun accessLost() = startFailed("Music access was removed. Restore access in settings.")
    fun overlayVisible(visible: Boolean) { mutableState.update { it.copy(overlayVisible = visible) } }
    fun message(message: String?) { mutableState.update { it.copy(message = message) } }
    private fun collectLyrics(recording: Recording) {
        if (collectionJobs[recording.id]?.isActive == true) return
        collectionJobs[recording.id] = scope.launch {
            try {
                repeat(3) {
                    val result = repository.lookup(recording, ::networkAllowed)
                    reader.collected(recording)
                    if (result.status !in ReaderCoordinator.retryable) return@launch
                    combine(connection, settings.state) { net, prefs -> net.permits(prefs.wifiOnly) }.first { it }
                    if (result.status != "offline") delay(result.retryAfterMs ?: 30_000)
                }
            } finally { collectionJobs.remove(recording.id) }
        }
    }
}
