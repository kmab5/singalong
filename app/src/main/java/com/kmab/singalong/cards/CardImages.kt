package com.kmab.singalong.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

object CardImages {
    /** Local, user-granted or player-provided images only. No hidden network artwork requests. */
    suspend fun load(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        if (uri.scheme !in setOf("content", "file", "android.resource")) return@withContext null
        try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize((info.size.width * scale).roundToInt().coerceAtLeast(1), (info.size.height * scale).roundToInt().coerceAtLeast(1))
            }
        } catch (_: IOException) { null }
          catch (_: SecurityException) { null }
          catch (_: IllegalArgumentException) { null }
    }

    /** Makes an independent bounded copy; never recycles a bitmap owned by the music player. */
    fun bounded(bitmap: Bitmap): Bitmap {
        val scale = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1), true) else bitmap
        if (scaled !== bitmap && scaled.config != Bitmap.Config.HARDWARE) return scaled
        return requireNotNull(scaled.copy(Bitmap.Config.ARGB_8888, false))
    }
}
