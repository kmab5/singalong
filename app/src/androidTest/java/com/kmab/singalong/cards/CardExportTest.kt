package com.kmab.singalong.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CardExportTest {
    @Test fun exportIsAReadablePngAndProviderCannotExposeOtherCacheData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        try {
            val uri = CardExport.prepare(context, bitmap)
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.cards", uri.authority)
            val decoded = context.contentResolver.openInputStream(uri)!!.use(BitmapFactory::decodeStream)
            assertEquals(80, decoded.width)
            assertEquals(120, decoded.height)
            decoded.recycle()
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.cards", File(context.cacheDir, "unrelated-private-file"))
                fail("Provider must not expose the cache root")
            } catch (_: IllegalArgumentException) { }
        } finally { bitmap.recycle() }
    }
}
