package com.kmab.singalong.lyrics

import kotlin.math.sin

/** Visual transforms only: never change lyric layout, content or stored timing. */
object GlowMotion {
    data class Transform(val xDp: Float, val yDp: Float, val scale: Float)
    fun transform(word: Int, seconds: Float, emphasis: Float): Transform {
        val amount = emphasis.coerceIn(0f, 1f)
        val phase = seconds * .8f + word * .65f
        return Transform(sin(phase) * 1.2f * amount, (-2.2f + sin(phase * .83f) * .9f) * amount,
            1f + .045f * amount)
    }
    fun reservedSize(normal: Float, current: Float) = maxOf(normal, current) * 1.06f
}
