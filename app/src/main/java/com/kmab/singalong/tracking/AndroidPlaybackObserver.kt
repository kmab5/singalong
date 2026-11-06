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
import com.kmab.singalong.library.RecordingIdentity
import java.util.UUID
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
    private val observedRecordings = mutableMapOf<MediaSession.Token, Recording>()
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { attach(it.orEmpty()) }

    /** Snapshot only artwork whose current metadata still matches the selected recording. */
    fun cardArtwork(sessionId: String, recording: Recording): Pair<android.graphics.Bitmap?, android.net.Uri?> {
        val metadata = cardMetadata(sessionId, recording) ?: return null to null
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val uri = (metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI))
            ?.let(android.net.Uri::parse)?.takeIf { it.scheme in setOf("content", "file", "android.resource") }
        return bitmap to uri
    }

    fun cardFileDetails(sessionId: String, recording: Recording): com.kmab.singalong.cards.CardFileDetails {
        val metadata = cardMetadata(sessionId, recording)
        val file = com.kmab.singalong.cards.CardFileDetails.from(metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI))
        return if (file.name != null) file else com.kmab.singalong.cards.CardFileDetails.from(recording.fileName)
    }

    private fun cardMetadata(sessionId: String, recording: Recording): MediaMetadata? {
        val controller = controllers.values.firstOrNull { it.first.sessionToken.hashCode().toString() == sessionId }?.first
            ?: return null
        val metadata = try { controller.metadata } catch (_: SecurityException) { null } ?: return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
        val current = Recording("artwork", title, listOfNotNull(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }),
            metadata.getString(MediaMetadata.METADATA_KEY_ALBUM), metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).takeIf { it > 0 })
        if (!RecordingIdentity.compatible(recording, current)) return null
        return metadata
    }

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
        observedRecordings.clear()
        mutableSessions.value = emptyList()
    }

    private fun attach(next: List<MediaController>) {
        if (!started) return
        controllers.values.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        controllers.clear()
        observedRecordings.keys.retainAll(next.map { it.sessionToken }.toSet())
        next.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() {
                    observedRecordings.remove(controller.sessionToken)
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
                sessionId = controller.sessionToken.hashCode().toString(),
                packageName = controller.packageName,
                playerName = try {
                    val info = context.packageManager.getApplicationInfo(controller.packageName, 0)
                    context.packageManager.getApplicationLabel(info).toString()
                } catch (_: android.content.pm.PackageManager.NameNotFoundException) { controller.packageName },
                recording = title?.takeIf(String::isNotBlank)?.let {
                    val incoming = Recording(
                        id = UUID.randomUUID().toString(),
                        title = it,
                        artists = listOfNotNull(artist?.takeIf(String::isNotBlank)),
                        album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                        durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { d -> d > 0 },
                    )
                    val old = observedRecordings[controller.sessionToken]
                    val stable = if (old != null && RecordingIdentity.sameObservation(old, incoming)) RecordingIdentity.enrich(old, incoming) else incoming
                    observedRecordings[controller.sessionToken] = stable
                    stable
                },
                status = status,
                positionMs = playback?.position?.takeIf { it >= 0 },
                observedAtMs = playback?.lastPositionUpdateTime?.takeIf { it > 0 } ?: android.os.SystemClock.elapsedRealtime(),
                speed = playback?.playbackSpeed ?: 0f,
            )
        }
    }
}
