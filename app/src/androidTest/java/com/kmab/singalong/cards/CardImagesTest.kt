package com.kmab.singalong.cards

import android.graphics.Bitmap
import android.graphics.Color
import com.kmab.singalong.model.*
import org.junit.Assert.*
import org.junit.Test

class CardImagesTest {
    @Test fun capturedArtworkRemainsIndependentWhenPlayerReusesItsBitmap() {
        val player = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val content = CardContent(Recording("r", "Original song", listOf("Artist"), null, null),
            listOf(LyricLine("l", null, "Original line")), null, 0)
        val request = CardRequest(content, CardImages.bounded(player))
        player.eraseColor(Color.BLUE)
        player.recycle()
        assertEquals(Color.RED, request.cover!!.getPixel(10, 10))
        assertEquals("Original song", request.content.recording.title)
        request.cover.recycle()
    }
}
