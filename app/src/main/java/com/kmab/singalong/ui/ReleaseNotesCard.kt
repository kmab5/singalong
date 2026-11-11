package com.kmab.singalong.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kmab.singalong.BuildConfig
import com.kmab.singalong.app.ReleaseNote
import com.kmab.singalong.app.ReleaseNotes
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun AboutReleaseNotes(openLink: (String) -> Unit) {
    val context = LocalContext.current
    val note by produceState<ReleaseNote?>(null) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open("releases.json").bufferedReader().use {
                    ReleaseNotes.decode(it.readText()).forVersion(BuildConfig.VERSION_NAME)
                }
            } catch (_: IOException) { null } catch (_: IllegalArgumentException) { null }
        }
    }
    note?.let { ReleaseNotesCard(it) } ?: ActionButton(onClick = { openLink("https://github.com/kmab5/singalong/releases") }) {
        Icon(Icons.Outlined.NewReleases, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp)); Text("Release notes")
    }
}

@Composable
internal fun ReleaseNotesCard(note: ReleaseNote) {
    var expanded by rememberSaveable(note.version) { mutableStateOf(false) }
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.NewReleases, null, tint = SingalongColors.PurpleText)
                Text("What's new · ${note.version}", style = MaterialTheme.typography.labelLarge, color = SingalongColors.PurpleText)
            }
            Text(note.title, style = MaterialTheme.typography.titleLarge)
            Text(note.summary, color = SingalongColors.Muted, style = MaterialTheme.typography.bodyMedium)
            ActionButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide release notes" else "Read release notes")
                Spacer(Modifier.width(8.dp)); Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    note.sections.forEach { section ->
                        val color = when (section.kind) { "new" -> SingalongColors.Green; "fixed" -> SingalongColors.Amber; else -> SingalongColors.PurpleText }
                        val icon = when (section.kind) { "new" -> Icons.Outlined.AutoAwesome; "fixed" -> Icons.Outlined.Build; else -> Icons.Outlined.Tune }
                        val label = when (section.kind) { "new" -> "New features"; "fixed" -> "Fixes"; else -> "Improvements" }
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(color = SingalongColors.Line)
                            Surface(color = color.copy(alpha = .1f), contentColor = color, shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, color.copy(alpha = .25f))) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(icon, null, Modifier.size(18.dp)); Text(label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            section.items.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                                    Text(item.body, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
