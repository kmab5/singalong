package com.kmab.singalong.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
data class LyricLine(val id: String, val startMs: Long?, val text: String)

@Serializable
enum class LyricOrigin { PROVIDER, IMPORT, PERSONAL }

@Serializable
enum class MatchConfidence { CONFIDENT, UNCERTAIN }

@Serializable
data class LyricVersion(
    val id: String,
    val recordingId: String,
    val contentHash: String,
    val origin: LyricOrigin,
    val lines: List<LyricLine>,
    val sourceName: String?,
    val sourceUrl: String?,
    val confidence: MatchConfidence,
    val providerId: String? = null,
    val attribution: String? = null,
    val retention: String = "personal offline copy; no redistribution grant",
)

data class LibraryEntry(
    val recording: Recording,
    val lastPlayedAtMs: Long?,
    val playCount: Long,
    val hasLyrics: Boolean,
    val lookupStatus: String,
    val favorite: Boolean = false,
)
