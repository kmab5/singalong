package com.kmab.singalong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kmab.singalong.R
import com.kmab.singalong.app.FoundationState
import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.overlay.PreviewLyricsPanel
import com.kmab.singalong.ui.theme.SingalongColors
import com.kmab.singalong.ui.theme.SingalongTheme

data class PermissionStatus(val music: Boolean = false, val overlay: Boolean = false, val notifications: Boolean = false)

data class FoundationActions(
    val musicAccess: () -> Unit = {},
    val overlayAccess: () -> Unit = {},
    val notificationAccess: () -> Unit = {},
    val startTracking: () -> Unit = {},
    val stopTracking: () -> Unit = {},
    val showPreview: () -> Unit = {},
)

@Composable
fun FoundationScreen(
    state: FoundationState,
    sessions: List<PlaybackSnapshot>,
    permissions: PermissionStatus,
    actions: FoundationActions,
) {
    var destination by rememberSaveable { mutableStateOf("now") }
    var utility by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.activity.compose.BackHandler(enabled = utility != null) { utility = null }
    Scaffold(
        containerColor = SingalongColors.Background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (utility != null) {
                    IconButton(onClick = { utility = null }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Icon(painterResource(R.drawable.ic_kmab), contentDescription = "KMAB", tint = SingalongColors.Purple, modifier = Modifier.padding(end = 12.dp).size(28.dp))
                }
                Text(utility ?: "singalong", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (utility == null) IconButton(onClick = { utility = "settings" }) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Settings", tint = SingalongColors.Muted)
                }
            }
        },
        bottomBar = {
            if (utility == null) NavigationBar(containerColor = SingalongColors.Background, tonalElevation = 0.dp) {
                listOf("now" to Icons.Outlined.GraphicEq, "library" to Icons.Outlined.LibraryMusic, "history" to Icons.Outlined.History).forEach { (name, icon) ->
                    NavigationBarItem(
                        selected = destination == name,
                        onClick = { destination = name },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(name) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SingalongColors.PurpleText,
                            selectedTextColor = SingalongColors.Text,
                            indicatorColor = SingalongColors.Raised,
                            unselectedIconColor = SingalongColors.Muted,
                            unselectedTextColor = SingalongColors.Muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when (utility) {
                "settings" -> SetupContent(permissions, state, actions)
                "design previews" -> DesignGallery()
                else -> when (destination) {
                    "now" -> NowContent(state, sessions, permissions, actions, { utility = "settings" }, { utility = "design previews" })
                    "library" -> EmptyLibraryContent()
                    "history" -> EmptyHistoryContent()
                }
            }
            if (state.message != null) Surface(color = SingalongColors.Raised, shape = RoundedCornerShape(12.dp)) {
                Text(state.message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NowContent(
    state: FoundationState,
    sessions: List<PlaybackSnapshot>,
    permissions: PermissionStatus,
    actions: FoundationActions,
    openSetup: () -> Unit,
    openGallery: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(6.dp).background(if (state.tracking) SingalongColors.Green else SingalongColors.Muted, RoundedCornerShape(3.dp)))
        Text(if (state.tracking) "following your player" else "ready for your music", style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("keep the\nwords close.", style = MaterialTheme.typography.displaySmall)
        Text("Your music keeps playing.\nA small space for the words.", style = MaterialTheme.typography.bodyLarge, color = SingalongColors.Muted)
    }
    PreviewLyricsPanel(bilingual = false, modifier = Modifier.fillMaxWidth())
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = if (permissions.overlay) actions.showPreview else actions.overlayAccess,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (permissions.overlay) "open floating preview" else "set up floating lyrics")
        }
        Text("P0 preview · sample words only. Song lookup and saved lyrics arrive in the next phases.", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
    }
    HorizontalDivider(color = SingalongColors.Line)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("player connection", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = openSetup) { Text("manage", color = SingalongColors.PurpleText) }
    }
    if (sessions.isEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Headphones, contentDescription = null, tint = SingalongColors.Muted)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (state.tracking) "play a song in your music app" else "connect your music app", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.tracking) "Player details appear here when Android shares a media session." else "Allow music access and start tracking to check the current title, artist and timing.",
                    style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted,
                )
            }
        }
    } else sessions.forEach { SessionCard(it) }
    OutlinedButton(onClick = if (state.tracking) actions.stopTracking else actions.startTracking, enabled = permissions.music || state.tracking, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
        Text(if (state.tracking) "stop tracking" else "start tracking", color = SingalongColors.Text)
    }
    TextButton(onClick = openGallery) {
        Text("view design previews", color = SingalongColors.PurpleText)
    }
}

