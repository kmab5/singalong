package com.kmab.singalong.cards

import android.graphics.Bitmap

/** Sample at most 4,096 pixels of our software-decoded draft image; never mutate its pixels. */
object CardArtworkColors {
    fun extract(artwork: Bitmap?): CardPaletteColors? {
        if (artwork == null || artwork.isRecycled) return null
        val width = minOf(64, artwork.width)
        val height = minOf(64, artwork.height)
        val pixels = IntArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            pixels[y * width + x] = artwork.getPixel(
                ((x + .5) * artwork.width / width).toInt().coerceAtMost(artwork.width - 1),
                ((y + .5) * artwork.height / height).toInt().coerceAtMost(artwork.height - 1))
        }
        return ArtworkPalette.fromPixels(pixels)
    }
}
