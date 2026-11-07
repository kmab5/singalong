package com.kmab.singalong.tracking

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.kmab.singalong.R
import com.kmab.singalong.app.MainActivity
import com.kmab.singalong.app.SingalongApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Bound only while the shade needs the tile. Observation never starts tracking. */
class TrackingTileService : TileService() {
    private val container get() = (application as SingalongApplication).container
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        listening = scope.launch {
            combine(container.state, container.settings.state) { _, _ -> presentation() }
                .collect(::render)
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::toggle) else toggle()
    }

    private fun presentation(): TrackingTileState {
        val state = container.state.value
        val settings = container.settings.state.value
        return TrackingTileState.from(state.tracking, state.trackingStarting, state.trackingStopping,
            settings.ready, container.hasMusicAccess(), settings.allowedPlayers.isNotEmpty())
    }

    private fun toggle() {
        // Re-read after unlocking: tracking or permissions may have changed meanwhile.
        when (presentation().action) {
            TrackingTileAction.START -> container.enableTracking()
            TrackingTileAction.STOP -> container.disableTracking()
            TrackingTileAction.SETUP -> {
                val message = if (!container.hasMusicAccess()) "Allow music access in Singalong first."
                    else "Select at least one music app in Singalong settings."
                container.message(message)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                openSetup()
            }
            TrackingTileAction.WAIT -> Unit
        }
        render(presentation())
    }

    private fun render(model: TrackingTileState) {
        qsTile?.apply {
            label = getString(R.string.tracking_tile_label)
            subtitle = model.subtitle
            contentDescription = "$label, ${model.subtitle}"
            stateDescription = model.subtitle
            state = when (model.action) {
                TrackingTileAction.STOP -> Tile.STATE_ACTIVE
                TrackingTileAction.WAIT -> Tile.STATE_UNAVAILABLE
                else -> Tile.STATE_INACTIVE
            }
            updateTile()
        }
    }

    private fun openSetup() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 3, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else openSetupLegacy(intent)
    }

    // The PendingIntent overload does not exist on Android 12/13. Caller gates this to API < 34.
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun openSetupLegacy(intent: Intent) = startActivityAndCollapse(intent)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
