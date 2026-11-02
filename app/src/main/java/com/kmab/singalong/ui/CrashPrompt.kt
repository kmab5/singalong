package com.kmab.singalong.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kmab.singalong.BuildConfig
import com.kmab.singalong.app.CrashDiagnostics
import com.kmab.singalong.app.FeedbackLink
import kotlinx.coroutines.*
import java.io.IOException

@Composable
fun CrashPrompt(openLink: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { report = withContext(Dispatchers.IO) { CrashDiagnostics.pending(context) } }
        catch (_: IOException) { /* The About page can retry reading the local report. */ }
    }
    fun dismiss() { report?.let { CrashDiagnostics.reviewed(context, it) }; report = null }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val captured = report
        if (uri != null && captured != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No stream")
                    stream.bufferedWriter().use { it.write(captured) }
                }
                dismiss()
                openLink(FeedbackLink.url(BuildConfig.VERSION_NAME, "bug"))
            } catch (_: IOException) { error = "Could not save the log. Choose another location." }
            catch (_: SecurityException) { error = "File access changed. Choose the location again." }
        }
    }
    report?.let { captured -> AlertDialog(onDismissRequest = ::dismiss,
        title = { Text("Singalong closed unexpectedly") },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A diagnostic log was saved on this phone. It contains the crash time, app version and code locations, without song details or lyrics.")
            Text("Save the log, then attach it on the feedback page. You review and submit the report there; nothing is sent automatically.")
            ActionButton(onClick = { showDetails = !showDetails }) { Text(if (showDetails) "hide log" else "review log") }
            if (showDetails) Text(captured, style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { ActionButton(onClick = { export.launch("singalong-crash.txt") }) { Text("save log & open feedback") } },
        dismissButton = { ActionButton(onClick = ::dismiss) { Text("not now") } }) }
}
