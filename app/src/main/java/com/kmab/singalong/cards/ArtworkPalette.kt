package com.kmab.singalong.cards

import kotlin.math.pow
import kotlin.math.roundToInt

data class CardPaletteColors(val base: Int, val ink: Int, val accent: Int)

/** Population-based color extraction from a small sample. No image or Android dependencies. */
object ArtworkPalette {
    fun fromPixels(pixels: IntArray): CardPaletteColors? {
        val counts = IntArray(4096)
        val red = LongArray(4096)
        val green = LongArray(4096)
        val blue = LongArray(4096)
        for (pixel in pixels) {
            if ((pixel ushr 24) < 128) continue
            val r = pixel ushr 16 and 255
            val g = pixel ushr 8 and 255
            val b = pixel and 255
            val bucket = (r shr 4 shl 8) or (g shr 4 shl 4) or (b shr 4)
            counts[bucket]++
            red[bucket] += r.toLong(); green[bucket] += g.toLong(); blue[bucket] += b.toLong()
        }
        val ranked = counts.indices.filter { counts[it] > 0 }.sortedWith(compareByDescending<Int> { counts[it] }.thenBy { it })
        if (ranked.isEmpty()) return null
        fun color(index: Int) = rgb((red[index] / counts[index]).toInt(), (green[index] / counts[index]).toInt(), (blue[index] / counts[index]).toInt())
        val dominant = color(ranked.first())
        val population = counts.sum()
        val secondary = ranked.firstOrNull { counts[it] >= population * .05 && distance(color(it), dominant) > 60 }
            ?.let(::color) ?: dominant
        val base = mix(dominant, 0xff000000.toInt(), .78)
        val ink = mix(dominant, 0xffffffff.toInt(), .94)
        var accent = secondary
        // Accents appear in glows and backgrounds; keep the swatch distinct against the base too.
        while (contrast(accent, base) < 4.5) accent = mix(accent, 0xffffffff.toInt(), .15)
        return CardPaletteColors(base, ink, accent)
    }

    private fun distance(a: Int, b: Int) = listOf(16, 8, 0).sumOf { kotlin.math.abs((a ushr it and 255) - (b ushr it and 255)) }
    private fun rgb(r: Int, g: Int, b: Int) = (255 shl 24) or (r shl 16) or (g shl 8) or b
    private fun mix(a: Int, b: Int, fraction: Double): Int {
        fun channel(shift: Int) = ((a ushr shift and 255) * (1 - fraction) + (b ushr shift and 255) * fraction).roundToInt()
        return rgb(channel(16), channel(8), channel(0))
    }
    internal fun contrast(a: Int, b: Int): Double {
        fun luminance(color: Int): Double {
            fun channel(shift: Int): Double {
                val c = (color ushr shift and 255) / 255.0
                return if (c <= .04045) c / 12.92 else ((c + .055) / 1.055).pow(2.4)
            }
            return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
        }
        val x = luminance(a); val y = luminance(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }
}
