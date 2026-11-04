package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricVersion
import kotlin.math.abs

/** Rank eligible matches, never relax title/artist/edition matching or override a user's selection. */
object LyricPreference {
    fun sorted(versions: List<LyricVersion>, durationMs: Long?) = versions.sortedWith(
        compareByDescending<LyricVersion> { it.lines.any { line -> line.startMs != null } }
            .thenBy { version ->
                if (durationMs != null && durationMs > 0 && version.providerDurationMs != null)
                    abs(durationMs.toDouble() - version.providerDurationMs) else Double.MAX_VALUE
            }.thenBy { it.confidence.ordinal }.thenBy { it.id },
    )
}
