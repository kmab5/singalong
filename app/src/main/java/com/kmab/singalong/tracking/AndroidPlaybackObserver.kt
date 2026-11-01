package com.kmab.singalong.tracking

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.kmab.singalong.model.PlaybackObserver
import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.model.PlaybackStatus
import com.kmab.singalong.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reads only media-session metadata. Never reads or stores notification text. */
class AndroidPlaybackObserver(
    private val context: Context,
    private val onAccessLost: () -> Unit,
) : PlaybackObserver {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val mutableSessions = MutableStateFlow<List<PlaybackSnapshot>>(emptyList())
    override val sessions = mutableSessions.asStateFlow()
    private val controllers = linkedMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private var started = false
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { attach(it.orEmpty()) }

    // Enabled notification listeners may read sessions without privileged
    // MEDIA_CONTENT_CONTROL. The caller checks access; SecurityException handles revocation.
    @SuppressLint("MissingPermission")
    override fun start() {
        if (started) return
        try {
            manager.addOnActiveSessionsChangedListener(sessionListener, component, handler)
            started = true
            attach(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            stop()
            onAccessLost()
        }
    }

    override fun stop() {
        if (started) manager.removeOnActiveSessionsChangedListener(sessionListener)
        started = false
        controllers.values.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        controllers.clear()
        mutableSessions.value = emptyList()
    }

    private fun attach(next: List<MediaController>) {
        if (!started) return
        controllers.values.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        controllers.clear()
        next.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() {
                    controllers.remove(controller.sessionToken)?.let { it.first.unregisterCallback(it.second) }
                    publish()
                }
            }
            controllers[controller.sessionToken] = controller to callback
            controller.registerCallback(callback, handler)
        }
        publish()
    }

    private fun publish() {
        if (!started) return
        mutableSessions.value = controllers.values.map { (controller, _) ->
            val metadata = controller.metadata
            val playback = controller.playbackState
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val status = when (playback?.state) {
                PlaybackState.STATE_PLAYING -> PlaybackStatus.PLAYING
                PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
                PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING -> PlaybackStatus.BUFFERING
                PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE -> PlaybackStatus.STOPPED
                else -> PlaybackStatus.UNKNOWN
            }
            PlaybackSnapshot(
                // Process-only diagnostic ID. P1 supplies durable recording identity.
                sessionId = controller.sessionToken.hashCode().toString(),
                packageName = controller.packageName,
                playerName = runCatching {
                    val info = context.packageManager.getApplicationInfo(controller.packageName, 0)
                    context.packageManager.getApplicationLabel(info).toString()
                }.getOrDefault(controller.packageName),
                recording = title?.let {
                    Recording(
                        id = "${controller.packageName}:$it:$artist",
                        title = it,
                        artists = listOfNotNull(artist),
                        album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                        durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { d -> d > 0 },
                    )
                },
                status = status,
                positionMs = playback?.position?.takeIf { it >= 0 },
                observedAtMs = playback?.lastPositionUpdateTime ?: 0,
                speed = playback?.playbackSpeed ?: 0f,
            )
        }
    }
}
