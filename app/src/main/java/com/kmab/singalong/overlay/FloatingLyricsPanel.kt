package com.kmab.singalong.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
fun FloatingLyricsPanel(panel: OverlayState, settings: AppSettings, reader: ReaderState, blurEnabled: Boolean,
    onOpen: () -> Unit, onDock: () -> Unit, onClose: () -> Unit, onExpand: () -> Unit,
    onFollow: (Boolean) -> Unit, onRetry: () -> Unit,
    onDragStart: () -> Unit, onMove: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    val density = LocalDensity.current.density
    val left = settings.xFraction < .5f
    val docked = panel.mode == PanelMode.MANUAL_MINIMIZED || panel.mode == PanelMode.AUTO_MINIMIZED
    val drag = Modifier.semantics {
        contentDescription = "Move floating lyrics"
        customActions = buildList {
            add(CustomAccessibilityAction("Move up") { onMove(0f, -48 * density); onDragEnd(); true })
            add(CustomAccessibilityAction("Move down") { onMove(0f, 48 * density); onDragEnd(); true })
            if (!docked) {
                add(CustomAccessibilityAction("Move left") { onMove(-48 * density, 0f); onDragEnd(); true })
                add(CustomAccessibilityAction("Move right") { onMove(48 * density, 0f); onDragEnd(); true })
            }
        }
    }.pointerInput(docked, left) {
        var horizontal = 0f
        detectDragGestures(onDragStart = { horizontal = 0f; onDragStart() },
            onDragCancel = onDragEnd, onDragEnd = {
                onDragEnd()
                if (docked && (if (left) horizontal else -horizontal) > 24 * density) onOpen()
            }) { change, offset -> change.consume(); horizontal += offset.x; onMove(offset.x, offset.y) }
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
        } else GlassPanel(Modifier.fillMaxSize(), opacity = if (blurEnabled) settings.opacity.coerceIn(.85f, .96f) else .98f) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).then(drag), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DragHandle, null, Modifier.size(20.dp), tint = SingalongColors.Muted)
                        Text(reader.snapshot?.recording?.title ?: "Singalong", Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close floating lyrics", Modifier.size(20.dp)) }
                }
                LyricsReader(reader.copy(translation = if (settings.translationEnabled) reader.translation else null),
                    Modifier.weight(1f), compact = !panel.expanded, textSize = settings.textSizeSp,
                    translationSize = settings.translationSizeSp, onFollow = onFollow, onRetry = onRetry)
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDock) { Icon(Icons.Outlined.VerticalAlignCenter, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Dock") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onExpand) { Text(if (panel.expanded) "Compact" else "Expand"); Spacer(Modifier.width(6.dp)); Icon(if (panel.expanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore, null, Modifier.size(18.dp)) }
                }
            }
        }
    }
}
