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
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
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
                com.kmab.singalong.ui.CrashPrompt { url ->
                    try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                    catch (_: ActivityNotFoundException) { container.message("No browser is available. Your crash log is saved; open feedback from About later.") }
                }
                FoundationScreen(
                    container, permissions.value,
                    FoundationActions(
                        musicAccess = { openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        overlayAccess = { openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())) },
                        notificationAccess = ::requestNotifications,
                        trackingNotificationSettings = {
                            com.kmab.singalong.tracking.TrackingNotification.ensureChannel(this)
                            openSettings(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                .putExtra(Settings.EXTRA_CHANNEL_ID, com.kmab.singalong.tracking.TrackingNotification.CHANNEL))
                        },
                        openLink = { url ->
                            try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                            catch (_: ActivityNotFoundException) { container.message("No browser is available to open this link.") }
                        },
                    ),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshPermissions()
        container.onAppOpened()
        container.activityVisible(true)
        if (intent.action == ACTION_OPEN_NOW) { container.openNow(); intent.action = null }
        if (intent.action == ACTION_SHOW_LYRICS) {
            container.showLyrics()
            intent.action = null
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    override fun onStop() {
        container.activityVisible(false)
        super.onStop()
    }

    private fun refreshPermissions() {
        permissions.value = PermissionStatus(
            music = container.hasMusicAccess(),
            overlay = Settings.canDrawOverlays(this),
            notifications = NotificationManagerCompat.from(this).areNotificationsEnabled(),
        )
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !permissions.value.notifications &&
            !container.settings.state.value.notificationRequested
        ) {
            container.notificationRequested()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_OPEN_NOW) { container.openNow(); intent.action = null }
        if (intent.action == ACTION_SHOW_LYRICS) { container.showLyrics(); intent.action = null }
    }
    companion object {
        const val ACTION_SHOW_LYRICS = "com.kmab.singalong.SHOW_LYRICS"
        const val ACTION_OPEN_NOW = "com.kmab.singalong.OPEN_NOW"
    }
}
