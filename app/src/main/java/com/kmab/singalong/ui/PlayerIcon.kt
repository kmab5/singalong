package com.kmab.singalong.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            try { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }
            catch (_: PackageManager.NameNotFoundException) { null }
        }
    }
    val image = bitmap
    if (image != null) Image(image, null, modifier.size(36.dp))
    else Icon(Icons.Outlined.MusicNote, null, modifier.size(36.dp))
}
