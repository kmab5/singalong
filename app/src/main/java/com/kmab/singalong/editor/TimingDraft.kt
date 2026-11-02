package com.kmab.singalong.editor

import com.kmab.singalong.lyrics.PlaybackClock
import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.model.PlaybackStatus
import java.util.Locale

object LyricTimestamp {
    const val MAX_MS = 599_999_999L
    private val pattern = Regex("^(\\d{1,4}):(\\d{2})(?:[.](\\d{1,3}))?$")
    fun parse(text: String): Long? {
        if (text.isBlank()) return null
        val match = requireNotNull(pattern.matchEntire(text.trim())) { "Use minutes:seconds, for example 01:23.450, or leave blank." }
        val seconds = match.groupValues[2].toLong()
        require(seconds < 60) { "Seconds must be between 00 and 59." }
        return match.groupValues[1].toLong() * 60_000 + seconds * 1000 + match.groupValues[3].padEnd(3, '0').toLong()
    }
    fun format(ms: Long?): String = ms?.let { String.format(Locale.ROOT, "%02d:%02d.%03d", it / 60_000, it / 1000 % 60, it % 1000) }.orEmpty()
    fun validate(times: List<Long?>) {
        require(times.all { it == null || it in 0..MAX_MS }) { "Times must be between 00:00.000 and 9999:59.999." }
        require(times.filterNotNull().zipWithNext().all { (a, b) -> a <= b }) { "Keep timestamps in lyric order. A later line cannot start before an earlier one." }
    }
}

/** Immutable draft, including unfinished timestamp input, retained when controls are hidden. */
data class TimingDraft(val times: List<Long?>, val selectedIndex: Int = 0, val timeInput: String? = null) {
    init { require(times.isNotEmpty() && selectedIndex in times.indices) }
    val input: String get() = timeInput ?: LyricTimestamp.format(times[selectedIndex])
    fun resolvedTimes(): List<Long?> = if (timeInput == null) times else times.mapIndexed { i, time -> if (i == selectedIndex) LyricTimestamp.parse(timeInput) else time }
    fun select(index: Int): TimingDraft = copy(times = resolvedTimes(), selectedIndex = index, timeInput = null)
    fun setTime(ms: Long?): TimingDraft {
        require(ms == null || ms in 0..LyricTimestamp.MAX_MS) { "Timestamp is outside the supported range." }
        return copy(times = times.mapIndexed { i, time -> if (i == selectedIndex) ms else time }, timeInput = null)
    }
    fun nudge(deltaMs: Long): TimingDraft {
        val current = requireNotNull(LyricTimestamp.parse(input)) { "Set a time before adjusting it." }
        return setTime((current + deltaMs).coerceIn(0, LyricTimestamp.MAX_MS))
    }
    fun stamp(recordingId: String?, snapshot: PlaybackSnapshot?, nowMs: Long): TimingDraft {
        val position = requireNotNull(playbackPosition(recordingId, snapshot, nowMs)) { "Play this song in your selected music app to stamp a line." }
        return setTime(position).copy(selectedIndex = (selectedIndex + 1).coerceAtMost(times.lastIndex))
    }
    fun problem(lineCount: Int): String? = try {
        require(times.size == lineCount) { "Line count changed. Restore the lines or create an untimed version before retiming." }
        LyricTimestamp.validate(resolvedTimes()); null
    } catch (e: IllegalArgumentException) { e.message }

    companion object {
        fun playbackPosition(recordingId: String?, snapshot: PlaybackSnapshot?, nowMs: Long): Long? =
            if (recordingId != null && snapshot?.recording?.id == recordingId &&
                snapshot.status in listOf(PlaybackStatus.PLAYING, PlaybackStatus.PAUSED))
                PlaybackClock.position(snapshot, nowMs)?.takeIf { it <= LyricTimestamp.MAX_MS } else null
    }
}
