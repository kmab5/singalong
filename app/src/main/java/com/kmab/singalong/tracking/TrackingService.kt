package com.kmab.singalong.tracking

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.content.res.Configuration
import android.provider.Settings
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kmab.singalong.app.SingalongApplication
import com.kmab.singalong.overlay.OverlayWindowHost
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TrackingService : Service() {
    private val container get() = (application as SingalongApplication).container
    private lateinit var overlay: OverlayWindowHost
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val screenAllowed = MutableStateFlow(false)
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshScreen()
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayWindowHost(this, container)
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshScreen()
        scope.launch {
            combine(container.panel, container.settings.state, screenAllowed) { panel, settings, allowed -> Triple(panel, settings, allowed) }
                .collect { (panel, settings, allowed) ->
                    if (!overlay.render(panel, settings, allowed)) {
                        container.closePanel()
                        container.message("Allow display over other apps to open floating lyrics.")
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            container.disableTracking()
            stopSelf()
            return START_NOT_STICKY
        }
        // Rebinding or Android recreating a service is not a user opening the app.
        if (!container.appOpened || intent == null || (intent.action == ACTION_START && !container.gate.active)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this, TrackingNotification.ID, TrackingNotification.create(this, container.gate.active),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
        } catch (_: SecurityException) {
            container.message("Android could not start the session. Check permissions and try again.")
            stopSelf()
            return START_NOT_STICKY
        }
        container.serviceStarted()
        if (intent.action == ACTION_SHOW_OVERLAY) {
            val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
            if (locked || !Settings.canDrawOverlays(this)) {
                container.message("Unlock your phone and allow display over other apps to open lyrics.")
                if (!container.gate.active) stopSelf()
            } else container.showPanel()
        }
        return START_NOT_STICKY
    }

    private fun refreshScreen() {
        val allowed = getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        screenAllowed.value = allowed
        container.screenAllowed(allowed)
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); overlay.configurationChanged() }

    override fun onDestroy() {
        scope.cancel()
        overlay.hide()
        unregisterReceiver(screenReceiver)
        container.serviceStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.kmab.singalong.START"
        const val ACTION_STOP = "com.kmab.singalong.STOP"
        const val ACTION_SHOW_OVERLAY = "com.kmab.singalong.SHOW_OVERLAY"
    }
}
