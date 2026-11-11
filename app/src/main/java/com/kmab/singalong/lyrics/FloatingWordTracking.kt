package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine

/** Supplied timestamps only: plain and manually browsed lyrics need no word processing. */
class FloatingWordTracking private constructor(line: LyricLine, lineEndMs: Long?) {
    val ranges = WordSync.ranges(line.words)
    private val timeline = WordSync.Timeline(line.words, lineEndMs)

    fun emphasis(index: Int, positionMs: Long?, delayMs: Long) = timeline.emphasis(index, positionMs, delayMs)

    companion object {
        fun create(line: LyricLine, lineEndMs: Long?, enabled: Boolean, following: Boolean): FloatingWordTracking? {
            // Keep provider extremes readable without rebuilding huge styled strings on every frame.
            if (!enabled || !following || line.words.size > 128 || line.text.length > 4096 || !WordSync.valid(line)) return null
            return FloatingWordTracking(line, lineEndMs)
        }
    }
}
