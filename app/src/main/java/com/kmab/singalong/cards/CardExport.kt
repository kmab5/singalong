package com.kmab.singalong.cards

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

object CardExport {
    suspend fun prepare(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "lyric_cards")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create card storage")
        // Only our own generated cache files are eligible. Never touch picked images or saved cards.
        val previous = directory.listFiles().orEmpty().filter { it.isFile && it.name.startsWith("card-") && it.extension == "png" }
            .sortedByDescending { it.lastModified() }
        previous.forEachIndexed { index, file ->
            if (index >= 7 || System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) file.delete()
        }
        val file = File(directory, "card-${UUID.randomUUID()}.png")
        try {
            file.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Card could not be encoded") }
            FileProvider.getUriForFile(context, "${context.packageName}.cards", file)
        } catch (failure: IOException) { file.delete(); throw failure }
    }

    fun share(context: Context, uri: Uri): Boolean = try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "Singalong lyric card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share lyric card"))
        true
    } catch (_: ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }

    suspend fun save(context: Context, source: Uri, target: Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(source) ?: throw IOException("Card is no longer available")
        input.use { from ->
            val output = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Destination is unavailable")
            output.use { to -> from.copyTo(to) }
        }
        Unit
    }
}
