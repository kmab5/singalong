package com.kmab.singalong.overlay

/** Keep every action reachable without reducing its touch target. */
object OverlayControlsLayout {
    const val actionCount = 5
    fun columns(widthDp: Float) = ((widthDp - 8f) / 48f).toInt().coerceIn(1, actionCount)
    fun rows(widthDp: Float) = (actionCount + columns(widthDp) - 1) / columns(widthDp)
    fun minimumHeight(widthDp: Float) = 48f + rows(widthDp) * 48f + 96f
}
