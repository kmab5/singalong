package com.kmab.singalong.tracking

import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.model.PlaybackStatus
import java.util.UUID
import com.kmab.singalong.lyrics.PlaybackClock

data class Qualification(val recordingId: String, val occurrenceId: String)

/** Uses observed elapsed playing time, never media position or wall-clock time. */
class CollectionQualifier(private val newId: () -> String = { UUID.randomUUID().toString() }) {
    private var previous: PlaybackSnapshot? = null
    private var observedAt = 0L
    private var listenedMs = 0L
    private var occurrenceId = ""
    private var qualified = false

    fun observe(snapshot: PlaybackSnapshot?, nowMs: Long): Qualification? {
        if (snapshot?.recording == null) { reset(); return null }
        val old = previous
        val changed = old?.sessionId != snapshot.sessionId || old?.recording?.id != snapshot.recording.id
        val duration = snapshot.recording.durationMs
        val previousPosition = PlaybackClock.position(old, nowMs)
        val repeat = !changed && snapshot.observedAtMs != old?.observedAtMs && duration != null && previousPosition != null && snapshot.positionMs != null &&
            previousPosition >= duration - 1_500 && snapshot.positionMs <= 1_500 && previousPosition - snapshot.positionMs > 5_000
        if (changed || repeat) {
            listenedMs = 0; qualified = false; occurrenceId = newId()
        } else if (old?.status == PlaybackStatus.PLAYING && old.speed > 0f) {
            val intervalEnd = if (snapshot.status != old.status && snapshot.observedAtMs in observedAt..nowMs) snapshot.observedAtMs else nowMs
            listenedMs += (intervalEnd - observedAt).coerceAtLeast(0)
        }
        previous = snapshot
        observedAt = nowMs
        if (!qualified && listenedMs >= 10_000) {
            qualified = true
            return Qualification(snapshot.recording.id, occurrenceId)
        }
        return null
    }

    fun reset() { previous = null; listenedMs = 0; qualified = false; occurrenceId = ""; observedAt = 0 }
}
