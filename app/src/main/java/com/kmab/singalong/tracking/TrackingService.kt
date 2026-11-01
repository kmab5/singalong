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
import android.provider.Settings
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kmab.singalong.app.SingalongApplication
import com.kmab.singalong.overlay.OverlayWindowHost

class TrackingService : Service() {
    private val container get() = (application as SingalongApplication).container
    private lateinit var overlay: OverlayWindowHost
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) closePreview()
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayWindowHost(this, container::overlayVisible)
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
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
            if (locked || !Settings.canDrawOverlays(this) || !overlay.show(::closePreview)) {
                container.message("Unlock your phone and allow display over other apps to open the preview.")
                if (!container.gate.active) stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun closePreview() {
        overlay.hide()
        if (!container.gate.active) stopSelf()
    }

    override fun onDestroy() {
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
        const val ACTION_SHOW_OVERLAY = "com.kmab.singalong.SHOW_PREVIEW"
    }
}
