package com.kmab.singalong.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.MatchConfidence
import com.kmab.singalong.ui.theme.SingalongColors

fun lookupMessage(status: String) = when (status) {
    "loading" -> "Finding the words…"
    "offline" -> "Waiting for a connection. Saved lyrics are available offline."
    "not_found" -> "No lyrics found in the available sources."
    "instrumental" -> "An instrumental moment."
    "rate_limited" -> "The lyrics source needs a moment."
    "network_error", "unavailable" -> "Couldn't reach the lyrics sources."
    "invalid_response" -> "The lyrics source returned unreadable data."
    else -> "Play a song in one of your selected music apps."
}

@Composable
fun LyricsReader(state: ReaderState, modifier: Modifier = Modifier, compact: Boolean = false, textSize: Float = 22f,
    translationSize: Float = 16f, onFollow: (Boolean) -> Unit = {}, onRetry: () -> Unit = {}) {
    // Do not animate the whole reader on every clock tick.
    Crossfade(state.version?.id ?: "empty", modifier, animationSpec = SingalongMotion.tween(), label = "Lyric version") { versionId ->
        val version = state.version?.takeIf { it.id == versionId }
        if (version == null) {
            Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 28.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.status == "loading") CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(if (state.status == "offline") Icons.Outlined.CloudOff else Icons.Outlined.Lyrics, null,
                    Modifier.size(28.dp), tint = SingalongColors.Purple)
                Spacer(Modifier.height(16.dp))
                AnimatedContent(lookupMessage(state.status), transitionSpec = { fadeIn(SingalongMotion.tween()) togetherWith fadeOut(SingalongMotion.tween()) }, label = "Lookup status") {
                    Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                state.retryInSeconds?.let { Text("Retrying in ${it}s", Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted) }
                if (state.status !in listOf("idle", "loading", "instrumental", "offline"))
                    TextButton(onClick = onRetry) { Text(if (state.retryInSeconds != null) "Retry now" else "Try again") }
            }
        } else {
            val list = rememberLazyListState()
            val dragged by list.interactionSource.collectIsDraggedAsState()
            LaunchedEffect(dragged) { if (dragged) onFollow(false) }
            LaunchedEffect(version.id, state.moment.index, state.following, compact) {
                if (state.canSync && state.following && state.moment.index >= 0 && !compact)
                    list.animateScrollToItem(state.moment.index, -(list.layoutInfo.viewportSize.height / 3))
            }
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(state.canSync && !state.following) {
                    TextButton(onClick = { onFollow(true) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Follow lyrics")
                    }
                }
                if (!state.canSync && !compact) Text(
                    if (version.confidence == MatchConfidence.UNCERTAIN && !state.allowUncertainSync) "Possible match · scroll to read" else "Scroll to read",
                    Modifier.padding(horizontal = 24.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                if (compact && state.canSync && state.following) {
                    AnimatedContent(state.moment.index, Modifier.weight(1f), transitionSpec = {
                        (fadeIn(SingalongMotion.tween()) + slideInVertically { it / 5 }) togetherWith
                            (fadeOut(SingalongMotion.tween()) + slideOutVertically { -it / 5 })
                    }, label = "Current line") { index ->
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.Center) {
                            val line = version.lines.getOrNull(index)
                            Text(line?.text?.ifBlank { "♪" } ?: state.moment.countdownSeconds?.let { "Lyrics in ${it}s" } ?: "Waiting for the first line",
                                fontSize = textSize.sp, lineHeight = (textSize * 1.35f).sp, fontWeight = FontWeight.SemiBold)
                            line?.id?.let { state.translation?.lines?.get(it) }?.let {
                                Text(it, Modifier.padding(top = 8.dp), fontSize = minOf(translationSize, textSize - 2).sp,
                                    lineHeight = (translationSize * 1.4f).sp, color = SingalongColors.PurpleText)
                            }
                            version.lines.getOrNull(index + 1)?.text?.takeIf { it.isNotBlank() }?.let {
                                Text(it, Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted, maxLines = 2)
                            }
                        }
                    }
                } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list,
                    contentPadding = PaddingValues(horizontal = if (compact) 22.dp else 24.dp, vertical = if (compact) 12.dp else 32.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 28.dp)) {
                    itemsIndexed(version.lines, key = { _, line -> line.id }) { index, line ->
                        val active = !state.canSync || !state.following || index == state.moment.index
                        val color by animateColorAsState(if (active) SingalongColors.Text else SingalongColors.Muted,
                            animationSpec = SingalongMotion.tween(), label = "Line emphasis")
                        Column {
                            Text(line.text.ifBlank { "♪" }, fontSize = textSize.sp, lineHeight = (textSize * 1.45f).sp,
                                fontWeight = FontWeight.SemiBold, color = color)
                            AnimatedVisibility(state.translation?.lines?.containsKey(line.id) == true) {
                                Text(state.translation?.lines?.get(line.id).orEmpty(), Modifier.padding(top = 8.dp),
                                    fontSize = minOf(translationSize, textSize - 2).sp, lineHeight = (translationSize * 1.5f).sp,
                                    color = SingalongColors.PurpleText)
                            }
                        }
                    }
                    if (!compact) item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReaderControls(state: ReaderState, adjust: (Long?, Boolean) -> Unit) {
    if (state.version?.lines?.any { it.startMs != null } != true) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Lyric timing", style = MaterialTheme.typography.titleMedium)
        Text("Move the words earlier or later to match what you hear.", style = MaterialTheme.typography.bodyMedium, color = SingalongColors.Muted)
        if (state.version.confidence == MatchConfidence.UNCERTAIN && !state.allowUncertainSync)
            OutlinedButton(onClick = { adjust(0, true) }) { Text("Try timing for this version") }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { adjust(-250, false) }) { Text("− 0.25s") }
            TextButton(onClick = { adjust(null, false) }) { Text("${state.delayMs / 1000.0}s · Reset") }
            OutlinedButton(onClick = { adjust(250, false) }) { Text("+ 0.25s") }
        }
    }
}
