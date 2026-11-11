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
    val overlayVisible: Boolean = false, val message: String? = null,
    val trackingStarting: Boolean = false, val trackingStopping: Boolean = false)

private data class PanelObservation(val songId: String?, val playing: Boolean?, val status: String, val hasLyrics: Boolean, val retrying: Boolean)

class AppContainer(private val context: Context) {
    private val mutableState = MutableStateFlow(FoundationState())
    val state = mutableState.asStateFlow()
    private val mutableOpenNow = MutableStateFlow(false)
    val openNowRequested = mutableOpenNow.asStateFlow()
    fun openNow() { mutableOpenNow.value = true; minimizePanel() }
    fun openedNow() { mutableOpenNow.value = false }
    private val mutableCard = MutableStateFlow<com.kmab.singalong.cards.CardRequest?>(null)
    val card = mutableCard.asStateFlow()
    private val mutableCardLoading = MutableStateFlow(false)
    val cardLoading = mutableCardLoading.asStateFlow()
    private var cardJob: Job? = null
    private var cardGeneration = 0L
    fun closeCard() { cardGeneration++; cardJob?.cancel(); mutableCard.value = null; mutableCardLoading.value = false }
    fun openCard(recording: Recording?, version: LyricVersion, index: Int, sessionId: String? = null) {
        val content = com.kmab.singalong.cards.CardContent.capture(recording, version, index) ?: return
        val artwork = sessionId?.let { observer.cardArtwork(it, content.recording) }
        closeCard()
        val generation = cardGeneration
        mutableCardLoading.value = true
        reader.follow(false)
        cardJob = scope.launch {
            try {
                val image = artwork?.first?.let { original -> withContext(Dispatchers.Default) {
                    if (original.isRecycled) null else com.kmab.singalong.cards.CardImages.bounded(original)
                } } ?: artwork?.second?.let { com.kmab.singalong.cards.CardImages.load(context, it) }
                // A player may reuse a mutable 'current art' URI. Validate again after decoding,
                // then retain only the owned pixels, never a URI to reload on rotation.
                val stillMatching = sessionId?.let { observer.cardArtwork(it, content.recording) }
                    ?.let { it.first != null || it.second != null } == true
                if (generation == cardGeneration) mutableCard.value = com.kmab.singalong.cards.CardRequest(content, image.takeIf { stillMatching })
            } finally {
                if (generation == cardGeneration) mutableCardLoading.value = false
            }
        }
    }
    fun nowAppearance(value: com.kmab.singalong.settings.NowAppearance) { scope.launch { settings.nowAppearance(value) } }
    fun floatingAppearance(value: com.kmab.singalong.settings.FloatingAppearance) { scope.launch { settings.floatingAppearance(value) } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
        if (error is java.io.IOException || error is android.database.sqlite.SQLiteException) {
            stopTrackingWork()
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
    private val provider = FallbackLyricsProvider(LrclibClient(permitsNetwork = ::networkAllowed), LyricsOvhClient(permitsNetwork = ::networkAllowed))
    private val repository = LyricsRepository(library, provider)
    private val folderProvider = FallbackLyricsProvider(LrclibClient(permitsNetwork = ::networkAllowed), LyricsOvhClient(permitsNetwork = ::networkAllowed))
    private val folderSearch = LyricSearch(folderProvider)
    val folderScan = FolderScanCoordinator(CoroutineScope(scope.coroutineContext + Dispatchers.IO), object : FolderImportStore {
        override suspend fun hasLyrics(recording: Recording): Boolean = storage { library.folderHasLyrics(recording) }
        override suspend fun save(recording: Recording, versions: List<LyricVersion>): Boolean = storage { library.saveFolderMatch(recording, versions) }
        private suspend fun <T> storage(block: suspend () -> T): T = try { block() }
            catch (error: android.database.sqlite.SQLiteException) { throw java.io.IOException("Folder import storage unavailable", error) }
    }, { folderSearch.search(it, ::networkAllowed) }, ::networkAllowed, {
        combine(connection, settings.state) { net, prefs -> prefs.ready && net.permits(prefs.wifiOnly) }.first { it }
    })
    fun scanFolder(uri: android.net.Uri) { folderScan.start(AudioFolderSource(context.applicationContext).files(uri)) }
    private val collectionJobs = mutableMapOf<String, Job>()
    val tracking = TrackingCoordinator(scope, observer, settings.state, library, SystemClock::elapsedRealtime, System::currentTimeMillis, ::collectLyrics)
    private val translationProvider = com.kmab.singalong.translation.MyMemoryProvider(
        detector = com.kmab.singalong.translation.LocalLanguageDetector(),
        cache = com.kmab.singalong.translation.TranslationSegmentCache(context),
        permitsNetwork = { networkAllowed() && settings.state.value.translationEnabled },
    )
    val translations = com.kmab.singalong.translation.TranslationRepository(library, translationProvider)
    val reader = ReaderCoordinator(scope, tracking.current, library, repository, ::networkAllowed, SystemClock::elapsedRealtime, translations,
        wordTrackingEnabled = { screenAllowed && activityVisible && readerVisible && settings.state.value.now.wordTrackingEnabled },
        translationEnabled = { settings.state.value.translationEnabled })
    private val mutablePanel = MutableStateFlow(OverlayState())
    val panel = mutablePanel.asStateFlow()
    private val mutablePlayers = MutableStateFlow<List<PlayerApp>>(emptyList())
    val players = mutablePlayers.asStateFlow()
    var userSessionAuthorized = false; private set
    private val startPending get() = state.value.trackingStarting
    private val startRequests = TrackingStartRequests { pending ->
        mutableState.update { it.copy(trackingStarting = pending) }
    }
    private var serviceRunning = false
    private var readerVisible = false
    private var activityVisible = false
    private var screenAllowed = true

    init {
        scope.launch { combine(connection, settings.state.map { it.wifiOnly to it.translationEnabled }.distinctUntilChanged()) { net, prefs -> net to prefs }.distinctUntilChanged().collect {
            if (!networkAllowed()) { provider.cancelRequests(); folderProvider.cancelRequests() }
            if (!networkAllowed() || !settings.state.value.translationEnabled) translationProvider.cancelRequests()
            reader.networkChanged()
        } }
        scope.launch {
            combine(panel, reader.state.map { state -> PanelObservation(state.snapshot?.recording?.id,
                when (state.snapshot?.status) { PlaybackStatus.PLAYING -> true; PlaybackStatus.PAUSED -> false; else -> null },
                state.status, state.version != null, state.retryInSeconds != null) }.distinctUntilChanged())
                { panel, observation -> panel to observation }
                .distinctUntilChanged().collectLatest { (panel, observation) ->
                    updateDemand()
                    fun observed(state: OverlayState) = state.observe(observation.songId, observation.playing, observation.status,
                        observation.hasLyrics, observation.retrying, SystemClock.elapsedRealtime())
                    val next = observed(panel)
                    if (next != panel) mutablePanel.value = next
                    else panel.nextCheckAt()?.let { deadline ->
                        delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
                        mutablePanel.update(::observed)
                    }
                }
        }
    }
    fun hasMusicAccess() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    private fun networkAllowed() = connection.value.permits(settings.state.value.wifiOnly)
    fun onAppOpened() {
        userSessionAuthorized = true
        val expected = startRequests.current
        scope.launch {
            val prefs = settings.state.first { it.ready }
            if (startRequests.isCurrent(expected) && prefs.trackingEnabled && !state.value.tracking && !startPending) {
                beginTracking(startRequests.begin())
            }
            mutablePlayers.value = PlayerCatalog(context).installed()
        }
    }
    fun enableTracking() {
        if (startPending || state.value.tracking || state.value.trackingStopping) return
        // A tile click is explicit user authorization, including in a fresh process.
        userSessionAuthorized = true
        val expected = startRequests.begin()
        scope.launch {
            settings.state.first { it.ready }
            if (!startRequests.isCurrent(expected)) return@launch
            if (!canStart()) { startRequests.finish(expected); return@launch }
            settings.tracking(true)
            if (startRequests.isCurrent(expected)) beginTracking(expected)
        }
    }
    private fun canStart(): Boolean {
        if (!hasMusicAccess()) { message("Allow music access first."); return false }
        if (settings.state.value.allowedPlayers.isEmpty()) { message("Select at least one music app in settings."); return false }
        return true
    }
    private fun beginTracking(request: Long) {
        if (state.value.trackingStopping || !canStart()) { startRequests.finish(request); return }
        gate.onAppOpened(true)
        startService(TrackingService.ACTION_START)
    }
    fun disableTracking() {
        stopTrackingWork()
        scope.launch { settings.tracking(false) }
        context.stopService(Intent(context, TrackingService::class.java))
    }
    fun allowPlayer(packageName: String, allowed: Boolean) { scope.launch { settings.allowPlayer(packageName, allowed) } }
    fun wifiOnly(enabled: Boolean) { scope.launch { settings.wifiOnly(enabled) } }
    fun notificationRequested() { scope.launch { settings.notificationRequested() } }
    fun translation(enabled: Boolean) { scope.launch { settings.translation(enabled); reader.request() } }
    fun translationSize(value: Float) { scope.launch { settings.translationSize(value) } }
    fun opacity(value: Float) { scope.launch { settings.opacity(value) } }
    fun minimalHeight(value: Float) { scope.launch { settings.minimalHeight(value) } }
    fun minimalPanel(enabled: Boolean) { scope.launch {
        settings.minimal(enabled)
        mutablePanel.update { it.copy(controlsVisible = false) }
    } }
    fun revealPanelControls() { mutablePanel.update { it.copy(controlsVisible = true) } }
    fun geometry(x: Float, y: Float, width: Float, height: Float) { scope.launch { settings.geometry(x, y, width, height) } }
    fun activityVisible(visible: Boolean) { activityVisible = visible; updateDemand() }
    fun readerVisible(visible: Boolean) { readerVisible = visible; updateDemand() }
    fun screenAllowed(allowed: Boolean) { screenAllowed = allowed; updateDemand() }
    private var previousDemand = false
    private fun updateDemand() {
        val demand = screenAllowed && ((activityVisible && readerVisible) || panel.value.needsLyrics)
        if (demand != previousDemand) { previousDemand = demand; reader.visible(demand) }
    }
    fun showLyrics() {
        if (!userSessionAuthorized || state.value.trackingStopping) return
        val expected = startRequests.begin()
        scope.launch {
            settings.state.first { it.ready }
            if (!startRequests.isCurrent(expected)) return@launch
            if (!gate.active) {
                if (!canStart()) { startRequests.finish(expected); return@launch }
                settings.tracking(true)
                if (!startRequests.isCurrent(expected)) return@launch
                gate.onAppOpened(true)
            }
            startService(TrackingService.ACTION_SHOW_OVERLAY)
        }
    }
    fun showPanel() { mutablePanel.update { it.show() }; updateDemand() }
    fun closePanel() {
        mutablePanel.update { it.close() }; updateDemand()
        if (!gate.active) context.stopService(Intent(context, TrackingService::class.java))
    }
    fun minimizePanel() { mutablePanel.update { it.minimize() }; updateDemand() }
    fun expandPanel() { scope.launch {
        val wasMinimal = settings.state.value.minimalMode
        settings.minimal(false)
        mutablePanel.update { it.copy(expanded = wasMinimal || !it.expanded, controlsVisible = false) }
    } }
    fun chooseSession(id: String?) = tracking.chooseSession(id)
    suspend fun lookupSaved(id: String): LookupResult? = library.recording(id)?.let { repository.lookup(it, ::networkAllowed, forceRefresh = true) }
    suspend fun translationForSaved(version: LyricVersion, onProgress: suspend (com.kmab.singalong.translation.TranslationRecord) -> Unit = {}) =
        translations.resolve(version, settings.state.value.translationEnabled, true, networkAllowed(), onProgress)
    suspend fun manualLookup(id: String, title: String, artist: String, album: String): String {
        val recording = requireNotNull(library.recording(id))
        if (!networkAllowed()) return "offline"
        val query = recording.copy(title = title.trim(), artists = listOf(artist.trim()), album = album.ifBlank { null }, durationMs = null)
        require(SearchQueries.plan(query).isNotEmpty()) { "Enter a title and artist, or a filename such as Artist - Title.mp3." }
        val result = LyricSearch(provider).search(query, ::networkAllowed)
        val candidates = result.versions.map { it.copy(id = it.id + ":manual", confidence = MatchConfidence.UNCERTAIN) }
        if (candidates.isNotEmpty()) library.saveCandidates(recording, candidates)
        return result.status
    }
    private fun startService(action: String) {
        try { ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java).setAction(action)) }
        catch (_: IllegalStateException) { startFailed("Open Singalong and try again to start the background session.") }
        catch (_: SecurityException) { startFailed("Check music and notification access, then try again.") }
    }
    private fun startFailed(reason: String) {
        stopTrackingWork(); context.stopService(Intent(context, TrackingService::class.java)); message(reason)
    }
    fun serviceCreated() { serviceRunning = true }
    fun serviceStarted() {
        startRequests.finish(startRequests.current)
        mutableState.update { it.copy(tracking = gate.active, trackingStopping = false, message = null) }
        reader.tracking(gate.active)
        if (gate.active) {
            tracking.start()
            if (state.value.listenerConnected) observer.start()
            else NotificationListenerService.requestRebind(ComponentName(context, MediaNotificationListener::class.java))
        }
    }
    fun serviceStopped() {
        serviceRunning = false
        stopTrackingWork()
    }
    private fun stopTrackingWork() {
        startRequests.cancel(); gate.disable(); tracking.stop(); observer.stop(); reader.tracking(false)
        collectionJobs.values.toList().forEach { it.cancel() }; collectionJobs.clear()
        provider.cancelRequests()
        translationProvider.cancelRequests()
        closePanel()
        mutableState.update { it.copy(tracking = false, overlayVisible = false, trackingStopping = serviceRunning) }
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
                    val result = repository.lookup(recording, ::networkAllowed, forceRefresh = it > 0)
                    reader.collected(recording)
                    if (result.status !in ReaderCoordinator.retryable) return@launch
                    combine(connection, settings.state) { net, prefs -> net.permits(prefs.wifiOnly) }.first { it }
                    if (result.status != "offline") delay(maxOf(result.retryAfterMs ?: 0, RetryPolicy.delayMs(it)))
                }
            } finally { collectionJobs.remove(recording.id) }
        }
    }
}
