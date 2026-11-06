package com.kmab.singalong.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kmab.singalong.cards.*
import com.kmab.singalong.ui.theme.SingalongColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CardEditorControls(section: CardEditorSection, request: CardRequest, style: CardStyle, selected: List<Int>,
    coverError: String?, backgroundError: String?, hasCover: Boolean, onStyle: (CardStyle) -> Unit,
    onSelection: (List<Int>) -> Unit, onCover: () -> Unit, onBackground: () -> Unit,
    onResetCover: () -> Unit, onResetBackground: () -> Unit, customCover: Boolean, customBackground: Boolean) {
    when (section) {
        CardEditorSection.LOOKS -> {
            TemplateChoices(request, selected.toSet(), style.template) { onStyle(style.withTemplate(it)) }
            ControlTitle("Palette")
            PaletteChoices(style.palette) { onStyle(style.copy(palette = it)) }
            ControlTitle("Format")
            CardChoices(CardAspect.entries, style.aspect, { it.label }) { onStyle(style.copy(aspect = it)) }
            ActionButton(onClick = onBackground, modifier = Modifier.fillMaxWidth()) { Text("Choose background photo") }
            if (customBackground) ActionButton(onClick = onResetBackground, modifier = Modifier.fillMaxWidth()) { Text("Use preset background") }
            backgroundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        CardEditorSection.TEXT -> {
            ControlTitle("Font")
            FontChoices(style.font) { onStyle(style.copy(font = it)) }
            ControlTitle("Size")
            CardChoices(CardTextSize.entries, style.textSize, { it.label }) { onStyle(style.copy(textSize = it)) }
            ControlTitle("Alignment")
            CardChoices(CardAlignment.entries, style.alignment, { when (it) {
                CardAlignment.START -> "Start"; CardAlignment.CENTER -> "Center"; CardAlignment.END -> "End"
            } }) { onStyle(style.copy(alignment = it)) }
        }
        CardEditorSection.DETAILS -> {
            Text("Keep only the details you want on your card.", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
            val recording = request.content.recording
            DetailToggle("Song title", style.details.title, recording.title.isNotBlank()) { onStyle(style.copy(details = style.details.copy(title = it))) }
            DetailToggle("Artist", style.details.artist, recording.artists.any { it.isNotBlank() }) { onStyle(style.copy(details = style.details.copy(artist = it))) }
            DetailToggle("Album", style.details.album, !recording.album.isNullOrBlank()) { onStyle(style.copy(details = style.details.copy(album = it))) }
            DetailToggle("Filename", style.details.fileName, request.content.file.name != null) { onStyle(style.copy(details = style.details.copy(fileName = it))) }
            DetailToggle("File location", style.details.location, request.content.file.location != null) { onStyle(style.copy(details = style.details.copy(location = it))) }
            DetailToggle("Album cover", style.showCover, hasCover) { onStyle(style.copy(showCover = it)) }
            DetailToggle("App name", style.showAppName) { onStyle(style.copy(showAppName = it)) }
            Text("The Singalong icon stays on the card. File location is the audio folder, not your GPS location.",
                style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
            coverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ActionButton(onClick = onCover, modifier = Modifier.fillMaxWidth()) { Text("Choose album cover") }
            if (customCover) ActionButton(onClick = onResetCover, modifier = Modifier.fillMaxWidth()) { Text("Use original cover") }
        }
        CardEditorSection.LINES -> {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (request.content.initialLine - 2).coerceAtLeast(0))
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${selected.size} selected · original wording and order", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("card-lines"), state = listState) {
                    itemsIndexed(request.content.lines, key = { index, _ -> index }) { index, line ->
                        val checked = index in selected
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox,
                            onValueChange = { onSelection(if (checked) selected - index else selected + index) })
                            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked, onCheckedChange = null)
                            Text(line.text.ifEmpty { "(Empty line)" }, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlTitle(text: String) = Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)

@Composable
private fun DetailToggle(label: String, checked: Boolean, available: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked && available, enabled = available,
        role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (available) SingalongColors.Text else SingalongColors.Muted)
            if (!available) Text("Not available for this song", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
        }
        Switch(checked && available, onCheckedChange = null, enabled = available)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> CardChoices(options: List<T>, value: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(value == option, onClick = { onSelect(option) },
            label = { Text(label(option)) }, modifier = Modifier.heightIn(min = 48.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontChoices(value: CardFont, onSelect: (CardFont) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CardFont.entries.forEach { font ->
            FilterChip(value == font, onClick = { onSelect(font) }, modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(font.label, fontFamily = remember(font) { FontFamily(Font(font.resource)) }) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteChoices(value: CardPalette, onSelect: (CardPalette) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CardPalette.entries.forEach { palette ->
            FilterChip(value == palette, onClick = { onSelect(palette) }, modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(palette.label) }, leadingIcon = {
                    Row { listOf(palette.base, palette.ink, palette.accent).forEach { color ->
                        Box(Modifier.size(12.dp).background(Color(color), CircleShape))
                    } }
                })
        }
    }
}

@Composable
private fun TemplateChoices(request: CardRequest, selected: Set<Int>, current: CardTemplate, onSelect: (CardTemplate) -> Unit) {
    val context = LocalContext.current
    val renderer = remember(context) { LyricCardRenderer(context) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(CardTemplate.entries, key = { it.name }) { template ->
            val preview by produceState<CardRenderResult?>(null, request.id, selected, template) {
                value = withContext(Dispatchers.Default) {
                    renderer.render(request.content, selected, template.style(), request.cover, width = 180)
                }
            }
            Surface(onClick = { onSelect(template) }, shape = RoundedCornerShape(16.dp),
                color = Color(template.style().palette.base),
                border = BorderStroke(if (current == template) 2.dp else 1.dp,
                    if (current == template) SingalongColors.PurpleText else SingalongColors.Line),
                modifier = Modifier.width(136.dp).semantics { this.selected = current == template }) {
                Column(Modifier.padding(10.dp)) {
                    val bitmap = preview?.bitmap
                    if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(1f))
                    else Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Text("Aa", fontFamily = FontFamily(Font(template.style().font.resource)), color = Color(template.style().palette.ink), style = MaterialTheme.typography.headlineLarge)
                    }
                    Text(template.label, color = Color(template.style().palette.ink), style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
    Text(current.description, style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
}
