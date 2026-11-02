package com.kmab.singalong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.overlay.PreviewLyricsPanel
import com.kmab.singalong.settings.AppSettings
import com.kmab.singalong.tracking.PlayerApp
import com.kmab.singalong.ui.theme.SingalongColors
import kotlin.math.roundToInt

@Composable
fun ColumnScope.SettingsScreen(container: AppContainer, permissions: PermissionStatus, actions: FoundationActions,
    settings: AppSettings, tracking: Boolean, installed: List<PlayerApp>, sessions: List<PlaybackSnapshot>) {
    var page by rememberSaveable { mutableStateOf("index") }
    var query by rememberSaveable { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    BackHandler(page != "index") { page = "index" }
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("Translate opened lyrics?") },
        text = { Text("MyMemory receives each lyric line and stores submitted text on its servers. English translations are saved on this phone with the original lyrics. Its free daily limit may interrupt translation. Original lyrics stay available.") },
        confirmButton = { ActionButton(onClick = { consent = false; container.translation(true) }) { Text("Enable translations") } },
        dismissButton = { ActionButton(onClick = { consent = false }) { Text("Not now") } })
    AnimatedContent(page, Modifier.weight(1f), transitionSpec = {
        (fadeIn(SingalongMotion.tween()) + slideInHorizontally { it / 12 }) togetherWith fadeOut(SingalongMotion.tween())
    }, label = "Settings page") { section ->
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                if (section != "index") ActionButton(onClick = { page = "index" }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null); Spacer(Modifier.width(8.dp)); Text("Settings") }
                PageHeading(when (section) { "access" -> "Music access"; "players" -> "Music apps"; "appearance" -> "Look & feel"; "translation" -> "English subtext"; "storage" -> "Library & backup"; "about" -> "About Singalong"; else -> "Your setup" })
            }
            when (section) {
                "index" -> {
                    item { GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (tracking) "Following your music" else "Ready to follow your music", style = MaterialTheme.typography.titleMedium)
                        Text("${settings.allowedPlayers.size} apps selected · lyrics saved as you listen", color = SingalongColors.Muted, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = if (tracking) container::disableTracking else container::enableTracking, enabled = settings.ready,
                            modifier = Modifier.fillMaxWidth()) { Text(if (tracking) "Pause tracking" else "Start tracking") }
                    } } }
                    item { SettingsLink("Music access", "Permissions and connection", Icons.Outlined.Security) { page = "access" } }
                    item { SettingsLink("Music apps", "Choose which players to follow", Icons.Outlined.LibraryMusic) { page = "players" } }
                    item { SettingsLink("Look & feel", "Lyric size, glass and edge position", Icons.Outlined.Palette) { page = "appearance" } }
                    item { SettingsLink("English subtext", "Optional translations beneath the original", Icons.Outlined.Translate) { page = "translation" } }
                    item { SettingsLink("Library & backup", "Export, restore and manage saved words", Icons.Outlined.FolderOpen) { page = "storage" } }
                    item { SettingsLink("About Singalong", "App details, credits and feedback", Icons.Outlined.Info) { page = "about" } }
                    item { Text("Singalong · by kmab", Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted) }
                }
                "access" -> {
                    item { Text("Read song details from your players. Singalong never records your microphone.", color = SingalongColors.Muted) }
                    item { PermissionRow("Music access", permissions.music, actions.musicAccess) }
                    item { PermissionRow("Display over other apps", permissions.overlay, actions.overlayAccess) }
                    item { PermissionRow("Notifications", permissions.notifications, actions.notificationAccess) }
                    item { SettingsLink("Quiet tracking notification", "Silent while tracking. Status-bar visibility follows your phone's notification settings.", Icons.Outlined.NotificationsOff, actions.trackingNotificationSettings) }
                    item { ToggleSetting("Download on Wi-Fi only", "Saved lyrics remain available on any connection.", settings.wifiOnly, container::wifiOnly) }
                    item { Text("Open Singalong after restarting your phone to resume tracking.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted) }
                }
                "players" -> {
                    item { Text("Choose your music players. Selected apps stay at the top.", color = SingalongColors.Muted) }
                    item { OutlinedTextField(query, { query = it }, label = { Text("Search installed apps") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    val apps = (installed + sessions.map { PlayerApp(it.packageName, it.playerName) } + settings.allowedPlayers.map { PlayerApp(it, it) })
                        .distinctBy { it.packageName }.sortedWith(compareByDescending<PlayerApp> { it.packageName in settings.allowedPlayers }.thenBy { it.label.lowercase() })
                        .filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
                    items(apps, key = { it.packageName }) { app ->
                        GlassPanel { Row(Modifier.fillMaxWidth().clickable { container.allowPlayer(app.packageName, app.packageName !in settings.allowedPlayers) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlayerIcon(app.packageName)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(app.label, style = MaterialTheme.typography.titleMedium); Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted) }
                            Checkbox(app.packageName in settings.allowedPlayers, { container.allowPlayer(app.packageName, it) }, Modifier.semantics { contentDescription = "Follow ${app.label}" })
                        } }
                    }
                }
                "appearance" -> {
                    item { PreviewLyricsPanel(settings.translationEnabled, Modifier.fillMaxWidth(), textSize = settings.textSizeSp, translationSize = settings.translationSizeSp) }
                    item { ToggleSetting("Minimal floating lyrics", "Only the words. Tap to reveal controls; long-press and drag to move.", settings.minimalMode, container::minimalPanel) }
                    item { SettingSlider("Lyric size", settings.textSizeSp, 12f..32f, "${settings.textSizeSp.roundToInt()} sp", container::textSize) }
                    item { SettingSlider("Glass opacity", settings.opacity, .20f.. .96f, "${(settings.opacity * 100).roundToInt()}%", container::opacity) }
                    item { Text("Background blur follows your phone's settings. A text shadow helps at lower opacity; readability also depends on the app underneath.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted) }
                    item { SettingSlider("Floating width", settings.widthDp, 160f..560f, "${settings.widthDp.roundToInt()} dp", { container.geometry(settings.xFraction, settings.yFraction, it, settings.heightDp) }) }
                    item { SettingSlider("Floating height", settings.heightDp, 120f..640f, "${settings.heightDp.roundToInt()} dp", { container.geometry(settings.xFraction, settings.yFraction, settings.widthDp, it) }) }
                    item { SettingSlider("Minimal height", settings.minimalHeightDp, 56f..240f, "${settings.minimalHeightDp.roundToInt()} dp", container::minimalHeight) }
                    item { GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Edge handle", style = MaterialTheme.typography.titleMedium)
                        Text("Dock the lyrics, then tap or swipe the tab inward to reopen. Drag it up or down to move it.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(settings.xFraction < .5f, { container.geometry(0f, settings.yFraction, settings.widthDp, settings.heightDp) }, label = { Text("Left edge") })
                            FilterChip(settings.xFraction >= .5f, { container.geometry(1f, settings.yFraction, settings.widthDp, settings.heightDp) }, label = { Text("Right edge") })
                        }
                    } } }
                    item { OutlinedButton(onClick = if (permissions.overlay) container::showLyrics else actions.overlayAccess, modifier = Modifier.fillMaxWidth()) { Text("Try floating lyrics") } }
                    item { Text("Motion follows Android's animation settings.", style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted) }
                }
                "translation" -> {
                    item { ToggleSetting("English beneath the original", "Translate only lyrics you open. Originals always appear first.", settings.translationEnabled,
                        { if (it) consent = true else container.translation(false) }) }
                    item { SettingSlider("English text size", settings.translationSizeSp, 10f..22f, "${settings.translationSizeSp.roundToInt()} sp", container::translationSize) }
                    item { GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("MyMemory", style = MaterialTheme.typography.titleMedium)
                        Text("English lines appear as they arrive, with repeated lines reused. Translations are saved for offline reading. Language detection happens on this phone.", color = SingalongColors.Muted)
                        Text("MyMemory stores submitted text. Its anonymous allowance is limited; unavailable translations never hide the original lyrics.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
                    } } }
                }
                "storage" -> {
                    item { GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { BackupControls(container) } } }
                    item { GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lyric sources", style = MaterialTheme.typography.titleMedium)
                        Text("LRCLIB · timed and plain lyrics\nlyrics.ovh · plain-lyric fallback", color = SingalongColors.Muted)
                        Text("Each saved version keeps its source. Coverage varies by song.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
                    } } }
                }
                "about" -> item { AboutContent(actions.openLink) }
            }
        }
    }
}

@Composable private fun SettingsLink(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = SingalongColors.PurpleText)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted) }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = SingalongColors.Muted)
        }
    }
}
@Composable private fun PermissionRow(title: String, allowed: Boolean, action: () -> Unit) {
    SettingsLink(title, if (allowed) "Allowed · manage" else "Tap to allow", if (allowed) Icons.Outlined.CheckCircle else Icons.Outlined.Security, action)
}
@Composable private fun ToggleSetting(title: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    GlassPanel { Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted) }
        Switch(value, onChange, Modifier.semantics { contentDescription = title })
    } }
}
@Composable private fun SettingSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.coerceIn(range)) }
    GlassPanel { Column(Modifier.padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleMedium); Text(display, style = MaterialTheme.typography.labelMedium, color = SingalongColors.PurpleText) }
        Slider(draft, { draft = it }, onValueChangeFinished = { onChange(draft) }, valueRange = range, modifier = Modifier.semantics { contentDescription = title })
    } }
}
