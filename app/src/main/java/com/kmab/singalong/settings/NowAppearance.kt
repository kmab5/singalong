package com.kmab.singalong.settings

enum class LyricAlignment { START, CENTER, END }
enum class LyricAnimation { NONE, FADE, FOCUS, WORD_GLOW }

data class NowAppearance(
    val textSizeSp: Float = 22f,
    val alignment: LyricAlignment = LyricAlignment.START,
    val animation: LyricAnimation = LyricAnimation.FOCUS,
    val lineSpacing: Float = 1.45f,
    val dimInactive: Boolean = true,
    val tapToFocus: Boolean = true,
    val timedTextSizeSp: Float = textSizeSp,
    val wordTrackingEnabled: Boolean = true,
) {
    fun sanitized() = copy(textSizeSp = if (textSizeSp.isFinite()) textSizeSp.coerceIn(12f, 40f) else 22f,
        timedTextSizeSp = if (timedTextSizeSp.isFinite()) timedTextSizeSp.coerceIn(12f, 56f) else 22f,
        lineSpacing = if (lineSpacing.isFinite()) lineSpacing.coerceIn(1.1f, 2f) else 1.45f)
}
