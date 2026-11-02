package com.kmab.singalong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.singalong.BuildConfig
import com.kmab.singalong.app.FeedbackLink
import com.kmab.singalong.ui.theme.SingalongColors

@Composable
fun AboutContent(openLink: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassPanel { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Singalong", style = MaterialTheme.typography.headlineMedium)
            Text("Keep the words close.", color = SingalongColors.Muted)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})${if (BuildConfig.DEBUG) " · Debug" else ""}", style = MaterialTheme.typography.labelMedium)
            Text("Made by Samuel · KMAB\nAndroid 12 and newer", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
        } }
        Button(onClick = { openLink(FeedbackLink.url(BuildConfig.VERSION_NAME)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Outlined.Feedback, null); Spacer(Modifier.width(10.dp)); Text("Send feedback")
        }
        Text("Opens the KMAB form with Singalong and this version selected. Add details or attachments yourself.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
        AboutSection("Your music, your data", "Singalong reads player metadata and playback position. It does not record your microphone. Lyrics, history, favorites and translations are stored on this phone; backups are exported only when you choose.")
        AboutSection("Lyrics & translations", "LRCLIB supplies timed and plain lyrics; lyrics.ovh adds plain-lyric coverage. English subtext uses MyMemory when enabled. Language detection runs locally with ML Kit. MyMemory receives and stores submitted lyric text. Provider coverage and translation accuracy vary.")
        AboutSection("Experimental sync", "Untimed lyrics can follow an estimated reading pace. This is an optional approximation, not audio recognition or word-level timing. Original text and timestamps are preserved.")
        AboutSection("Credits", "Design by KMAB. Inter and PT Mono fonts use the SIL Open Font License. Built with Kotlin, Jetpack Compose, Room, OkHttp and ML Kit. This personal app is independent of music players and lyric providers; source attribution is kept with each saved version.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(onClick = { openLink("https://github.com/kmab5/singalong") }) { Text("Source code & licenses") }
            ActionButton(onClick = { openLink("https://github.com/kmab5/kmab-brand") }) { Text("KMAB design") }
            ActionButton(onClick = { openLink("https://mymemory.translated.net/terms-and-conditions") }) { Text("Translation terms") }
        }
    }
}

@Composable private fun AboutSection(title: String, text: String) {
    GlassPanel { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
    } }
}
