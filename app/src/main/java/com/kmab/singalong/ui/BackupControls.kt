package com.kmab.singalong.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.backup.*
import com.kmab.singalong.settings.SettingsBackup
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.IOException

@Composable
fun BackupControls(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<BackupDocument?>(null) }
    var clear by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    var lyrics by rememberSaveable { mutableStateOf(LyricsBackupMode.FULL) }
    var songs by rememberSaveable { mutableStateOf(true) }
    var history by rememberSaveable { mutableStateOf(true) }
    var settings by rememberSaveable { mutableStateOf(true) }
    var restoreSettings by remember { mutableStateOf(false) }
    val options = BackupOptions(lyrics, songs, history, settings)

    fun action(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            try { block() }
            catch (e: IllegalArgumentException) { message = e.message ?: "Invalid backup." }
            catch (_: IOException) { message = "Could not read or write the backup. Choose the file again and check free space." }
            catch (_: SecurityException) { message = "File access was removed. Choose the file again." }
            catch (_: android.database.sqlite.SQLiteException) { message = "Local storage is unavailable. Free some space and try again." }
            finally { busy = false }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) action {
            val preferences = if (options.settings) SettingsBackup.export(container.settings.state.first { it.ready }) else emptyMap()
            withContext(Dispatchers.IO) {
                val document = container.library.exportBackup(options).copy(preferences = preferences)
                requireNotNull(resolver.openOutputStream(uri, "wt")) { "Could not open the backup destination." }.use {
                    BackupArchive.write(document, it, createdAtEpochMs = System.currentTimeMillis(),
                        appVersion = com.kmab.singalong.BuildConfig.VERSION_NAME)
                }
            }
            message = "ZIP backup saved. Keep a second copy somewhere safe."
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) action {
            pending = withContext(Dispatchers.IO) {
                requireNotNull(resolver.openInputStream(uri)) { "Could not open the backup." }.use(BackupArchive::read)
            }
            restoreSettings = false
        }
    }
    Text("Library backup", style = MaterialTheme.typography.titleLarge)
    Text("Choose what to keep in a ZIP: lyrics, song details, history and reading settings. Audio files are never copied.", color = SingalongColors.Muted)
    message?.let { Text(it) }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(enabled = !busy, onClick = { choosing = true }) { Text("Export backup") }
        ActionButton(enabled = !busy, onClick = { restore.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/json", "text/*", "application/octet-stream")) }) { Text("Restore backup") }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(enabled = !busy, onClick = { clear = "history" }) { Text("Clear history") }
        ActionButton(enabled = !busy, onClick = { clear = "downloads" }) { Text("Clear downloads") }
    }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Choose backup contents") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lyrics", style = MaterialTheme.typography.titleSmall)
            LyricsBackupMode.entries.forEach { mode ->
                FilterChip(selected = lyrics == mode, onClick = { lyrics = mode }, modifier = Modifier.fillMaxWidth(), label = {
                    Text(when (mode) { LyricsBackupMode.FULL -> "Full lyrics"; LyricsBackupMode.LINKS_METADATA -> "Links & metadata only"; LyricsBackupMode.NONE -> "No lyrics" })
                })
            }
            Text(when (lyrics) {
                LyricsBackupMode.FULL -> "Includes original lines, personal edits, timing and saved translations."
                LyricsBackupMode.LINKS_METADATA -> "Keeps source references only. Text, personal edits, translations and timing are omitted. A source may not be available later."
                LyricsBackupMode.NONE -> "No lyric text or source references."
            }, style = MaterialTheme.typography.bodySmall)
            BackupChoice("Song metadata", "Album, duration, filenames and favorites. No audio files.", songs) { songs = it }
            BackupChoice("Play history", "Play counts and last-played dates.", history) { history = it }
            BackupChoice("Reading settings", "Appearance, library sorting and Wi-Fi preference. Access and consent stay on this phone.", settings) { settings = it }
            Text("Song titles and artists accompany lyrics and history so they can be matched when restored.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { ActionButton(enabled = options.hasContent && !busy, onClick = { choosing = false; export.launch("singalong-backup.zip") }) { Text("Save ZIP") } },
        dismissButton = { ActionButton(onClick = { choosing = false }) { Text("Cancel") } })

    pending?.let { document -> AlertDialog(onDismissRequest = { if (!busy) pending = null }, title = { Text("Restore backup?") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${document.songs.size} songs · ${document.songs.sumOf { it.versions.size }} lyric versions · ${document.songs.sumOf { it.lyricReferences.size }} source references")
            Text("Merge included data. Existing selections and personal edits stay intact. Missing sections leave your library unchanged. Source references contain no offline lyrics.")
            if (document.options.history) Text("Includes play counts and last-played dates.", style = MaterialTheme.typography.bodySmall)
            if (document.options.settings && document.preferences.isNotEmpty()) BackupChoice("Restore reading settings", "Replace appearance, sorting and Wi-Fi preferences. Tracking, access and translation consent stay unchanged.", restoreSettings) { restoreSettings = it }
        }
    }, confirmButton = { ActionButton(enabled = !busy, onClick = { action {
        val added = withContext(Dispatchers.IO) { container.library.restore(document) }
        container.reader.refreshLocal()
        pending = null
        message = "Restored $added new library records."
        if (restoreSettings) {
            try { container.settings.restorePreferences(document.preferences); message = "Restored $added new library records and reading settings." }
            catch (_: IOException) { message = "Library restored, but settings could not be written. Try restoring again." }
        }
    } }) { Text("Restore") } }, dismissButton = { ActionButton(enabled = !busy, onClick = { pending = null }) { Text("Cancel") } }) }

    clear?.let { kind -> AlertDialog(onDismissRequest = { clear = null }, title = { Text("Clear $kind?") },
        text = { Text(if (kind == "history") "Lyrics and favorites will be retained." else "Favorites, imports and personal edits will be retained. Other downloaded versions will be removed.") },
        confirmButton = { ActionButton(enabled = !busy, onClick = { clear = null; action {
            withContext(Dispatchers.IO) { if (kind == "history") container.library.clearHistory() else container.library.clearDownloads() }
            container.reader.refreshLocal(); message = "$kind cleared."
        } }) { Text("Clear") } }, dismissButton = { ActionButton(onClick = { clear = null }) { Text("Cancel") } }) }
}

@Composable
private fun BackupChoice(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
        }
        Checkbox(checked, onCheckedChange = null)
    }
}
