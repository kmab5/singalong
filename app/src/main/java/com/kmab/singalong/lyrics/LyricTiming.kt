package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*

object LrcParser {
    private val tag = Regex("\\[(\\d{1,4}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)
    fun parse(synced: String?, plain: String?): List<LyricLine> {
        val shift = synced?.let { offset.find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: 0L
        val timed = synced.orEmpty().lineSequence().flatMap { row ->
            val tags = tag.findAll(row).toList()
            val text = if (tags.isEmpty()) "" else row.substring(tags.last().range.last + 1).trim()
            tags.mapNotNull { match ->
                val seconds = match.groupValues[2].toLong()
                if (seconds >= 60) null else {
                    val fraction = match.groupValues[3].padEnd(3, '0').toLong()
                    ((match.groupValues[1].toLong() * 60_000 + seconds * 1000 + fraction + shift).coerceAtLeast(0)) to text
                }
            }.asSequence()
        }.sortedBy { it.first }.toList()
        if (timed.any { it.second.isNotBlank() }) return timed.mapIndexed { index, (ms, text) -> LyricLine("$index", ms, text) }
        return plain.orEmpty().lines().takeIf { rows -> rows.any { it.isNotBlank() } }.orEmpty()
            .mapIndexed { index, text -> LyricLine("$index", null, text) }
    }
}

object PlaybackClock {
    fun position(snapshot: PlaybackSnapshot?, nowMs: Long): Long? {
        val base = snapshot?.positionMs?.takeIf { it >= 0 } ?: return null
        val extra = if (snapshot.status == PlaybackStatus.PLAYING && snapshot.speed.isFinite() && snapshot.speed > 0)
            ((nowMs - snapshot.observedAtMs).coerceAtLeast(0) * snapshot.speed.toDouble()).toLong() else 0
        return (base + extra).coerceAtLeast(0).let { value -> snapshot.recording?.durationMs?.let { value.coerceAtMost(it) } ?: value }
    }
}

data class LyricMoment(val index: Int = -1, val countdownSeconds: Long? = null)

object LyricTimeline {
    fun at(lines: List<LyricLine>, positionMs: Long, delayMs: Long): LyricMoment {
        val time = positionMs - delayMs
        val index = lines.indexOfLast { it.startMs != null && it.startMs <= time }
        // A blank timed marker explicitly describes an instrumental interval. A long gap does not.
        val next = if (index < 0 || lines[index].text.isBlank()) lines.drop(index + 1).firstOrNull { it.startMs != null && it.text.isNotBlank() } else null
        return LyricMoment(index, next?.startMs?.let { ((it - time).coerceAtLeast(0) + 999) / 1000 })
    }
}
