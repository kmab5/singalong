package com.kmab.singalong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.R
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.*
import com.kmab.singalong.translation.*
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.launch

data class PermissionStatus(val music: Boolean = false, val overlay: Boolean = false, val notifications: Boolean = false)
data class FoundationActions(val musicAccess: () -> Unit, val overlayAccess: () -> Unit, val notificationAccess: () -> Unit,
    val trackingNotificationSettings: () -> Unit, val openLink: (String) -> Unit,
    val translateLyrics: (List<LyricLine>) -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
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
    var destination by rememberSaveable { mutableStateOf("now") }
    var savedId by rememberSaveable { mutableStateOf<String?>(null) }
    var savedVersion by remember { mutableStateOf<LyricVersion?>(null) }
    var savedRecording by remember { mutableStateOf<Recording?>(null) }
    var savedTranslation by remember { mutableStateOf<TranslationRecord?>(null) }
    var savedStatus by remember { mutableStateOf("loading") }
    var options by remember { mutableStateOf(false) }
    var controlTab by rememberSaveable { mutableStateOf(true) }
    var folderScanOpen by rememberSaveable { mutableStateOf(false) }
    val folderScan by container.folderScan.state.collectAsStateWithLifecycle()
    val openNow by container.openNowRequested.collectAsStateWithLifecycle()
    LaunchedEffect(openNow) { if (openNow) { destination = "now"; savedId = null; tools = false; container.openedNow() } }
    fun acceptsSavedTranslation(id: String, version: LyricVersion) = savedId == id && !tools && settings.translationEnabled &&
        savedVersion?.id == version.id && savedVersion?.contentHash == version.contentHash
    LaunchedEffect(savedId, settings.translationEnabled, tools) {
        savedVersion = null; savedTranslation = null; savedStatus = "loading"; savedRecording = null
        if (!tools) savedId?.let { id ->
            savedRecording = container.library.recording(id)
            savedVersion = container.library.selectedVersion(id)
            savedStatus = container.library.storedStatus(id) ?: "not_found"
            savedVersion?.let { version ->
                val result = container.translationForSaved(version) { partial -> if (acceptsSavedTranslation(id, version)) savedTranslation = partial }
                if (acceptsSavedTranslation(id, version)) savedTranslation = (result as? TranslationResult.Available)?.record ?: savedTranslation
            }
        }
    }
    DisposableEffect(destination, savedId, tools) {
        container.readerVisible(destination == "now" && savedId == null && !tools)
        onDispose { container.readerVisible(false) }
    }
    BackHandler(!tools && (savedId != null || destination != "now")) { if (savedId != null) savedId = null else destination = "now" }
    AmbientBackground {
        Scaffold(containerColor = Color.Transparent, contentColor = SingalongColors.Text, bottomBar = {
            if (!tools) Surface(color = SingalongColors.Surface.copy(alpha = .9f), contentColor = SingalongColors.Text) {
                NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                    listOf("now" to "Now", "library" to "Library", "history" to "History", "settings" to "Settings").forEach { (name, label) ->
                        NavigationBarItem(selected = destination == name, onClick = { destination = name; savedId = null },
                            icon = { Icon(when (name) { "now" -> Icons.Outlined.GraphicEq; "library" -> Icons.Outlined.LibraryMusic; "history" -> Icons.Outlined.History; else -> Icons.Outlined.Tune }, null) },
                            label = { Text(label) })
                    }
                }
            }
        }) { padding ->
            AnimatedContent(if (tools) "editor" else if (savedId != null) "saved" else destination,
                Modifier.padding(if (tools) PaddingValues(0.dp) else padding).fillMaxSize(),
                transitionSpec = { (fadeIn(SingalongMotion.tween()) + slideInHorizontally { it / 18 }) togetherWith fadeOut(SingalongMotion.tween()) },
                label = "App page") { page ->
                if (page == "editor") LibraryTools(container, toolsId) { tools = false; container.reader.request() }
                else Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_kmab), "kmab", Modifier.size(24.dp), tint = SingalongColors.Purple)
                        Text("singalong", Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(if (state.tracking) "Following music" else "Paused", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                    }
                    AnimatedVisibility(state.message != null) {
                        GlassPanel { Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(state.message.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { container.message(null) }) { Icon(Icons.Outlined.Close, "Dismiss message") }
                        } }
                    }
                    when (page) {
                        "now" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(reader.snapshot?.recording?.title ?: "Ready when you are.", style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(reader.snapshot?.recording?.artists?.joinToString() ?: "Your music. The words, right here.",
                                    style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = if (permissions.overlay) container::showLyrics else actions.overlayAccess, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Icon(Icons.Outlined.PictureInPictureAlt, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Floating lyrics")
                                }
                                FilledTonalIconButton(onClick = { options = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.MoreHoriz, "Lyric and player options") }
                            }
                            AnimatedVisibility(!state.tracking) {
                                OutlinedButton(onClick = {
                                    when { !permissions.music -> actions.musicAccess(); settings.allowedPlayers.isEmpty() -> destination = "settings"; else -> container.enableTracking() }
                                }, modifier = Modifier.fillMaxWidth()) { Text(if (settings.allowedPlayers.isEmpty() || !permissions.music) "Set up music access" else "Start following music") }
                            }
                            GlassPanel(Modifier.weight(1f).fillMaxWidth()) {
                                LyricsReader(reader.copy(translation = if (settings.translationEnabled) reader.translation else null), Modifier.fillMaxSize(),
                                    textSize = settings.textSizeSp, translationSize = settings.translationSizeSp, onFollow = container.reader::follow, onRetry = container.reader::request,
                                    appearance = settings.now)
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(reader.version?.sourceName?.let { "Lyrics · $it" } ?: "LRCLIB + lyrics.ovh", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                                Text(reader.snapshot?.playerName.orEmpty(), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted, maxLines = 1)
                            }
                        }
                        "saved" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { savedId = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to collection") }
                                Text("Saved lyrics", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { toolsId = savedId; tools = true }) { Icon(Icons.Outlined.Edit, "Versions, import and edit") }
                            }
                            Text(savedRecording?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                            Text(savedRecording?.artists?.joinToString().orEmpty(), color = SingalongColors.Muted)
                            GlassPanel(Modifier.weight(1f).fillMaxWidth()) {
                                LyricsReader(ReaderState(status = savedStatus, version = savedVersion, translation = if (settings.translationEnabled) savedTranslation else null),
                                    Modifier.fillMaxSize(), textSize = settings.textSizeSp, translationSize = settings.translationSizeSp, onRetry = {
                                        val id = savedId
                                        if (id != null) scope.launch {
                                            savedStatus = "loading"
                                            val expectedVersion = savedVersion?.id
                                            val result = container.lookupSaved(id)
                                            if (savedId == id && !tools && savedVersion?.id == expectedVersion) {
                                                savedStatus = result?.status ?: "not_found"; savedVersion = result?.version
                                                savedTranslation = null
                                                result?.version?.let { version ->
                                                    val translated = container.translationForSaved(version) { partial -> if (acceptsSavedTranslation(id, version)) savedTranslation = partial }
                                                    if (acceptsSavedTranslation(id, version)) savedTranslation = (translated as? TranslationResult.Available)?.record ?: savedTranslation
                                                }
                                            }
                                        }
                                    })
                            }
                            Text("Saved copy · floating lyrics follow your current song", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                            Spacer(Modifier.height(4.dp))
                        }
                        "library", "history" -> CollectionScreen(entries, page == "history",
                            onOpen = { savedId = it }, onAdd = { toolsId = null; tools = true },
                            onFavorite = { entry -> scope.launch { container.library.favorite(entry.recording.id, !entry.favorite) } },
                            sort = settings.librarySort, onSort = { scope.launch { container.settings.librarySort(it) } },
                            onScanFolder = { folderScanOpen = true }, scanning = folderScan.running)
                        "settings" -> SettingsScreen(container, permissions, actions, settings, state.tracking, installed, sessions)
                    }
                }
            }
        }
        if (folderScanOpen) FolderScanSheet(container) { folderScanOpen = false }
        if (options) ModalBottomSheet(onDismissRequest = { options = false }, containerColor = SingalongColors.Surface,
            contentColor = SingalongColors.Text) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Lyric options", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(controlTab, { controlTab = true }, label = { Text("Controls") })
                    FilterChip(!controlTab, { controlTab = false }, label = { Text("Versions & player") })
                }
                AnimatedVisibility(controlTab) {
                    ReaderControls(reader, container.reader::adjust, container.reader::experimentalSync, container.reader::follow, container.reader::setDelay, container.reader::alignLine)
                }
                reader.translationStatus?.takeIf { settings.translationEnabled }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted) }
                if (!controlTab && reader.candidates.isNotEmpty()) {
                    Text("Saved versions", style = MaterialTheme.typography.titleMedium)
                    reader.candidates.forEach { version -> ActionButton(onClick = { container.reader.select(version) }) {
                        Icon(if (reader.version?.id == version.id) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked, null)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(version.attribution ?: version.sourceName.orEmpty())
                            Text(com.kmab.singalong.lyrics.WordSync.label(version), style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted)
                        }
                    } }
                }
                if (!controlTab && sessions.size > 1) {
                    Text("Follow a player", style = MaterialTheme.typography.titleMedium)
                    FilterChip(selected = override == null, onClick = { container.chooseSession(null) }, label = { Text("Automatic") })
                    sessions.filter { it.packageName in settings.allowedPlayers }.forEach { session ->
                        FilterChip(selected = override == session.sessionId, onClick = { container.chooseSession(session.sessionId) }, label = { Text(session.playerName) })
                    }
                }
                OutlinedButton(onClick = { toolsId = reader.snapshot?.recording?.id; tools = true; options = false }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Find, import or edit lyrics")
                }
                val originalLines = reader.version?.lines.orEmpty()
                val translateLink = remember(originalLines) { com.kmab.singalong.translation.GoogleTranslateLink.from(originalLines) }
                if (originalLines.any { it.text.isNotBlank() }) {
                    OutlinedButton(onClick = { actions.translateLyrics(originalLines) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Translate, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Open in Google Translate")
                    }
                    Text(when {
                        translateLink == null -> "Copies the full original lyrics and opens Google Translate with English selected. These lyrics are too long for a link; paste them in sections of up to 5,000 characters."
                        translateLink.exceedsInputLimit -> "Copies the full original lyrics and tries to fill Google Translate with English selected. Paste if needed; Google limits input to 5,000 characters at a time."
                        else -> "Copies the full original lyrics, then opens Google Translate with English selected. Paste if the input is empty. Results stay in Google Translate."
                    }, style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
                }
                ActionButton(onClick = { destination = "settings"; options = false }) { Text("Appearance & translation settings") }
            }
        }
    }
}
