package com.kmab.singalong.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
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
    Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                        CardPreview(bitmap, loading, minOf(paneWidth * .38f, paneHeight * .72f))
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    CardControlPane(section, { section = it }, Modifier.weight(.6f).fillMaxHeight(), controls)
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardPreview(bitmap, loading, minOf(paneWidth, paneHeight * .45f, 280.dp))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    CardControlPane(section, { section = it }, Modifier.weight(1f).fillMaxWidth(), controls)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = actions)
    }
}

@Composable
private fun CardControlPane(section: CardEditorSection, onSection: (CardEditorSection) -> Unit,
    modifier: Modifier, controls: @Composable (CardEditorSection) -> Unit) {
    Column(modifier) {
        PrimaryScrollableTabRow(selectedTabIndex = section.ordinal, edgePadding = 0.dp, divider = {}) {
            CardEditorSection.entries.forEach { item ->
                Tab(selected = section == item, onClick = { onSection(item) }, text = { Text(item.label) })
            }
        }
        key(section) {
            if (section == CardEditorSection.LINES) {
                Box(Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)) { controls(section) }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .testTag("card-controls").padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    controls(section)
                }
            }
        }
    }
}

@Composable
private fun CardPreview(bitmap: Bitmap?, loading: Boolean, side: Dp) {
    Box(Modifier.size(side).clip(RoundedCornerShape(24.dp)).background(SingalongColors.Background)
        .testTag("card-preview"), contentAlignment = Alignment.Center) {
        // Keep the old frame visible while rendering; crossfade only finished images.
        Crossfade(bitmap, animationSpec = tween(180), label = "Card preview") { frame ->
            if (frame != null) Image(frame.asImageBitmap(), "Lyric card preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.TopEnd).padding(12.dp).size(20.dp), strokeWidth = 2.dp)
    }
}
