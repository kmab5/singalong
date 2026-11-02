package com.kmab.singalong.ui

import android.os.SystemClock
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kmab.singalong.editor.LyricTimestamp
import com.kmab.singalong.editor.TimingDraft
import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

@Composable
fun TimingEditor(
    text: String,
    draft: TimingDraft,
    recordingId: String?,
    playback: StateFlow<PlaybackSnapshot?>,
    enabled: Boolean,
    onChange: (TimingDraft) -> Unit,
) {
    val rows = remember(text) { text.lines() }
    val snapshot by playback.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(SystemClock.elapsedRealtime(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { value = SystemClock.elapsedRealtime(); delay(100) }
        }
    }
    val position = TimingDraft.playbackPosition(recordingId, snapshot, now)
    var choosing by rememberSaveable { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val structuralMatch = rows.size == draft.times.size
    fun change(action: () -> TimingDraft) {
        try { onChange(action()); actionError = null }
        catch (e: IllegalArgumentException) { actionError = e.message }
    }
    val error = actionError ?: draft.problem(rows.size)
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Line timing", style = MaterialTheme.typography.titleMedium)
            Text("Choose a line and enter its start time, or tap as you hear it. Turn off Edit timing to change the words.",
                style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
            Text("You can save partway through; lines without times won't be followed automatically.",
                style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
            OutlinedButton(onClick = { choosing = true }, enabled = enabled && structuralMatch, modifier = Modifier.fillMaxWidth()) {
                Text("Line ${draft.selectedIndex + 1} of ${rows.size}", Modifier.weight(1f))
                Icon(Icons.Outlined.ExpandMore, "Choose lyric line")
            }
            Text(rows.getOrNull(draft.selectedIndex)?.ifBlank { "Instrumental / blank line" }.orEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = draft.input,
                onValueChange = { actionError = null; onChange(draft.copy(timeInput = it)) },
                modifier = Modifier.fillMaxWidth(), enabled = enabled && structuralMatch,
                label = { Text("Start time · mm:ss.mmm") },
                placeholder = { Text("01:23.450") }, singleLine = true,
                supportingText = { Text("Leave blank to keep this line untimed.") },
                isError = draft.problem(rows.size) != null,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(enabled = enabled && structuralMatch, onClick = { change { draft.nudge(-100) } },
                    modifier = Modifier.semantics { contentDescription = "Move line 0.1 seconds earlier" }) { Text("−0.1 s") }
                ActionButton(enabled = enabled && structuralMatch, onClick = { change { draft.nudge(100) } },
                    modifier = Modifier.semantics { contentDescription = "Move line 0.1 seconds later" }) { Text("+0.1 s") }
                ActionButton(enabled = enabled && structuralMatch, onClick = { change { draft.setTime(null) } }) { Text("clear time") }
            }
            Text(if (position != null) "Playback · ${LyricTimestamp.format(position)}" else "Play this song in your selected music app to stamp lines.",
                style = MaterialTheme.typography.labelLarge, color = SingalongColors.PurpleText)
            Button(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled && structuralMatch && position != null,
                onClick = { change { draft.stamp(recordingId, playback.value, SystemClock.elapsedRealtime()) } }) {
                Icon(Icons.Outlined.Timer, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(if (draft.selectedIndex == rows.lastIndex) "stamp last line" else "stamp & next line")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = enabled && structuralMatch && draft.selectedIndex > 0,
                    onClick = { change { draft.select(draft.selectedIndex - 1) } }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous lyric line") }
                val timedCount = try { draft.resolvedTimes().count { it != null } } catch (_: IllegalArgumentException) { draft.times.count { it != null } }
                Text("$timedCount / ${rows.size} lines timed", style = MaterialTheme.typography.labelMedium)
                IconButton(enabled = enabled && structuralMatch && draft.selectedIndex < rows.lastIndex,
                    onClick = { change { draft.select(draft.selectedIndex + 1) } }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next lyric line") }
            }
            error?.let { Text(it, color = SingalongColors.Critical, style = MaterialTheme.typography.bodyMedium) }
        }
    }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Choose a lyric line") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                itemsIndexed(rows) { index, line ->
                    Column(Modifier.fillMaxWidth().clickable {
                        change { draft.select(index) }; choosing = false
                    }.padding(vertical = 12.dp).heightIn(min = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${index + 1} · ${LyricTimestamp.format(draft.times.getOrNull(index)).ifBlank { "Untimed" }}",
                            style = MaterialTheme.typography.labelMedium, color = SingalongColors.PurpleText)
                        Text(line.ifBlank { "Instrumental / blank line" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }, confirmButton = { ActionButton(onClick = { choosing = false }) { Text("close") } })
}
