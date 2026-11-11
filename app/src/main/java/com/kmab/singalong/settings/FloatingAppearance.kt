package com.kmab.singalong.settings

/** Floating typography is independent of the full-screen reader. */
data class FloatingAppearance(
    val currentTextSizeSp: Float = 22f,
    val otherTextSizeSp: Float = 14f,
    val alignment: LyricAlignment = LyricAlignment.START,
    val wordTrackingEnabled: Boolean = true,
) {
    fun sanitized() = copy(
        currentTextSizeSp = if (currentTextSizeSp.isFinite()) currentTextSizeSp.coerceIn(12f, 32f) else 22f,
        otherTextSizeSp = if (otherTextSizeSp.isFinite()) otherTextSizeSp.coerceIn(12f, 32f) else 14f,
    )
}
