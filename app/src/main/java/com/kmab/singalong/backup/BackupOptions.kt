package com.kmab.singalong.backup

import com.kmab.singalong.model.*
import com.kmab.singalong.lyrics.LyricPreference
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable enum class LyricsBackupMode { FULL, LINKS_METADATA, NONE }
@Serializable enum class LyricVersionScope { ALL, PREFERRED }

@Serializable data class BackupOptions(
    val lyrics: LyricsBackupMode = LyricsBackupMode.FULL,
    val songMetadata: Boolean = true,
    val history: Boolean = true,
    val settings: Boolean = true,
    val versionScope: LyricVersionScope = LyricVersionScope.ALL,
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
    require(options.lyrics == LyricsBackupMode.NONE || this.options.versionScope != LyricVersionScope.PREFERRED ||
        options.versionScope == LyricVersionScope.PREFERRED) { "This backup contains only preferred lyric versions; it cannot include all versions." }
    val selectedSongs = songs.mapNotNull { song ->
        val preferredOnly = options.lyrics != LyricsBackupMode.NONE && options.versionScope == LyricVersionScope.PREFERRED
        val preferredId = if (preferredOnly) song.preferredLyricId() else null
        val versions = if (preferredOnly) song.versions.filter { it.id == preferredId } else song.versions
        val sourceReferences = if (preferredOnly) song.lyricReferences.filter { it.id == preferredId } else song.lyricReferences
        val references = if (options.lyrics == LyricsBackupMode.LINKS_METADATA)
            (sourceReferences + versions.map(LyricReference::from)).distinctBy { it.id }
        else if (options.lyrics == LyricsBackupMode.FULL) sourceReferences else emptyList()
        val full = options.lyrics == LyricsBackupMode.FULL
        if (!options.songMetadata && !options.history && (!full || versions.isEmpty()) && references.isEmpty()) return@mapNotNull null
        song.copy(
            recording = if (options.songMetadata) song.recording else song.recording.identityOnly(),
            plays = if (options.history) song.plays else 0,
            lastPlayed = song.lastPlayed.takeIf { options.history },
            favorite = options.songMetadata && song.favorite,
            versions = if (full) versions else emptyList(), selected = song.selected.takeIf { full && (!preferredOnly || it == preferredId) },
            adjustments = if (full) song.adjustments.filter { !preferredOnly || it.versionId == preferredId } else emptyList(),
            translations = if (full) song.translations.filter { !preferredOnly || it.versionId == preferredId } else emptyList(),
            lyricReferences = references,
        )
    }
    return copy(songs = selectedSongs, preferences = if (options.settings) preferences else emptyMap(), options = options)
}

private fun BackupSong.preferredLyricId(): String? {
    selected?.takeIf { id -> versions.any { it.id == id } || lyricReferences.any { it.id == id } }?.let { return it }
    LyricPreference.sorted(versions, recording.durationMs).firstOrNull()?.let { return it.id }
    // Provenance alone cannot tell us whether text was synchronized. Rank only known metadata.
    return lyricReferences.minWithOrNull(compareBy<LyricReference> { it.confidence.ordinal }.thenBy {
        val duration = recording.durationMs
        if (duration != null && duration > 0 && it.providerDurationMs != null)
            abs(duration.toDouble() - it.providerDurationMs) else Double.MAX_VALUE
    }.thenBy { it.id })?.id
}

internal fun Recording.identityOnly() = copy(album = null, durationMs = null, versionLabel = null, fileName = null)
