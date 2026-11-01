package com.kmab.singalong.ui

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.MatchConfidence
import com.kmab.singalong.ui.theme.SingalongColors

fun lookupMessage(status: String) = when (status) {
    "loading" -> "Finding the words…"
    "offline" -> "Offline or waiting for Wi-Fi. Saved lyrics are still available."
    "not_found" -> "No matching lyrics found."
    "instrumental" -> "This recording is marked instrumental."
    "rate_limited" -> "The lyrics source is busy. Waiting before retrying."
    "network_error", "unavailable" -> "The lyrics source is unavailable. Try again shortly."
    "invalid_response" -> "The lyrics source returned unreadable data."
    else -> "Play a song in a selected music app."
}

@Composable
fun LyricsReader(state: ReaderState, modifier: Modifier = Modifier, compact: Boolean = false, textSize: Float = 22f,
    onFollow: (Boolean) -> Unit = {}, onRetry: () -> Unit = {}) {
    val version = state.version
    if (version == null) {
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lookupMessage(state.status), style = MaterialTheme.typography.bodyLarge)
            if (state.status !in listOf("idle", "loading", "instrumental")) TextButton(onClick = onRetry) { Text("try again") }
        }
        return
    }
    val list = rememberLazyListState()
    val dragged by list.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { if (dragged) onFollow(false) }
    LaunchedEffect(version.id, state.moment.index, state.following, compact) {
        if (state.canSync && state.following && state.moment.index >= 0 && !compact) list.scrollToItem(state.moment.index)
    }
    Column(modifier) {
        if (state.canSync && !state.following) TextButton(onClick = { onFollow(true) }) { Text("resume sync") }
        if (!state.canSync) Text(
            if (version.confidence == MatchConfidence.UNCERTAIN && !state.allowUncertainSync) "Possible match · read manually" else "Manual reading · timing unavailable",
            Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted,
        )
        if (compact && state.canSync && state.following) {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.Center) {
                val text = state.moment.countdownSeconds?.let { "Lyrics in ${it}s" }
                    ?: version.lines.getOrNull(state.moment.index)?.text?.ifBlank { "Instrumental" } ?: "Waiting for the first line"
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(3.dp).height(28.dp).background(SingalongColors.Purple))
                    Text(text, fontSize = textSize.sp, lineHeight = (textSize * 1.4f).sp)
                }
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            itemsIndexed(version.lines, key = { _, line -> line.id }) { index, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(3.dp).height(28.dp).background(if (state.canSync && state.following && index == state.moment.index) SingalongColors.Purple else androidx.compose.ui.graphics.Color.Transparent))
                    Text(line.text.ifBlank { "♪" }, fontSize = textSize.sp, lineHeight = (textSize * 1.4f).sp,
                        color = if (state.canSync && state.following && index != state.moment.index) SingalongColors.Muted else SingalongColors.Text)
                }
            }
        }
    }
}

@Composable
fun ReaderControls(state: ReaderState, adjust: (Long?, Boolean) -> Unit) {
    if (state.version?.lines?.any { it.startMs != null } != true) return
    if (state.version.confidence == MatchConfidence.UNCERTAIN && !state.allowUncertainSync)
        TextButton(onClick = { adjust(0, true) }) { Text("try sync for this version") }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { adjust(-250, false) }) { Text("earlier") }
        TextButton(onClick = { adjust(null, false) }) { Text("${state.delayMs / 1000.0}s · reset") }
        TextButton(onClick = { adjust(250, false) }) { Text("later") }
    }
}
