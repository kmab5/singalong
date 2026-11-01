package com.kmab.singalong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.R
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.*
import com.kmab.singalong.tracking.PlayerApp
import com.kmab.singalong.ui.theme.SingalongColors
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

data class PermissionStatus(val music: Boolean = false, val overlay: Boolean = false, val notifications: Boolean = false)
data class FoundationActions(val musicAccess: () -> Unit, val overlayAccess: () -> Unit, val notificationAccess: () -> Unit)

@Composable
fun FoundationScreen(container: AppContainer, permissions: PermissionStatus, actions: FoundationActions) {
    val scope = rememberCoroutineScope()
    val state by container.state.collectAsStateWithLifecycle()
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val reader by container.reader.state.collectAsStateWithLifecycle()
    val sessions by container.observer.sessions.collectAsStateWithLifecycle()
    val installed by container.players.collectAsStateWithLifecycle()
    val entries by container.entries.collectAsStateWithLifecycle()
    val override by container.tracking.overrideSession.collectAsStateWithLifecycle()
    var tools by rememberSaveable { mutableStateOf(false) }
    var toolsId by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf("now") }
    var savedId by rememberSaveable { mutableStateOf<String?>(null) }
    var savedVersion by remember { mutableStateOf<LyricVersion?>(null) }
    var savedTitle by remember { mutableStateOf("") }
    var savedTranslation by remember { mutableStateOf<com.kmab.singalong.translation.TranslationRecord?>(null) }
    var savedStatus by remember { mutableStateOf("loading") }
    LaunchedEffect(savedId, settings.translationEnabled, tools) {
        savedVersion = null; savedTranslation = null; savedStatus = "loading"; savedTitle = ""
        savedId?.let { id ->
            savedTitle = container.library.recording(id)?.title.orEmpty()
            savedVersion = container.library.selectedVersion(id)
            savedStatus = container.library.storedStatus(id) ?: "not_found"
            savedVersion?.let { savedTranslation = (container.translations.resolve(it, settings.translationEnabled, true, false) as? com.kmab.singalong.translation.TranslationResult.Available)?.record }
        }
    }
    DisposableEffect(destination, savedId) {
        container.readerVisible(destination == "now" && savedId == null)
        onDispose { container.readerVisible(false) }
    }
    if (tools) {
        LibraryTools(container, toolsId) { tools = false; container.reader.request() }
        return
    }
    BackHandler(savedId != null || destination == "settings") { if (savedId != null) savedId = null else destination = "now" }
    Scaffold(containerColor = SingalongColors.Background, bottomBar = {
        NavigationBar {
            listOf("now", "library", "history", "settings").forEach { name ->
                NavigationBarItem(selected = destination == name && savedId == null,
                    onClick = { destination = name; savedId = null }, icon = { Icon(when(name) { "now" -> Icons.Outlined.GraphicEq; "library" -> Icons.Outlined.LibraryMusic; "history" -> Icons.Outlined.History; else -> Icons.Outlined.Tune }, null) }, label = { Text(name) })
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_kmab), "KMAB", Modifier.size(28.dp), tint = SingalongColors.Purple)
                Text("singalong", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleLarge)
            }
            state.message?.let { Text(it, color = SingalongColors.PurpleText, style = MaterialTheme.typography.bodyMedium) }
            if (savedId != null) {
                TextButton(onClick = { savedId = null }) { Text("back to $destination") }
                TextButton(onClick = { toolsId = savedId; tools = true }) { Text("versions, import & edit") }
                Text(savedTitle, style = MaterialTheme.typography.headlineSmall)
                Text("Saved copy · live floating lyrics continue separately", style = MaterialTheme.typography.labelSmall)
                LyricsReader(ReaderState(status = savedStatus, version = savedVersion, translation = if (settings.translationEnabled) savedTranslation else null), Modifier.weight(1f), textSize = settings.textSizeSp,
                    onRetry = {
                        val id = savedId
                        if (id != null) scope.launch {
                            savedStatus = "loading"
                            val result = container.lookupSaved(id)
                            if (savedId == id) { savedStatus = result?.status ?: "not_found"; savedVersion = result?.version }
                        }
                    })
            } else when (destination) {
                "now" -> {
                    Text(reader.snapshot?.recording?.title ?: "keep the words close.", style = MaterialTheme.typography.headlineMedium)
                    Text(reader.snapshot?.recording?.artists?.joinToString().orEmpty(), color = SingalongColors.Muted)
                    Button(onClick = if (permissions.overlay) container::showLyrics else actions.overlayAccess, modifier = Modifier.fillMaxWidth()) { Text("open floating lyrics") }
                    if (!state.tracking) OutlinedButton(onClick = container::enableTracking, enabled = settings.ready) { Text("start tracking") }
                    if (sessions.size > 1) {
                        var choose by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { choose = true }) { Text(reader.snapshot?.playerName ?: "choose player") }
                            DropdownMenu(choose, { choose = false }) {
                                DropdownMenuItem(text = { Text(if (override == null) "Automatic ✓" else "Automatic") }, onClick = { container.chooseSession(null); choose = false })
                                sessions.filter { it.packageName in settings.allowedPlayers }.forEach { session ->
                                    DropdownMenuItem(text = { Text(session.playerName) }, onClick = { container.chooseSession(session.sessionId); choose = false })
                                }
                            }
                        }
                    }
                    LyricsReader(reader.copy(translation = if (settings.translationEnabled) reader.translation else null), Modifier.weight(1f), textSize = settings.textSizeSp, translationSize = settings.translationSizeSp, onFollow = container.reader::follow, onRetry = container.reader::request)
                    ReaderControls(reader, container.reader::adjust)
                    if (reader.candidates.size > 1) {
                        var choose by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { choose = true }) { Text("choose saved version (${reader.candidates.size})") }
                            DropdownMenu(choose, { choose = false }) { reader.candidates.forEach { version ->
                                DropdownMenuItem(text = { Text(version.attribution ?: version.sourceName.orEmpty()) }, onClick = { container.reader.select(version); choose = false })
                            } }
                        }
                    }
                    reader.version?.sourceName?.let { Text("Lyrics: $it", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted) }
                }
                "library", "history" -> {
                    Text(if (destination == "library") "your words, saved." else "songs you come back to.", style = MaterialTheme.typography.headlineMedium)
                    Row {
                        TextButton(onClick = { toolsId = null; tools = true }) { Text("add a song") }
                        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("favorites") })
                    }
                    OutlinedTextField(search, { search = it }, label = { Text("Search title or artist") }, modifier = Modifier.fillMaxWidth())
                    val rows = entries.filter { (if (destination == "library") it.hasLyrics else it.playCount > 0) &&
                        (!favoritesOnly || it.favorite) && (it.recording.title + " " + it.recording.artists.joinToString()).contains(search, true) }
                    if (rows.isEmpty()) Text("Play a song for ten seconds to start your collection.", color = SingalongColors.Muted)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(rows, key = { it.recording.id }) { entry ->
                            Surface(color = SingalongColors.Surface, modifier = Modifier.fillMaxWidth().clickable { savedId = entry.recording.id }) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { scope.launch { container.library.favorite(entry.recording.id, !entry.favorite) } }) { Text(if (entry.favorite) "★ favorited" else "favorite") }
                                    if (entry.hasLyrics) Text("saved offline", style = MaterialTheme.typography.labelSmall)
                                    Text(entry.recording.title, style = MaterialTheme.typography.titleMedium)
                                    Text(entry.recording.artists.joinToString(), color = SingalongColors.Muted)
                                    if (destination == "history") Text("${entry.playCount} plays · ${entry.lastPlayedAtMs?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) }.orEmpty()}", style = MaterialTheme.typography.labelSmall)
                                    if (!entry.hasLyrics) Text(lookupMessage(entry.lookupStatus), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                "settings" -> {
                    var query by rememberSaveable { mutableStateOf("") }
                    val apps = (installed + sessions.map { PlayerApp(it.packageName, it.playerName) } + settings.allowedPlayers.map { PlayerApp(it, it) })
                        .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("a little setup.", style = MaterialTheme.typography.headlineMedium) }
                        item { Text("Music access reads media details without recording audio. Select the apps you want to follow.") }
                        item { PermissionButton("music access", permissions.music, actions.musicAccess) }
                        item { PermissionButton("display over other apps", permissions.overlay, actions.overlayAccess) }
                        item { PermissionButton("notifications", permissions.notifications, actions.notificationAccess) }
                        item { Button(onClick = if (state.tracking) container::disableTracking else container::enableTracking, enabled = settings.ready) { Text(if (state.tracking) "stop tracking" else "start tracking") } }
                        item { Text("Open Singalong after a restart to resume tracking. Closing lyrics leaves collection running.", color = SingalongColors.Muted) }
                        item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(settings.wifiOnly, container::wifiOnly); Text("Download only on Wi-Fi", Modifier.padding(start = 12.dp)) } }
                        item { Text("Lyric size"); Slider(settings.textSizeSp, container::textSize, valueRange = 16f..36f) }
                        item { Text("Panel opacity"); Slider(settings.opacity, container::opacity, valueRange = 0.85f..1f) }
                        item { BackupControls(container) }
                        item { Text("English translations", style = MaterialTheme.typography.titleLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) { Switch(settings.translationEnabled, container::translation); Text("Show English subtext", Modifier.padding(start = 12.dp)) }
                            Text("Translation text size"); Slider(settings.translationSizeSp, container::translationSize, valueRange = 14f..24f)
                            Text("Unavailable · no free online provider has been approved for this build. Original lyrics always remain available. A future opt-in service would send opened lyrics online and save aligned translations with their version.", style = MaterialTheme.typography.bodyMedium)
                        }
                        item { Text("music apps", style = MaterialTheme.typography.titleLarge); OutlinedTextField(query, { query = it }, label = { Text("Search installed apps") }, modifier = Modifier.fillMaxWidth()) }
                        items(apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }, key = { it.packageName }) { app ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(app.packageName in settings.allowedPlayers, { container.allowPlayer(app.packageName, it) })
                                Column { Text(app.label); Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun PermissionButton(title: String, allowed: Boolean, action: () -> Unit) {
    OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text("$title · ${if (allowed) "allowed" else "set up"}") }
}
