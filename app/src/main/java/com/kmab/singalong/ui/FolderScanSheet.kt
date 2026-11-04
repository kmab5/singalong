package com.kmab.singalong.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.library.ScanPhase
import com.kmab.singalong.ui.theme.SingalongColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScanSheet(container: AppContainer, onDismiss: () -> Unit) {
    val scan by container.folderScan.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) container.scanFolder(uri)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SingalongColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Lyrics from a folder", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a music folder, including its subfolders. Singalong reads audio tags and filenames, looks up lyrics and saves the matches. Songs already in your lyric library are skipped.", color = SingalongColors.Muted)
            Text("Song details are sent to the lyric sources; audio is not uploaded. Wi-Fi-only settings apply. Keep Singalong open while scanning. If it closes, choose the folder again; saved lyrics stay saved.", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
            if (scan.phase != ScanPhase.IDLE) {
                Text(when (scan.phase) {
                    ScanPhase.SCANNING -> "Reading files and finding lyrics…"
                    ScanPhase.WAITING_NETWORK -> "Waiting for an allowed connection…"
                    ScanPhase.COOLDOWN -> "Waiting for the source · retry in ${scan.retrySeconds ?: 0}s"
                    ScanPhase.STOPPING -> "Stopping…"
                    ScanPhase.COMPLETE -> if (scan.checked == 0) "No supported audio files found" else "Folder scan complete"
                    ScanPhase.CANCELLED -> "Scan stopped · saved lyrics kept"
                    ScanPhase.FAILED -> "Scan interrupted · saved lyrics kept"
                    ScanPhase.IDLE -> "Ready"
                }, style = MaterialTheme.typography.titleMedium)
                if (scan.running) LinearProgressIndicator(Modifier.fillMaxWidth())
                scan.currentFile?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2) }
                Text("${scan.checked} checked · ${scan.saved} saved · ${scan.skipped} already saved", style = MaterialTheme.typography.bodyMedium)
                Text("${scan.notFound} without lyrics · ${scan.failed} couldn't be processed", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
                scan.message?.let { Text(it, color = SingalongColors.Muted) }
            }
            if (scan.running) OutlinedButton(onClick = container.folderScan::cancel, enabled = scan.phase != ScanPhase.STOPPING, modifier = Modifier.fillMaxWidth()) { Text("Cancel scan") }
            else Button(onClick = { picker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Choose music folder") }
            ActionButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(if (scan.running) "Continue using Singalong" else "Done") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
