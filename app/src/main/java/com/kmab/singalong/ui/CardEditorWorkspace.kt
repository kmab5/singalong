package com.kmab.singalong.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmab.singalong.ui.theme.SingalongColors

internal enum class CardEditorSection(val label: String) { LOOKS("Looks"), TEXT("Text"), DETAILS("Details"), LINES("Lines") }

/** The preview and export actions never enter the scrolling settings region. */
@Composable
internal fun CardEditorWorkspace(bitmap: Bitmap?, error: String?, loading: Boolean, title: String,
    onDismiss: () -> Unit, actions: @Composable ColumnScope.() -> Unit,
    controls: @Composable (CardEditorSection) -> Unit) {
    var section by rememberSaveable { mutableStateOf(CardEditorSection.LOOKS) }
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your lyric card", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                    Text(title, style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val paneWidth = maxWidth
                val paneHeight = maxHeight
                val wide = paneWidth > paneHeight
                if (wide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                            CardPreview(bitmap, error, loading, minOf(paneWidth * .38f, paneHeight * .80f))
                        }
                        CardControlPane(section, { section = it }, Modifier.weight(.6f).fillMaxHeight(), controls)
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CardPreview(bitmap, error, loading, minOf(paneWidth, paneHeight * .46f, 300.dp))
                        CardControlPane(section, { section = it }, Modifier.weight(1f).fillMaxWidth(), controls)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(SingalongColors.Surface).padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

@Composable
private fun CardControlPane(section: CardEditorSection, onSection: (CardEditorSection) -> Unit,
    modifier: Modifier, controls: @Composable (CardEditorSection) -> Unit) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().background(SingalongColors.Raised, RoundedCornerShape(18.dp)).padding(4.dp)
            .height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CardEditorSection.entries.forEach { item ->
                val active = section == item
                val color by animateColorAsState(if (active) SingalongColors.Purple else SingalongColors.Raised,
                    animationSpec = tween(160), label = "Editor section")
                Box(Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                    .background(color).selectable(active, role = Role.Tab, onClick = { onSection(item) })
                    .padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(item.label, color = if (active) SingalongColors.Background else SingalongColors.Muted,
                        style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        key(section) {
            if (section == CardEditorSection.LINES) {
                Box(Modifier.fillMaxWidth().weight(1f).clipToBounds().padding(top = 16.dp)) { controls(section) }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f).clipToBounds().verticalScroll(rememberScrollState())
                    .testTag("card-controls").padding(top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    controls(section)
                }
            }
        }
    }
}

@Composable
private fun CardPreview(bitmap: Bitmap?, error: String?, loading: Boolean, side: Dp) {
    Box(Modifier.size(side).clip(RoundedCornerShape(24.dp)).background(SingalongColors.Background)
        .testTag("card-preview"), contentAlignment = Alignment.Center) {
        // Keep the old frame visible while rendering; crossfade only finished images.
        Crossfade(bitmap, animationSpec = tween(180), label = "Card preview") { frame ->
            if (frame != null) Image(frame.asImageBitmap(), "Lyric card preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (error != null) Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = SingalongColors.Surface.copy(alpha = .96f)) {
            Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.TopEnd).padding(12.dp).size(20.dp), strokeWidth = 2.dp)
    }
}