@Composable
private fun SessionCard(session: PlaybackSnapshot) {
    Surface(color = SingalongColors.Surface, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(session.playerName, style = MaterialTheme.typography.labelMedium, color = SingalongColors.PurpleText)
            Text(session.recording?.title ?: "No title shared", style = MaterialTheme.typography.titleMedium)
            Text(session.recording?.artists?.joinToString()?.ifBlank { "No artist shared" } ?: "No artist shared", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
            session.recording?.album?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted) }
            Text("${session.status.name.lowercase()} · ${session.positionMs?.let { "${it / 1000}s reported" } ?: "timing unavailable"}", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
        }
    }
}

@Composable
private fun SetupContent(permissions: PermissionStatus, state: FoundationState, actions: FoundationActions) {
    Text("a little setup.\nthen, just music.", style = MaterialTheme.typography.headlineMedium)
    Text("You control each permission. Music access reads media details, without recording audio.", style = MaterialTheme.typography.bodyLarge, color = SingalongColors.Muted)
    PermissionRow(Icons.Outlined.Headphones, "music access", "Read the song and playback state shared by your player. Android calls this notification access.", permissions.music, actions.musicAccess)
    PermissionRow(Icons.Outlined.Layers, "display over other apps", "Keep a small lyric panel above your music app.", permissions.overlay, actions.overlayAccess)
    PermissionRow(Icons.Outlined.NotificationsNone, "notifications", "Reopen the panel or stop tracking from a notification.", permissions.notifications, actions.notificationAccess)
    HorizontalDivider(color = SingalongColors.Line)
    Text("tracking", style = MaterialTheme.typography.titleMedium)
    Text("After Android stops the app or your phone restarts, open Singalong to resume. Closing the panel does not stop tracking.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
    Button(onClick = if (state.tracking) actions.stopTracking else actions.startTracking, enabled = permissions.music || state.tracking, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
        Text(if (state.tracking) "stop tracking" else "start tracking")
    }
    Text("${if (state.listenerConnected) "Music access connected" else "Music access not connected"} · ${if (state.overlayVisible) "panel visible" else "panel closed"}", style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
    HorizontalDivider(color = SingalongColors.Line)
    Text("about this build", style = MaterialTheme.typography.titleMedium)
    Text("P0 checks the app foundation, media connection and floating display. It does not fetch or store lyrics. Translation is unavailable in this build.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, description: String, granted: Boolean, onClick: () -> Unit) {
    Surface(color = SingalongColors.Surface, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = SingalongColors.Muted)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = if (granted) "Allowed" else "Not allowed", tint = if (granted) SingalongColors.Green else SingalongColors.Muted)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
            TextButton(onClick = onClick) { Text(if (granted) "manage access" else "allow access", color = SingalongColors.PurpleText) }
        }
    }
}

@Composable
fun EmptyLibraryContent() {
    Spacer(Modifier.height(24.dp))
    Text("your words,\nsaved.", style = MaterialTheme.typography.displaySmall)
    Icon(Icons.Outlined.LibraryMusic, contentDescription = null, tint = SingalongColors.PurpleText, modifier = Modifier.padding(top = 16.dp).size(40.dp))
    Text("Your library starts with a song.", style = MaterialTheme.typography.titleLarge)
    Text("Saved lyrics, imports and personal edits will live here. Library collection is part of the next development phases.", style = MaterialTheme.typography.bodyLarge, color = SingalongColors.Muted)
    Text("nothing saved yet", style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted)
}

@Composable private fun EmptyHistoryContent() {
    Spacer(Modifier.height(24.dp))
    Text("the songs\nyou come back to.", style = MaterialTheme.typography.displaySmall)
    Text("Listening history is not being recorded in this foundation build.", style = MaterialTheme.typography.bodyLarge, color = SingalongColors.Muted)
    Text("P1 adds played-song collection and play counts. Right now, tracking only checks live player details.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
}

@Composable private fun DesignGallery() {
    Text("one line.\ntwo ways to read.", style = MaterialTheme.typography.headlineMedium)
    Text("Original lyrics lead. English meaning sits beneath, when you choose it.", style = MaterialTheme.typography.bodyLarge, color = SingalongColors.Muted)
    PreviewLyricsPanel(false, Modifier.fillMaxWidth())
    PreviewLyricsPanel(true, Modifier.fillMaxWidth())
    Text("These are written samples for checking type, spacing and readability. They are not downloaded lyrics or an online translation.", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
}

@Preview(name = "now", widthDp = 393, heightDp = 852)
@Composable private fun NowPreview() = SingalongTheme {
    FoundationScreen(FoundationState(), emptyList(), PermissionStatus(), FoundationActions())
}

@Preview(name = "library", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF0E0E11)
@Composable private fun LibraryPreview() = SingalongTheme {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) { EmptyLibraryContent() }
}
