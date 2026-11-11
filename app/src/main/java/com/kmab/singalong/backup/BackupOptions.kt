package com.kmab.singalong.backup

import com.kmab.singalong.model.*
import kotlinx.serialization.Serializable

@Serializable enum class LyricsBackupMode { FULL, LINKS_METADATA, NONE }

@Serializable data class BackupOptions(
    val lyrics: LyricsBackupMode = LyricsBackupMode.FULL,
    val songMetadata: Boolean = true,
    val history: Boolean = true,
    val settings: Boolean = true,
) {
    val hasContent: Boolean get() = lyrics != LyricsBackupMode.NONE || songMetadata || history || settings
}

/** Provenance without any lyric text. References never count as a downloaded lyric version. */
@Serializable data class LyricReference(
    val id: String,
    val recordingId: String,
    val contentHash: String,
    val origin: LyricOrigin,
    val sourceName: String?,
    val sourceUrl: String?,
    val confidence: MatchConfidence,
    val providerId: String? = null,
    val attribution: String? = null,
    val retention: String = "personal offline copy; no redistribution grant",
    val providerDurationMs: Long? = null,
) {
    companion object {
        fun from(version: LyricVersion) = LyricReference(version.id, version.recordingId, version.contentHash,
            version.origin, version.sourceName, version.sourceUrl, version.confidence, version.providerId,
            version.attribution, version.retention, version.providerDurationMs)
    }
}

/** Selecting a section also carries the minimal recording identity needed to restore it. */
fun BackupDocument.select(options: BackupOptions): BackupDocument {
    require(options.hasContent) { "Choose at least one backup section." }
    require((!options.songMetadata || this.options.songMetadata) && (!options.history || this.options.history) &&
        (!options.settings || this.options.settings) && (options.lyrics == LyricsBackupMode.NONE ||
            this.options.lyrics == LyricsBackupMode.FULL || options.lyrics == this.options.lyrics)) {
        "The source backup does not contain all requested sections."
    }
    val selectedSongs = songs.mapNotNull { song ->
        val references = if (options.lyrics == LyricsBackupMode.LINKS_METADATA)
            (song.lyricReferences + song.versions.map(LyricReference::from)).distinctBy { it.id }
        else if (options.lyrics == LyricsBackupMode.FULL) song.lyricReferences else emptyList()
        val full = options.lyrics == LyricsBackupMode.FULL
        if (!options.songMetadata && !options.history && (!full || song.versions.isEmpty()) && references.isEmpty()) return@mapNotNull null
        song.copy(
            recording = if (options.songMetadata) song.recording else song.recording.identityOnly(),
            plays = if (options.history) song.plays else 0,
            lastPlayed = song.lastPlayed.takeIf { options.history },
            favorite = options.songMetadata && song.favorite,
            versions = if (full) song.versions else emptyList(), selected = song.selected.takeIf { full },
            adjustments = if (full) song.adjustments else emptyList(), translations = if (full) song.translations else emptyList(),
            lyricReferences = references,
        )
    }
    return copy(songs = selectedSongs, preferences = if (options.settings) preferences else emptyMap(), options = options)
}

internal fun Recording.identityOnly() = copy(album = null, durationMs = null, versionLabel = null, fileName = null)
