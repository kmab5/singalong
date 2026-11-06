package com.kmab.singalong.cards

/** Only rows intersecting a corner mark lose width; the rest retain the full verse width. */
internal object CardTextExclusion {
    fun insets(left: Float, right: Float, top: Float, bottom: Float,
        markLeft: Float, markTop: Float, markRight: Float, markBottom: Float): Pair<Float, Float> {
        if (bottom <= markTop || top >= markBottom || right <= markLeft || left >= markRight) return 0f to 0f
        return if (markLeft + markRight < left + right) (markRight - left).coerceAtLeast(0f) to 0f
        else 0f to (right - markLeft).coerceAtLeast(0f)
    }
}
