package com.kmab.singalong.overlay

enum class PanelMode { HIDDEN, VISIBLE, MANUAL_MINIMIZED, AUTO_MINIMIZED }
data class OverlayState(val mode: PanelMode = PanelMode.HIDDEN, val expanded: Boolean = false, val pauseSince: Long? = null) {
    fun show() = copy(mode = PanelMode.VISIBLE, pauseSince = null)
    fun close() = copy(mode = PanelMode.HIDDEN, pauseSince = null)
    fun minimize() = copy(mode = PanelMode.MANUAL_MINIMIZED, pauseSince = null)
    fun playback(playing: Boolean, now: Long): OverlayState {
        if (playing) return copy(mode = if (mode == PanelMode.AUTO_MINIMIZED) PanelMode.VISIBLE else mode, pauseSince = null)
        if (mode != PanelMode.VISIBLE) return this
        val since = pauseSince ?: now
        return copy(mode = if (now - since >= 15_000) PanelMode.AUTO_MINIMIZED else mode, pauseSince = since)
    }
}
