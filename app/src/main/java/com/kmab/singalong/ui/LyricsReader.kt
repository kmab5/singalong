package com.kmab.singalong.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.model.MatchConfidence
import com.kmab.singalong.model.PlaybackStatus
import com.kmab.singalong.ui.theme.SingalongColors
import com.kmab.singalong.settings.*

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
    translationSize: Float = 16f, onFollow: (Boolean) -> Unit = {}, onRetry: () -> Unit = {}, minimal: Boolean = false, shadowText: Boolean = false,
    appearance: NowAppearance? = null) {
    val size = appearance?.textSizeSp ?: textSize
    val alignment = when (appearance?.alignment) { LyricAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center; LyricAlignment.END -> androidx.compose.ui.text.style.TextAlign.End; else -> androidx.compose.ui.text.style.TextAlign.Start }
    val animateText = appearance?.animation != LyricAnimation.NONE
    val textStyle = MaterialTheme.typography.bodyLarge.copy(shadow = if (shadowText) Shadow(Color.Black, Offset(0f, 1f), 6f) else null)
    val subtextSize = minOf(translationSize, size - 2)
    // Do not animate the whole reader on every clock tick.
    Crossfade(state.version, modifier, animationSpec = tween(450), label = "Lyric version") { version ->
        if (version == null) {
            Column(Modifier.fillMaxSize().padding(if (minimal) 8.dp else if (compact) 16.dp else 28.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (!minimal) {
                if (state.status == "loading") CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(if (state.status == "offline") Icons.Outlined.CloudOff else Icons.Outlined.Lyrics, null,
                    Modifier.size(28.dp), tint = SingalongColors.Purple)
                Spacer(Modifier.height(16.dp))
                }
                AnimatedContent(lookupMessage(state.status), transitionSpec = { fadeIn(SingalongMotion.tween()) togetherWith fadeOut(SingalongMotion.tween()) }, label = "Lookup status") {
                    Text(it, style = textStyle, fontSize = if (minimal) textSize.sp else textStyle.fontSize, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                state.retryInSeconds?.let { Text("Retrying in ${it}s", Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium, color = SingalongColors.Muted) }
                if (!minimal && state.status !in listOf("idle", "loading", "instrumental", "offline"))
                    ActionButton(onClick = onRetry) { Text(if (state.retryInSeconds != null) "Retry now" else "Try again") }
            }
        } else {
            val list = rememberLazyListState()
            var focusedLine by remember(version.id) { mutableStateOf<Int?>(null) }
            LaunchedEffect(state.following) { if (state.following) focusedLine = null }
            val dragged by list.interactionSource.collectIsDraggedAsState()
            LaunchedEffect(dragged) { if (dragged) onFollow(false) }
            LaunchedEffect(version.id, state.moment.index, state.following, compact) {
                if (state.canSync && state.following && state.moment.index >= 0 && !compact) {
                    snapshotFlow { list.layoutInfo.viewportSize.height }.first { it > 0 }
                    val anchor = list.layoutInfo.viewportSize.height / 3
                    val visible = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == state.moment.index }
                    if (!animateText) list.scrollToItem(state.moment.index, -anchor)
                    else if (visible != null) list.animateScrollBy((visible.offset - anchor).toFloat(), tween(650, easing = FastOutSlowInEasing))
                    else list.animateScrollToItem(state.moment.index, -anchor)
                }
            }
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(!minimal && state.canSync && !state.following) {
                    ActionButton(onClick = { onFollow(true) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Follow lyrics")
                    }
                }
                if (state.estimated && !compact && !minimal) Text("Experimental · estimated sync", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.PurpleText)
                if (appearance?.animation == LyricAnimation.WORD_GLOW && state.hasTiming && state.canSync && !compact && !minimal)
                    Text(if (!appearance.wordTrackingEnabled) "Word tracking off · following lines"
                        else if (version.lines.getOrNull(state.moment.index)?.let(com.kmab.singalong.lyrics.WordSync::valid) == true)
                        "Glow · word-synced" else "Glow · estimated word progression",
                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                if (version.confidence == MatchConfidence.UNCERTAIN && state.hasTiming && state.canSync && !compact && !minimal)
                    Text("Possible match · using saved timestamps", Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                if (!state.canSync && !compact && !minimal) Text(
                    if (version.confidence == MatchConfidence.UNCERTAIN && !state.allowUncertainSync) "Possible match · scroll to read" else "Scroll to read",
                    Modifier.padding(horizontal = 24.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                if (compact && state.canSync && state.following) {
                    AnimatedContent(state.moment.index, Modifier.weight(1f), transitionSpec = {
                        (fadeIn(SingalongMotion.tween()) + slideInVertically { it / 5 }) togetherWith
                            (fadeOut(SingalongMotion.tween()) + slideOutVertically { -it / 5 })
                    }, label = "Current line") { index ->
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (minimal) 12.dp else 22.dp, vertical = if (minimal) 6.dp else 10.dp),
                            verticalArrangement = Arrangement.Center) {
                            val line = version.lines.getOrNull(index)
                            Text(line?.text?.ifBlank { "♪" } ?: state.moment.countdownSeconds?.let { "Lyrics in ${it}s" } ?: "Waiting for the first line",
                                style = textStyle, fontSize = textSize.sp, lineHeight = (textSize * 1.35f).sp, fontWeight = FontWeight.SemiBold)
                            line?.id?.let { state.translation?.lines?.get(it) }?.let {
                                Text(it, Modifier.padding(top = if (minimal) 4.dp else 8.dp), style = textStyle, fontSize = subtextSize.sp,
                                    lineHeight = (subtextSize * 1.4f).sp, color = SingalongColors.PurpleText)
                            }
                            if (!minimal) version.lines.getOrNull(index + 1)?.text?.takeIf { it.isNotBlank() }?.let {
                                Text(it, Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyMedium.copy(shadow = textStyle.shadow), color = SingalongColors.Muted, maxLines = 2)
                            }
                        }
                    }
                } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list,
                    contentPadding = PaddingValues(horizontal = if (minimal) 12.dp else if (compact) 22.dp else 24.dp, vertical = if (minimal) 6.dp else if (compact) 12.dp else 32.dp),
                    verticalArrangement = Arrangement.spacedBy(if (minimal) 10.dp else if (compact) 18.dp else 28.dp)) {
                    itemsIndexed(version.lines, key = { _, line -> line.id }) { index, line ->
                        val active = if (focusedLine != null && !state.following) index == focusedLine else !state.canSync || !state.following || index == state.moment.index
                        val timedActive = state.canSync && state.following && index == state.moment.index
                        val color by animateColorAsState(if (active || appearance?.dimInactive == false) SingalongColors.Text else SingalongColors.Muted,
                            animationSpec = if (animateText) tween(650) else snap(), label = "Line emphasis")
                        Column(Modifier.fillMaxWidth()
                            .then(if (appearance?.tapToFocus == true) Modifier.clickable(onClickLabel = "Focus lyric line") { focusedLine = index; onFollow(false) } else Modifier)) {
                            val lineStyle = textStyle.copy(fontSize = size.sp, lineHeight = (size * (appearance?.lineSpacing ?: 1.45f)).sp, fontWeight = FontWeight.SemiBold, color = color)
                            if (appearance != null) AnimatedLyricText(line.text, size,
                                if (state.hasTiming || state.allowUncertainSync) appearance.timedTextSizeSp else size, timedActive,
                                glow = appearance.wordTrackingEnabled && appearance.animation == LyricAnimation.WORD_GLOW && state.hasTiming,
                                gentle = appearance.animation == LyricAnimation.FOCUS, animate = animateText,
                                progress = if (index < state.moment.index) 1f else if (index == state.moment.index) state.wordProgress ?: 0f else 0f,
                                playing = state.snapshot?.status == PlaybackStatus.PLAYING, style = lineStyle,
                                lineSpacing = appearance.lineSpacing, alignment = alignment,
                                wordTimings = if (appearance.wordTrackingEnabled && com.kmab.singalong.lyrics.WordSync.valid(line)) line.words else emptyList(),
                                playback = state.snapshot, delayMs = state.delayMs,
                                wordLineEndMs = if (appearance.wordTrackingEnabled) com.kmab.singalong.lyrics.WordSync.displayEnd(version.lines, index, state.snapshot?.recording?.durationMs) else null)
                            else Text(line.text.ifBlank { "♪" }, Modifier.fillMaxWidth(), textAlign = alignment, style = lineStyle)
                            AnimatedVisibility(state.translation?.lines?.containsKey(line.id) == true) {
                                Text(state.translation?.lines?.get(line.id).orEmpty(), Modifier.fillMaxWidth().padding(top = if (minimal) 4.dp else 8.dp), textAlign = alignment, style = textStyle,
                                    fontSize = subtextSize.sp, lineHeight = (subtextSize * 1.5f).sp,
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
