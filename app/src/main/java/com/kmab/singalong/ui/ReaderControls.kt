package com.kmab.singalong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.ui.theme.SingalongColors

@Composable
fun ReaderControls(state: ReaderState, adjust: (Long?, Boolean) -> Unit, experimental: (Boolean) -> Unit,
    follow: (Boolean) -> Unit, setDelay: (Long) -> Unit, alignLine: (Int) -> Unit) {
    val version = state.version
    if (version == null) { Text("Open lyrics to adjust synchronization.", color = SingalongColors.Muted); return }
    var step by rememberSaveable { mutableLongStateOf(50L) }
    var offset by rememberSaveable(state.delayMs) { mutableStateOf((state.delayMs / 1000.0).toString()) }
    var lineMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(if (state.experimental) "Experimental sync" else "Follow lyrics", style = MaterialTheme.typography.titleMedium)
                Text(when {
                    !state.canTrySync -> "Waiting for playback position${if (!state.hasTiming) " and song duration" else ""}."
                    !state.hasTiming -> "Estimated from line length and song duration. Timing may drift."
                    state.experimental -> "Try the timestamps of this possible match."
                    else -> "Use this version's saved timestamps."
                }, style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
            }
            Switch(if (state.experimental) state.allowUncertainSync else state.following,
                { if (state.experimental) experimental(it) else follow(it) }, enabled = state.canTrySync || state.allowUncertainSync,
                modifier = Modifier.semantics { contentDescription = if (state.experimental) "Experimental sync" else "Follow lyrics" })
        }
        if (state.estimated) Text("Estimated · original lyrics have no timestamps", style = MaterialTheme.typography.labelMedium, color = SingalongColors.PurpleText)
        Text("Adjustment step", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(50L, 250L, 1000L).forEach { value ->
                FilterChip(step == value, { step = value }, label = { Text(if (value == 1000L) "1 second" else "$value ms") })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(onClick = { adjust(-step, false) }, enabled = state.canSync) { Text("Earlier −${step}ms") }
            ActionButton(onClick = { adjust(step, false) }, enabled = state.canSync) { Text("Later +${step}ms") }
        }
        val seconds = offset.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it in -600.0..600.0 }
        OutlinedTextField(offset, { offset = it }, label = { Text("Offset in seconds") }, singleLine = true,
            supportingText = { Text("Positive = later · negative = earlier · ±600 seconds") },
            isError = seconds == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(onClick = { seconds?.let { setDelay((it * 1000).toLong()) } }, enabled = state.canSync && seconds != null) { Text("Apply offset") }
            ActionButton(onClick = { adjust(null, false) }) { Text("Reset offset") }
        }
        Box {
            ActionButton(onClick = { lineMenu = true }, enabled = state.canSync) { Text("Align a line with now") }
            DropdownMenu(lineMenu, { lineMenu = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                version.lines.forEachIndexed { index, line -> if (line.text.isNotBlank() && (state.estimated || line.startMs != null)) {
                    DropdownMenuItem(text = { Text(line.text, maxLines = 2) }, onClick = { alignLine(index); lineMenu = false })
                } }
            }
        }
    }
}
