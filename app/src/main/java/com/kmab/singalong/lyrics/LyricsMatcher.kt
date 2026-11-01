package com.kmab.singalong.lyrics

import com.kmab.singalong.library.RecordingIdentity.normalize
import com.kmab.singalong.model.*
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import kotlin.math.abs

@Serializable
data class ProviderSong(
    val id: Long, val trackName: String, val artistName: String, val albumName: String = "",
    val duration: Double = 0.0, val instrumental: Boolean = false,
    val plainLyrics: String? = null, val syncedLyrics: String? = null,
)

object LyricsMatcher {
    private val qualifiers = Regex("\\b(live|remix|acoustic|instrumental|karaoke|demo|radio edit|extended)\\b")
    private fun comparison(value: String) = normalize(value).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    fun confidence(recording: Recording, song: ProviderSong): MatchConfidence? {
        val title = normalize(recording.title)
        val candidate = normalize(song.trackName)
        if (comparison(title) != comparison(candidate)) return null
        val artist = comparison(recording.artists.joinToString(", "))
        if (artist.isBlank() || artist != comparison(song.artistName)) return null
        if (qualifiers.findAll(title).map { it.value }.toSet() != qualifiers.findAll(candidate).map { it.value }.toSet()) return null
        val closeDuration = recording.durationMs?.let { song.duration > 0 && abs(it - song.duration * 1000) <= 2000 } == true
        if (recording.durationMs != null && song.duration > 0 && abs(recording.durationMs - song.duration * 1000) > maxOf(10_000.0, recording.durationMs * 0.1)) return null
        val albumMatches = !recording.album.isNullOrBlank() && normalize(recording.album) == normalize(song.albumName)
        return if (closeDuration && albumMatches) MatchConfidence.CONFIDENT else MatchConfidence.UNCERTAIN
    }
    fun versions(recording: Recording, songs: List<ProviderSong>): List<LyricVersion> = songs.mapNotNull { song ->
        val confidence = confidence(recording, song) ?: return@mapNotNull null
        val lines = LrcParser.parse(song.syncedLyrics, song.plainLyrics)
        if (lines.isEmpty() || song.instrumental) return@mapNotNull null
        val hash = MessageDigest.getInstance("SHA-256").digest(lines.joinToString("\n") { "${it.startMs}:${it.text}" }.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        LyricVersion("${recording.id}:lrclib:${song.id}:$hash", recording.id, hash, LyricOrigin.PROVIDER, lines,
            "LRCLIB", "https://lrclib.net/api/get/${song.id}", confidence, song.id.toString(),
            "${song.trackName} — ${song.artistName} · ${song.albumName} · ${song.duration.toInt()}s · LRCLIB")
    }.distinctBy { it.id }.sortedBy { it.confidence.ordinal }
}
