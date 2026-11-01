package com.kmab.singalong.library

import com.kmab.singalong.model.Recording
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

object RecordingIdentity {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")

    fun artists(recording: Recording): String = recording.artists.joinToString(" | ") { normalize(it) }

    fun compatible(a: Recording, b: Recording): Boolean =
        normalize(a.title) == normalize(b.title) && artists(a) == artists(b) &&
            (a.album.isNullOrBlank() || b.album.isNullOrBlank() || normalize(a.album) == normalize(b.album)) &&
            (a.durationMs == null || b.durationMs == null || abs(a.durationMs - b.durationMs) <= 2_000)

    /** Only within one uninterrupted media session; missing fields may be enriched. */
    fun sameObservation(a: Recording, b: Recording): Boolean =
        normalize(a.title) == normalize(b.title) &&
            (a.artists.isEmpty() || b.artists.isEmpty() || artists(a) == artists(b)) &&
            (a.album.isNullOrBlank() || b.album.isNullOrBlank() || normalize(a.album) == normalize(b.album)) &&
            (a.durationMs == null || b.durationMs == null || abs(a.durationMs - b.durationMs) <= 2_000)

    fun enrich(existing: Recording, incoming: Recording) = existing.copy(
        title = incoming.title.ifBlank { existing.title },
        artists = incoming.artists.ifEmpty { existing.artists },
        album = incoming.album?.takeIf(String::isNotBlank) ?: existing.album,
        durationMs = incoming.durationMs ?: existing.durationMs,
        versionLabel = incoming.versionLabel ?: existing.versionLabel,
    )
}
