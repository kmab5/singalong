package com.kmab.singalong.lyrics

import com.kmab.singalong.model.LyricLine

/** Opt-in reading aid, never provider timing. Computed in memory; original lines remain untouched. */
object EstimatedTimeline {
    fun starts(lines: List<LyricLine>, durationMs: Long): Map<Int, Long> {
        if (durationMs <= 0) return emptyMap()
        val weights = lines.mapIndexedNotNull { index, line ->
            if (line.text.isBlank()) null else index to line.text.count { !it.isWhitespace() }.coerceAtLeast(1).toLong()
        }
        val total = weights.sumOf { it.second }.coerceAtLeast(1)
        var consumed = 0L
        return weights.associate { (index, weight) ->
            val start = (consumed.toDouble() / total * durationMs).toLong()
            consumed += weight
            index to start
        }
    }
    fun at(lines: List<LyricLine>, positionMs: Long, durationMs: Long, delayMs: Long): LyricMoment {
        val time = positionMs - delayMs
        return LyricMoment(starts(lines, durationMs).entries.lastOrNull { it.value <= time }?.key ?: -1)
    }
}
