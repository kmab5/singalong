package com.kmab.singalong.overlay

enum class PanelMode { HIDDEN, VISIBLE, MANUAL_MINIMIZED, AUTO_MINIMIZED }
data class OverlayState(val mode: PanelMode = PanelMode.HIDDEN, val expanded: Boolean = false, val pauseSince: Long? = null, val controlsVisible: Boolean = false,
    val missingSince: Long? = null, val missingSongId: String? = null) {
    val needsLyrics: Boolean get() = mode == PanelMode.VISIBLE || mode == PanelMode.AUTO_MINIMIZED
    fun show() = copy(mode = PanelMode.VISIBLE, pauseSince = null, controlsVisible = false, missingSince = null, missingSongId = null)
    fun close() = copy(mode = PanelMode.HIDDEN, pauseSince = null, missingSince = null, missingSongId = null)
    fun minimize() = copy(mode = PanelMode.MANUAL_MINIMIZED, pauseSince = null, missingSince = null, missingSongId = null)
    fun playback(playing: Boolean, now: Long): OverlayState {
        if (playing) return copy(mode = if (mode == PanelMode.AUTO_MINIMIZED) PanelMode.VISIBLE else mode, pauseSince = null)
        if (mode != PanelMode.VISIBLE) return this
        val since = pauseSince ?: now
        return copy(mode = if (now - since >= 15_000) PanelMode.AUTO_MINIMIZED else mode, pauseSince = since)
    }
    fun observe(songId: String?, playing: Boolean?, status: String, hasLyrics: Boolean, retrying: Boolean, now: Long): OverlayState {
        if (!needsLyrics) return this
        if (mode == PanelMode.AUTO_MINIMIZED && missingSongId != null) return if (hasLyrics) show() else this
        val failed = songId != null && !hasLyrics && !retrying && status in setOf("not_found", "network_error", "unavailable", "invalid_response", "rate_limited", "offline")
        val next = if (failed) {
            val since = missingSince?.takeIf { missingSongId == songId } ?: now
            copy(missingSince = since, missingSongId = songId,
                mode = if (now - since >= 8_000) PanelMode.AUTO_MINIMIZED else mode)
        } else copy(missingSince = null, missingSongId = null)
        return if (next.mode == PanelMode.AUTO_MINIMIZED && next.missingSongId != null) next
            else playing?.let { next.playback(it, now) } ?: next.copy(pauseSince = null)
    }
    fun nextCheckAt(): Long? = if (mode != PanelMode.VISIBLE) null else listOfNotNull(pauseSince?.plus(15_000), missingSince?.plus(8_000)).minOrNull()
}
