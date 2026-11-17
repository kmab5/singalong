package com.kmab.singalong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Agreement is local to this installation and is not restored from library backups. */
@Composable
internal fun TermsAgreement(openLink: (String) -> Unit): Boolean {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("legal_agreement", android.content.Context.MODE_PRIVATE) }
    var accepted by remember { mutableStateOf(preferences.getInt("terms_version", 0) >= 1) }
    if (!accepted) AlertDialog(onDismissRequest = {}, title = { Text("Welcome to Singalong") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Singalong reads music metadata and playback position from selected players. Online lookup sends song details to lyric providers. Optional translation sends lyrics after separate consent. ML Kit processes language locally and may send SDK metrics to Google.")
            Text("Lyrics and artwork retain their owners' rights. Only import, translate or share content you are entitled to use. Review our terms and the linked service conditions before continuing.")
            ActionButton(onClick = { openLink("https://kmab5.github.io/singalong/terms.html") }) { Text("Read terms of use") }
            ActionButton(onClick = { openLink("https://kmab5.github.io/singalong/privacy.html") }) { Text("Read privacy policy") }
        }
    }, confirmButton = { ActionButton(onClick = {
        preferences.edit().putInt("terms_version", 1).apply()
        accepted = true
    }) { Text("Agree and continue") } }, dismissButton = { ActionButton(onClick = { (context as? android.app.Activity)?.finish() }) { Text("Exit") } })
    return accepted
}
