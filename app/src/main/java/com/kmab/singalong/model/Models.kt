package com.kmab.singalong.model

data class Recording(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String?,
    val durationMs: Long?,
    val versionLabel: String? = null,
)

enum class PlaybackStatus { PLAYING, PAUSED, BUFFERING, STOPPED, UNKNOWN }

data class PlaybackSnapshot(
    val sessionId: String,
    val packageName: String,
    val playerName: String,
    val recording: Recording?,
    val status: PlaybackStatus,
    val positionMs: Long?,
    val observedAtMs: Long,
    val speed: Float,
)

interface PlaybackObserver {
    val sessions: kotlinx.coroutines.flow.StateFlow<List<PlaybackSnapshot>>
    fun start()
    fun stop()
}
