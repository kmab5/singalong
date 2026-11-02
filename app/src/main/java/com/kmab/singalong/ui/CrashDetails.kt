package com.kmab.singalong.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kmab.singalong.app.CrashDiagnostics
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.*
import java.io.IOException

@Composable fun CrashDetails() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { report = withContext(Dispatchers.IO) { CrashDiagnostics.read(context) } }
        catch (_: IOException) { message = "Crash details could not be read." }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val text = report
        if (uri != null && text != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
                    output.bufferedWriter().use { it.write(text) }
                }
                message = "Saved. Attach this file to your feedback if you want to share it."
            } catch (_: IOException) { message = "Could not save the file. Try another location." }
            catch (_: SecurityException) { message = "File access changed. Choose the location again." }
        }
    }
    GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Crash details", style = MaterialTheme.typography.titleMedium)
        Text(if (report == null) "No crash recorded by this version yet." else "The last crash is saved on this phone. The report includes code locations and the app/Android version, without lyrics, song details or exception messages.", color = SingalongColors.Muted)
        ActionButton(onClick = { export.launch("singalong-crash.txt") }, enabled = report != null) { Text("Save crash details") }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    } }
}
