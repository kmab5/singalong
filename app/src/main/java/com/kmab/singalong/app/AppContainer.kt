package com.kmab.singalong.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kmab.singalong.tracking.AndroidPlaybackObserver
import com.kmab.singalong.tracking.MediaNotificationListener
import com.kmab.singalong.tracking.TrackingGate
import com.kmab.singalong.tracking.TrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FoundationState(
    val tracking: Boolean = false,
    val listenerConnected: Boolean = false,
    val overlayVisible: Boolean = false,
    val message: String? = null,
)

/** P0 keeps lightweight setup preferences. P1 migrates preferences to DataStore. */
class AppContainer(private val context: Context) {
    private val preferences = context.getSharedPreferences("foundation", Context.MODE_PRIVATE)
    val gate = TrackingGate()
    var appOpened = false
        private set
    private val mutableState = MutableStateFlow(FoundationState())
    val state = mutableState.asStateFlow()
    val observer = AndroidPlaybackObserver(context) { accessLost() }
    val savedTrackingEnabled get() = preferences.getBoolean("tracking_enabled", false)

    fun hasMusicAccess(): Boolean = context.packageName in
        NotificationManagerCompat.getEnabledListenerPackages(context)

    fun onAppOpened() {
        appOpened = true
        if (savedTrackingEnabled && !state.value.tracking) {
            if (hasMusicAccess()) {
                gate.onAppOpened(true)
                startService(TrackingService.ACTION_START)
            } else message("Restore music access to resume tracking.")
        }
    }

    fun enableTracking() {
        if (!hasMusicAccess()) {
            message("Allow music access first, then start tracking.")
            return
        }
        preferences.edit().putBoolean("tracking_enabled", true).apply()
        gate.onAppOpened(true)
        startService(TrackingService.ACTION_START)
    }

    fun disableTracking() {
        preferences.edit().putBoolean("tracking_enabled", false).apply()
        gate.disable()
        observer.stop()
        mutableState.update { it.copy(tracking = false) }
        context.stopService(Intent(context, TrackingService::class.java))
    }

    fun showOverlayPreview() {
        if (!appOpened) return
        startService(TrackingService.ACTION_SHOW_OVERLAY)
    }

    private fun startService(action: String) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java).setAction(action))
        } catch (_: IllegalStateException) {
            serviceStartFailed("Open Singalong and try again. Android could not start the background session.")
        } catch (_: SecurityException) {
            serviceStartFailed("Check music and notification access, then try again.")
        }
    }

    private fun serviceStartFailed(reason: String) {
        // Revocation must clear live observation as well as the authorization flag.
        // Preserve the user's preference so a later visible launch can retry.
        serviceStopped()
        context.stopService(Intent(context, TrackingService::class.java))
        message(reason)
    }

    fun serviceStarted() {
        mutableState.update { it.copy(tracking = gate.active, message = null) }
        if (gate.active) {
            if (state.value.listenerConnected) observer.start()
            else NotificationListenerService.requestRebind(ComponentName(context, MediaNotificationListener::class.java))
        }
    }

    fun serviceStopped() {
        gate.disable()
        observer.stop()
        mutableState.update { it.copy(tracking = false, overlayVisible = false) }
    }

    fun listenerConnected(connected: Boolean) {
        mutableState.update { it.copy(listenerConnected = connected) }
        if (connected && gate.active && state.value.tracking) observer.start()
        if (!connected) {
            observer.stop()
            if (state.value.tracking && !hasMusicAccess()) accessLost()
        }
    }

    private fun accessLost() {
        gate.disable()
        mutableState.update { it.copy(tracking = false, message = "Music access was removed. Restore access in settings.") }
        context.stopService(Intent(context, TrackingService::class.java))
    }

    fun overlayVisible(visible: Boolean) = mutableState.update { it.copy(overlayVisible = visible) }
    fun message(message: String?) = mutableState.update { it.copy(message = message) }
}
