package com.kmab.singalong.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.backup.*
import com.kmab.singalong.editor.LyricsEditor
import com.kmab.singalong.model.*
import kotlinx.coroutines.*
import java.io.IOException
import com.kmab.singalong.ui.theme.SingalongColors

class EditorDraft : androidx.lifecycle.ViewModel() {
    val text = mutableStateOf("")
    val source = mutableStateOf<LyricVersion?>(null)
    val dirty = mutableStateOf(false)
    fun clear() { text.value = ""; source.value = null; dirty.value = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTools(container: AppContainer, recordingId: String?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    val sessionKey = rememberSaveable { java.util.UUID.randomUUID().toString() }
    val activity = requireNotNull(LocalActivity.current as? androidx.activity.ComponentActivity)
    val draft = androidx.lifecycle.ViewModelProvider(activity)[sessionKey, EditorDraft::class.java]
    fun close() { draft.clear(); onClose() }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var text by draft.text
    var source by draft.source
    var versions by remember { mutableStateOf<List<LyricVersion>>(emptyList()) }
    var id by rememberSaveable { mutableStateOf(recordingId) }
    var timed by rememberSaveable { mutableStateOf(false) }
    var dirty by draft.dirty
    var discard by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var metadataOpen by rememberSaveable { mutableStateOf(recordingId == null) }
    var chooseVersion by remember { mutableStateOf(false) }
    fun action(block: suspend () -> Unit) { scope.launch {
        busy = true
        try { block() } catch (e: IllegalArgumentException) { message = e.message ?: "Invalid content." }
        catch (_: IOException) { message = "Could not read or write this file." }
        catch (_: SecurityException) { message = "File access was removed. Choose the document again." }
        finally { busy = false }
    } }
    fun load(version: LyricVersion?) { source = version; text = version?.lines?.joinToString("\n") { it.text }.orEmpty(); timed = version?.lines?.any { it.startMs != null } == true; dirty = false }
    suspend fun recording(): Recording {
        id?.let { return requireNotNull(container.library.recording(it)) }
        require(title.isNotBlank() && artist.isNotBlank()) { "Enter the song title and artist first." }
        return container.library.resolveRecording(Recording("new", title.trim(), listOf(artist.trim()), album.ifBlank { null }, null)).also { id = it.id }
    }
    LaunchedEffect(recordingId) { if (!dirty) recordingId?.let {
        container.library.recording(it)?.let { r -> title = r.title; artist = r.artists.joinToString(); album = r.album.orEmpty() }
        versions = container.library.versions(it); load(container.library.selectedVersion(it))
    } }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) action {
        val content = withContext(Dispatchers.IO) { requireNotNull(resolver.openInputStream(uri)).use { BackupCodec.read(it, LyricsEditor.MAX_BYTES) } }
        val preview = LyricsEditor.import(id ?: "preview", content)
        source = preview; text = preview.lines.joinToString("\n") { it.text }; timed = preview.lines.any { it.startMs != null }; dirty = true
        message = "Import preview · review the lyrics, then save to this song."
    } }
    BackHandler { if (dirty) discard = true else close() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard unsaved lyrics?") },
        confirmButton = { TextButton(onClick = ::close) { Text("discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("keep editing") } })
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = SingalongColors.Text, topBar = {
        TopAppBar(title = { Text(if (id == null) "Add lyrics" else "Your version") },
            navigationIcon = { IconButton(onClick = { if (dirty) discard = true else close() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SingalongColors.Background.copy(alpha = .9f)))
    }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassPanel { Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(title.ifBlank { "Song details" }, style = MaterialTheme.typography.titleMedium); Text(artist.ifBlank { "Add a title and artist to get started." }, color = SingalongColors.Muted, style = MaterialTheme.typography.bodyMedium) }
                IconButton(onClick = { metadataOpen = !metadataOpen }) { Icon(if (metadataOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, "Show song and search details") }
            }
            AnimatedVisibility(metadataOpen) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(if (id == null) "Song title" else "Search title") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(artist, { artist = it }, label = { Text(if (id == null) "Artist" else "Search artist") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(album, { album = it }, label = { Text("Album (optional)") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } }
        } }
        AnimatedVisibility(message != null) { GlassPanel { Text(message.orEmpty(), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) } }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = !busy && !dirty, onClick = { action { val r = recording(); val result = container.manualLookup(r.id, title, artist, album); versions = container.library.versions(r.id); message = if (result == "available") "Choose a matching version below. Timing is manual until you try sync." else lookupMessage(result) } }) { Text("find lyrics") }
            TextButton(enabled = !busy && !dirty, onClick = { importFile.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("import TXT / LRC") }
        }
        if (versions.isNotEmpty()) Box {
            OutlinedButton(enabled = !dirty && !busy, onClick = { chooseVersion = true }, modifier = Modifier.fillMaxWidth()) {
                Text(source?.sourceName ?: "Choose a saved version", Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null)
            }
            DropdownMenu(chooseVersion, { chooseVersion = false }) {
        versions.forEach { version -> DropdownMenuItem(enabled = !dirty && !busy, onClick = { chooseVersion = false; action {
            container.library.selectVersion(version.recordingId, version.id); load(version)
            if (container.reader.state.value.snapshot?.recording?.id == version.recordingId) container.reader.select(version)
        } }, text = { Text("${if (source?.id == version.id) "✓ " else ""}${version.sourceName ?: "Saved version"} · ${version.attribution ?: version.id.take(8)}") }) }
            }
        }
        source?.sourceUrl?.let { Text("Source: $it", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted) }
        if (timed) {
            Text("Timed copy · edit words without adding or removing lines.")
            TextButton(onClick = { timed = false; dirty = true }) { Text("create an untimed version") }
        }
        OutlinedTextField(text, { text = it; dirty = true }, label = { Text(if (timed) "Lyrics · keep line count" else "Lyrics") }, modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 600.dp),
            shape = RoundedCornerShape(24.dp), textStyle = MaterialTheme.typography.bodyLarge, enabled = !busy,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = SingalongColors.Surface.copy(alpha = .8f), focusedContainerColor = SingalongColors.Surface.copy(alpha = .9f)))
        Button(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy && text.isNotBlank(), onClick = { action {
            val r = recording()
            val base = source?.copy(recordingId = r.id) ?: LyricsEditor.import(r.id, text)
            val edited = LyricsEditor.edit(base, text, timed)
            if (base.origin == LyricOrigin.IMPORT) container.library.savePersonal(base)
            container.library.savePersonal(edited); load(edited); versions = container.library.versions(r.id)
            if (container.reader.state.value.snapshot?.recording?.id == r.id) container.reader.select(edited)
            message = "Personal version saved. The original is unchanged."
        } }) { Text(if (busy) "saving…" else "save personal version") }
        Text("Personal edits are saved as a new version.", color = SingalongColors.Muted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(20.dp))
    }
    }
}

@Composable
fun BackupControls(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var message by remember { mutableStateOf<String?>(null) }
    var storageBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(message) { storageBytes = withContext(Dispatchers.IO) { container.library.exportBackup().songs.sumOf { song -> song.versions.sumOf { v -> v.lines.sumOf { it.text.toByteArray().size.toLong() } } } } }
    var pending by remember { mutableStateOf<BackupDocument?>(null) }
    var clear by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun action(block: suspend () -> Unit) { scope.launch {
        busy = true
        try { block() } catch (e: IllegalArgumentException) { message = e.message ?: "Invalid backup." }
        catch (_: IOException) { message = "Could not read or write the backup." }
        catch (_: SecurityException) { message = "File access was removed. Choose the file again." }
        finally { busy = false }
    } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) action {
        val prefs = container.settings.state.value
        val document = container.library.exportBackup().copy(preferences = mapOf(
            "wifiOnly" to prefs.wifiOnly.toString(), "translationEnabled" to prefs.translationEnabled.toString(),
            "textSize" to prefs.textSizeSp.toString(), "translationSize" to prefs.translationSizeSp.toString(),
            "opacity" to prefs.opacity.toString(), "width" to prefs.widthDp.toString(), "height" to prefs.heightDp.toString(),
            "x" to prefs.xFraction.toString(), "y" to prefs.yFraction.toString()))
        withContext(Dispatchers.IO) { val text = BackupCodec.encode(document); requireNotNull(resolver.openOutputStream(uri, "wt")).bufferedWriter().use { it.write(text) } }
        message = "Backup saved. Store a second copy somewhere safe."
    } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) action {
        pending = withContext(Dispatchers.IO) { requireNotNull(resolver.openInputStream(uri)).use { BackupCodec.decode(BackupCodec.read(it, BackupCodec.MAX_BYTES)) } }
    } }
    Text("library storage", style = MaterialTheme.typography.titleLarge)
    storageBytes?.let { Text("${it / 1024} KiB of original lyric text · excludes database overhead", style = MaterialTheme.typography.bodySmall) }
    message?.let { Text(it) }
    FlowRow { TextButton(enabled = !busy, onClick = { export.launch("singalong-backup.json") }) { Text("export backup") }
        TextButton(enabled = !busy, onClick = { restore.launch(arrayOf("application/json", "text/*")) }) { Text("restore backup") } }
    FlowRow { TextButton(enabled = !busy, onClick = { clear = "history" }) { Text("clear history") }
        TextButton(enabled = !busy, onClick = { clear = "downloads" }) { Text("clear downloads") } }
    pending?.let { document -> AlertDialog(onDismissRequest = { if (!busy) pending = null }, title = { Text("Restore ${document.songs.size} songs?") },
        text = { Text("Merge saved versions and history. Current settings and selected versions stay unchanged. Tracking will not be enabled.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { action { val added = container.library.restore(document); pending = null; message = "Restored $added new records. Existing selections and settings retained." } }) { Text("restore") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("cancel") } }) }
    clear?.let { kind -> AlertDialog(onDismissRequest = { clear = null }, title = { Text("Clear $kind?") },
        text = { Text(if (kind == "history") "Lyrics and favorites will be retained." else "Favorites, imports and personal edits will be retained. Other downloaded versions will be removed.") },
        confirmButton = { TextButton(onClick = { clear = null; action { if (kind == "history") container.library.clearHistory() else container.library.clearDownloads(); container.reader.refreshLocal(); message = "$kind cleared." } }) { Text("clear") } },
        dismissButton = { TextButton(onClick = { clear = null }) { Text("cancel") } }) }
}
