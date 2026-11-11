package com.kmab.singalong.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.singalong.lyrics.ReaderState
import com.kmab.singalong.settings.AppSettings
import com.kmab.singalong.ui.*
import com.kmab.singalong.ui.theme.SingalongColors

@Composable
fun FloatingLyricsPanel(panel: OverlayState, settings: AppSettings, reader: ReaderState,
    onOpen: () -> Unit, onDock: () -> Unit, onClose: () -> Unit, onExpand: () -> Unit,
    onFollow: (Boolean) -> Unit, onRetry: () -> Unit,
    onMinimal: () -> Unit, onReveal: () -> Unit, onSync: () -> Unit,
    onDragStart: () -> Unit, onMove: (Float, Float) -> Unit, onDragEnd: () -> Unit,
    onOpenApp: () -> Unit = {},
    onLineLongPress: ((com.kmab.singalong.model.LyricVersion, Int) -> Unit)? = null) {
    val density = LocalDensity.current.density
    val left = settings.xFraction < .5f
    val docked = panel.mode == PanelMode.MANUAL_MINIMIZED || panel.mode == PanelMode.AUTO_MINIMIZED
    val minimal = settings.minimalMode && !panel.controlsVisible
    val drag = Modifier.semantics {
        contentDescription = "Move floating lyrics"
        customActions = buildList {
            add(CustomAccessibilityAction("Move up") { onMove(0f, -48 * density); onDragEnd(); true })
            add(CustomAccessibilityAction("Move down") { onMove(0f, 48 * density); onDragEnd(); true })
            add(CustomAccessibilityAction("Move left") { onDragStart(); onMove(-48 * density, 0f); onDragEnd(); true })
            add(CustomAccessibilityAction("Move right") { onDragStart(); onMove(48 * density, 0f); onDragEnd(); true })
        }
    }.pointerInput(docked, left) {
        detectDragGestures(onDragStart = { onDragStart() },
            onDragCancel = onDragEnd, onDragEnd = onDragEnd
        ) { change, offset -> change.consume(); onMove(offset.x, offset.y) }
    }
    Crossfade(docked, animationSpec = SingalongMotion.tween(), label = "Edge panel") { minimized ->
        if (minimized) Box(Modifier.fillMaxSize().then(drag).clickable(onClickLabel = "Open floating lyrics", onClick = onOpen), contentAlignment = if (left) Alignment.CenterStart else Alignment.CenterEnd) {
            Surface(Modifier.width(30.dp).height(88.dp), shape = RoundedCornerShape(14.dp),
                color = SingalongColors.Raised, contentColor = SingalongColors.PurpleText,
                border = BorderStroke(1.dp, SingalongColors.Purple.copy(alpha = .45f))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.GraphicEq, null, Modifier.size(18.dp))
                    Spacer(Modifier.height(8.dp))
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                        Icon(if (left) Icons.AutoMirrored.Outlined.KeyboardArrowRight else Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Open floating lyrics", Modifier.size(20.dp))
                    }
                }
            }
        } else GlassPanel(Modifier.fillMaxSize(), opacity = settings.opacity) {
            Crossfade(minimal, animationSpec = SingalongMotion.tween(), label = "Minimal lyrics") { lyricsOnly ->
            if (lyricsOnly) Box(Modifier.fillMaxSize()
                .semantics { customActions = listOf(
                    CustomAccessibilityAction("Show controls") { onReveal(); true },
                    CustomAccessibilityAction("Dock lyrics") { onDock(); true },
                    CustomAccessibilityAction("Close lyrics") { onClose(); true },
                    CustomAccessibilityAction("Move up") { onMove(0f, -48 * density); onDragEnd(); true },
                    CustomAccessibilityAction("Move down") { onMove(0f, 48 * density); onDragEnd(); true }) }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(onDragStart = { onDragStart() }, onDragEnd = onDragEnd, onDragCancel = onDragEnd) { change, offset ->
                        change.consume(); onMove(offset.x, offset.y)
                    }
                }.clickable(onClickLabel = "Show floating controls", onClick = onReveal)) {
                LyricsReader(reader.copy(translation = if (settings.translationEnabled) reader.translation else null), Modifier.fillMaxSize(),
                    compact = true, translationSize = settings.translationSizeSp,
                    floatingAppearance = settings.floating,
                    onFollow = onFollow, onRetry = onRetry, minimal = true, shadowText = settings.opacity < .85f)
            } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).then(drag).clickable(onClickLabel = "Open Singalong", onClick = onOpenApp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DragHandle, null, Modifier.size(20.dp), tint = SingalongColors.Muted)
                        Text(reader.snapshot?.recording?.title ?: "Singalong", Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalIconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close floating lyrics", Modifier.size(20.dp)) }
                }
                Box(Modifier.weight(1f).fillMaxWidth().clickable(onClickLabel = "Open Singalong", onClick = onOpenApp)) {
                    if (reader.version == null) FloatingEmptyState(reader, onRetry, onOpenApp)
                    else LyricsReader(reader.copy(translation = if (settings.translationEnabled) reader.translation else null),
                        Modifier.fillMaxSize(), compact = !panel.expanded,
                        floatingAppearance = settings.floating,
                        translationSize = settings.translationSizeSp, onFollow = onFollow, onRetry = onRetry, shadowText = settings.opacity < .85f,
                        onLineLongPress = onLineLongPress, onLineClick = onOpenApp)
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 4.dp), maxItemsInEachRow = OverlayControlsLayout.columns(maxWidth.value)) {
                        FilledTonalIconButton(onClick = onDock, modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.Outlined.VerticalAlignCenter, "Dock lyrics", Modifier.size(20.dp)) }
                        val syncOn = if (reader.experimental) reader.allowUncertainSync else reader.canSync && reader.following
                        FilledTonalIconToggleButton(checked = syncOn, onCheckedChange = { onSync() }, enabled = reader.canTrySync || syncOn, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(if (syncOn) Icons.Outlined.Sync else Icons.Outlined.SyncDisabled,
                                "${if (syncOn) "Turn off" else "Try"} ${if (reader.experimental) "experimental sync" else "lyric following"}", Modifier.size(20.dp))
                        }
                        FilledTonalIconButton(onClick = onMinimal, modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.AutoMirrored.Outlined.ShortText, "Minimal lyrics", Modifier.size(20.dp)) }
                        FilledTonalIconButton(onClick = onExpand, modifier = Modifier.weight(1f).height(48.dp)) { Icon(if (panel.expanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                            if (panel.expanded) "Compact view" else "Expanded view", Modifier.size(20.dp)) }
                        FilledTonalIconButton(onClick = onOpenApp, modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open Singalong", Modifier.size(20.dp)) }
                    }
                }
            }
            }
            }
        }
    }
}

@Composable
private fun FloatingEmptyState(reader: ReaderState, onRetry: () -> Unit, onOpenApp: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val roomy = maxHeight > 170.dp && maxWidth > 200.dp
        val loading = reader.status == "loading"
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            if (roomy) Icon(if (loading) Icons.Outlined.Search else Icons.Outlined.Lyrics, null, tint = SingalongColors.PurpleText)
            Text(when (reader.status) { "not_found" -> "No lyrics for this song yet"; "loading" -> "Looking for lyrics…"; "offline" -> "Waiting for internet";
                "idle" -> "Play a song to begin"; "instrumental" -> "Instrumental"; else -> "Lyrics couldn't load" },
                style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (roomy && reader.status !in listOf("idle", "instrumental", "loading")) Text(
                "Try again or open the app to search or add lyrics. This panel docks shortly and returns when lyrics load.",
                style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (!loading && reader.status !in listOf("idle", "instrumental")) {
                ActionButton(onClick = onRetry) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(reader.retryInSeconds?.let { "Retry in ${it}s" } ?: "Try again")
                }
                if (roomy) ActionButton(onClick = onOpenApp) { Text("search in app") }
            }
        }
    }
}
