package com.kmab.singalong.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.singalong.ui.FoundationActions
import com.kmab.singalong.ui.FoundationScreen
import com.kmab.singalong.ui.PermissionStatus
import com.kmab.singalong.ui.theme.SingalongTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as SingalongApplication).container
    private val permissions = mutableStateOf(PermissionStatus())
    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SingalongTheme {
                val state by container.state.collectAsStateWithLifecycle()
                val sessions by container.observer.sessions.collectAsStateWithLifecycle()
                FoundationScreen(
                    state, sessions, permissions.value,
                    FoundationActions(
                        musicAccess = { openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        overlayAccess = { openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())) },
                        notificationAccess = ::requestNotifications,
                        startTracking = container::enableTracking,
                        stopTracking = container::disableTracking,
                        showPreview = container::showOverlayPreview,
                    ),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshPermissions()
        container.onAppOpened()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        permissions.value = PermissionStatus(
            music = container.hasMusicAccess(),
            overlay = Settings.canDrawOverlays(this),
            notifications = NotificationManagerCompat.from(this).areNotificationsEnabled(),
        )
    }

    private fun requestNotifications() {
        val preference = getSharedPreferences("foundation", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !permissions.value.notifications &&
            !preference.getBoolean("notification_requested", false)
        ) {
            preference.edit().putBoolean("notification_requested", true).apply()
            notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            container.message("Open Android settings and find Singalong to change this permission.")
        }
    }
}
