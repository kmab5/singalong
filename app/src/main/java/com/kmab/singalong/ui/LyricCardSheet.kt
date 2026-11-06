package com.kmab.singalong.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.singalong.cards.*
import com.kmab.singalong.ui.theme.SingalongColors
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val CardStyleSaver = Saver<CardStyle, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString<CardStyle>(it).sanitized() },
)

private data class CardImageState(val source: String, val bitmap: Bitmap?, val error: String? = null)

/** Equality ties a preview and an export to exactly the draft and decoded images they represent. */
private data class CardDraftFrame(
    val style: CardStyle,
    val selected: Set<Int>,
    val cover: Bitmap?,
    val background: Bitmap?,
    val coverSource: String,
    val backgroundSource: String,
    val imagesReady: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricCardSheet(request: CardRequest, onDismiss: () -> Unit) {
    // Playback may continue behind this sheet. Only a new request starts a new draft.
    val frozen = remember(request.id) { request }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val renderer = remember(context) { LyricCardRenderer(context) }
    var editing by rememberSaveable(frozen.id) { mutableStateOf(false) }
    var style by rememberSaveable(frozen.id, stateSaver = CardStyleSaver) { mutableStateOf(CardStyle()) }
    var selection by rememberSaveable(frozen.id) { mutableStateOf(listOf(frozen.content.initialLine)) }
    var coverUri by rememberSaveable(frozen.id) { mutableStateOf<String?>(null) }
    var backgroundUri by rememberSaveable(frozen.id) { mutableStateOf<String?>(null) }
    var pendingSave by rememberSaveable(frozen.id) { mutableStateOf<String?>(null) }
    var loadedCover by remember(frozen.id) { mutableStateOf<CardImageState?>(null) }
    var loadedBackground by remember(frozen.id) { mutableStateOf<CardImageState?>(null) }
    var preview by remember(frozen.id) { mutableStateOf<Bitmap?>(null) }
    var previewFrame by remember(frozen.id) { mutableStateOf<CardDraftFrame?>(null) }
    var previewError by remember(frozen.id) { mutableStateOf<String?>(null) }
    var actionMessage by remember(frozen.id) { mutableStateOf<String?>(null) }
    var exporting by remember(frozen.id) { mutableStateOf(false) }
    var saving by remember(frozen.id) { mutableStateOf(false) }
    var exportJob by remember(frozen.id) { mutableStateOf<Job?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = editing)

    val coverSource = coverUri ?: when {
        frozen.cover != null -> "snapshot"
        else -> "none"
    }
    val backgroundSource = backgroundUri ?: "none"
    LaunchedEffect(frozen.id, coverSource) {
        val bitmap = when {
            coverUri != null -> CardImages.load(context, Uri.parse(coverUri))
            frozen.cover != null -> frozen.cover
            else -> null
        }
        loadedCover = CardImageState(coverSource, bitmap,
            if (coverSource != "none" && bitmap == null) "Couldn't open the album cover. Choose another image or use the card without a cover." else null)
    }
    LaunchedEffect(frozen.id, backgroundSource) {
        val bitmap = backgroundUri?.let { CardImages.load(context, Uri.parse(it)) }
        loadedBackground = CardImageState(backgroundSource, bitmap,
            if (backgroundUri != null && bitmap == null) "Couldn't open the background image. Choose another image or a color background." else null)
    }
    val imagesReady = loadedCover?.source == coverSource && loadedBackground?.source == backgroundSource
    val frame = CardDraftFrame(style.sanitized(), selection.toSet(), loadedCover?.takeIf { it.source == coverSource }?.bitmap,
        loadedBackground?.takeIf { it.source == backgroundSource }?.bitmap, coverSource, backgroundSource, imagesReady)
    val latestFrame by rememberUpdatedState(frame)
    LaunchedEffect(frozen.id, frame, editing) {
        exportJob?.cancel()
        exporting = false
        previewError = null
        actionMessage = null
        if (!editing || !frame.imagesReady) return@LaunchedEffect
        delay(100)
        val result = withContext(Dispatchers.Default) {
            renderer.render(frozen.content, frame.selected, frame.style, frame.cover, frame.background, width = 600)
        }
        // A cancelled render cannot publish its result. The previous bitmap stays alive while drawn.
        preview = result.bitmap
        previewError = result.error
        previewFrame = frame
    }
    LaunchedEffect(editing) { if (editing) sheetState.expand() }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) coverUri = uri.toString()
    }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            backgroundUri = uri.toString()
            style = style.copy(background = CardBackground.IMAGE)
        }
    }
    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { target ->
        val source = pendingSave
        pendingSave = null
        if (target != null && source != null) {
            scope.launch {
                saving = true
                try {
                    CardExport.save(context, Uri.parse(source), target)
                    actionMessage = "Card saved."
                } catch (_: IOException) {
                    actionMessage = "Couldn't save the card. Choose a different location and try again."
                } catch (_: SecurityException) {
                    actionMessage = "Access to that location was lost. Choose a location again."
                } finally {
                    saving = false
                }
            }
        }
    }
    val ready = editing && imagesReady && previewFrame == frame && preview != null && previewError == null
    fun export(save: Boolean) {
        if (!ready || exporting || saving) return
        val captured = frame
        exporting = true
        actionMessage = null
        exportJob = scope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    renderer.render(frozen.content, captured.selected, captured.style, captured.cover, captured.background)
                }
                if (latestFrame != captured) return@launch
                val bitmap = result.bitmap
                if (bitmap == null || result.error != null) {
                    actionMessage = result.error ?: "Couldn't create this card. Try a shorter excerpt."
                    return@launch
                }
                val uri = try { CardExport.prepare(context, bitmap) } finally { bitmap.recycle() }
                if (latestFrame != captured) return@launch
                if (save) {
                    pendingSave = uri.toString()
                    savePicker.launch("Singalong-lyric-card.png")
                } else if (!CardExport.share(context, uri)) {
                    actionMessage = "No app could share this image. You can save the PNG instead."
                }
            } catch (_: IOException) {
                actionMessage = "Couldn't create the image file. Try again."
            } catch (_: SecurityException) {
                actionMessage = "Couldn't access the image file. Choose the image again and retry."
            } finally {
                exporting = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = SingalongColors.Surface, contentColor = SingalongColors.Text) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (editing) "Your lyric card" else "Lyric line", Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
            }
            Text(frozen.content.recording.title, style = MaterialTheme.typography.titleMedium)
            if (!editing) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text(frozen.content.lines.getOrNull(frozen.content.initialLine)?.text.orEmpty(),
                        Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
                }
                Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Share lyric card")
                }
            } else {
                CardPreview(preview, frame.style.aspect, previewError,
                    loading = !imagesReady || previewFrame != frame)
                actionMessage?.let { Text(it, color = SingalongColors.PurpleText) }
                if (exporting || saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { export(false) }, enabled = ready && !exporting && !saving,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Share PNG") }
                    ActionButton(onClick = { export(true) }, enabled = ready && !exporting && !saving,
                        modifier = Modifier.weight(1f)) { Text("Save PNG") }
                }
                Text("Song details, source credit and Singalong · kmab stay on every card.",
                    color = SingalongColors.Muted, style = MaterialTheme.typography.bodySmall)
                CardEditorControls(frozen, style, selection, loadedCover?.error, loadedBackground?.error,
                    hasCover = frame.cover != null, hasBackground = frame.background != null,
                    onStyle = { style = it }, onSelection = { selection = it },
                    onCover = { coverPicker.launch("image/*") }, onBackground = { backgroundPicker.launch("image/*") },
                    onResetCover = { coverUri = null }, onResetBackground = { backgroundUri = null },
                    customCover = coverUri != null, customBackground = backgroundUri != null)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CardPreview(bitmap: Bitmap?, aspect: CardAspect, error: String?, loading: Boolean) {
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxHeight = with(LocalDensity.current) { (windowHeight.toDp() * .38f).coerceIn(128.dp, 340.dp) }
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Live preview", style = MaterialTheme.typography.labelLarge)
                Text("${aspect.width} × ${aspect.height}", style = MaterialTheme.typography.labelMedium,
                    color = SingalongColors.Muted)
            }
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), "Lyric card preview", Modifier.fillMaxWidth().height(maxHeight),
                    contentScale = ContentScale.Fit)
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Updating preview…", style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
                }
            } else if (loading) {
                Box(Modifier.fillMaxWidth().height(maxHeight), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                        Text("Updating preview…", color = SingalongColors.Muted)
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CardEditorControls(
    request: CardRequest, style: CardStyle, selected: List<Int>, coverError: String?, backgroundError: String?,
    hasCover: Boolean, hasBackground: Boolean, onStyle: (CardStyle) -> Unit, onSelection: (List<Int>) -> Unit,
    onCover: () -> Unit, onBackground: () -> Unit, onResetCover: () -> Unit, onResetBackground: () -> Unit,
    customCover: Boolean, customBackground: Boolean,
) {
    CardControlGroup("Templates") {
        Text("Choose a starting look, then make it yours.", color = SingalongColors.Muted,
            style = MaterialTheme.typography.bodySmall)
        CardChoices(CardTemplate.entries, style.template, { it.label }) { onStyle(it.style()) }
    }
    CardControlGroup("Lyric excerpt") {
        Text("${selected.size} selected · original wording and order", style = MaterialTheme.typography.bodySmall,
            color = SingalongColors.Muted)
        val listState = rememberLazyListState(initialFirstVisibleItemIndex =
            (request.content.initialLine - 2).coerceAtLeast(0))
        LazyColumn(Modifier.fillMaxWidth().height(224.dp), state = listState) {
            itemsIndexed(request.content.lines, key = { index, _ -> index }) { index, line ->
                val checked = index in selected
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(value = checked, role = Role.Checkbox, onValueChange = {
                        onSelection(if (checked) selected - index else selected + index)
                    }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, onCheckedChange = null)
                    Text(line.text.ifEmpty { "(Empty line)" }, Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text("Scroll the lines to choose another part of this song.", color = SingalongColors.Muted,
            style = MaterialTheme.typography.bodySmall)
    }
    CardControlGroup("Format") {
        CardChoices(CardAspect.entries, style.aspect, { it.label }) { onStyle(style.copy(aspect = it)) }
    }
    CardControlGroup("Type & color") {
        CardChoices(CardFont.entries, style.font, { it.label }) { onStyle(style.copy(font = it)) }
        CardSlider("Text size", style.fontSize, 24f..112f, "${style.fontSize.toInt()}") { onStyle(style.copy(fontSize = it)) }
        CardChoices(CardAlignment.entries, style.alignment, {
            when (it) { CardAlignment.START -> "Start"; CardAlignment.CENTER -> "Center"; CardAlignment.END -> "End" }
        }) { onStyle(style.copy(alignment = it)) }
        CardColorControl("Text color", style.textColor) { onStyle(style.copy(textColor = it)) }
        CardColorControl("Accent color", style.accentColor) { onStyle(style.copy(accentColor = it)) }
    }
    CardControlGroup("Lyric position") {
        CardSlider("Horizontal position", style.textX, 0f..1f) { onStyle(style.copy(textX = it)) }
        CardSlider("Vertical position", style.textY, 0f..1f) { onStyle(style.copy(textY = it)) }
        CardSlider("Text width", style.textWidth, 0.4f..0.88f) { onStyle(style.copy(textWidth = it)) }
    }
    CardControlGroup("Album cover") {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(style.showCover, role = Role.Switch,
            onValueChange = { onStyle(style.copy(showCover = it)) }), verticalAlignment = Alignment.CenterVertically) {
            Text("Show album cover", Modifier.weight(1f))
            Switch(style.showCover, onCheckedChange = null)
        }
        coverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!hasCover && coverError == null) Text("No cover available for this song. Choose an image to add one.",
            color = SingalongColors.Muted, style = MaterialTheme.typography.bodySmall)
        ActionButton(onClick = onCover, modifier = Modifier.fillMaxWidth()) { Text("Choose album cover") }
        if (customCover) TextButton(onClick = onResetCover, modifier = Modifier.heightIn(min = 48.dp)) { Text("Use original cover") }
        if (style.showCover) {
            CardSlider("Cover size", style.coverSize, 0.1f..0.4f) { onStyle(style.copy(coverSize = it)) }
            CardSlider("Cover horizontal position", style.coverX, 0f..1f) { onStyle(style.copy(coverX = it)) }
            CardSlider("Cover vertical position", style.coverY, 0f..1f) { onStyle(style.copy(coverY = it)) }
        }
    }
    CardControlGroup("Background") {
        CardChoices(CardBackground.entries, style.background, {
            when (it) { CardBackground.SOLID -> "Solid"; CardBackground.GRADIENT -> "Gradient"; CardBackground.IMAGE -> "Image" }
        }) { onStyle(style.copy(background = it)) }
        CardColorControl("Background color", style.backgroundColor) { onStyle(style.copy(backgroundColor = it)) }
        if (style.background == CardBackground.IMAGE) {
            backgroundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!hasBackground) Text(if (hasCover) "Using the album cover as the background." else "Choose a background image. The background color is used until then.",
                style = MaterialTheme.typography.bodySmall, color = SingalongColors.Muted)
            ActionButton(onClick = onBackground, modifier = Modifier.fillMaxWidth()) { Text("Choose background image") }
            if (customBackground) TextButton(onClick = onResetBackground, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove background image") }
            CardSlider("Image shade", style.imageShade, 0f..0.9f) { onStyle(style.copy(imageShade = it)) }
        }
    }
}

@Composable
private fun CardControlGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> CardChoices(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(selected == option, onClick = { onSelect(option) }, label = { Text(label(option)) },
                modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
private fun CardSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    valueLabel: String = "${(value * 100).toInt()}%", onValue: (Float) -> Unit) {
    Column {
        Text("$label · $valueLabel", style = MaterialTheme.typography.bodyMedium)
        Slider(value, onValueChange = onValue, valueRange = range,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = label })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardColorControl(label: String, color: Int, onColor: (Int) -> Unit) {
    var hex by rememberSaveable(color) { mutableStateOf(String.format(Locale.ROOT, "#%06X", color and 0xffffff)) }
    val valid = hex.matches(Regex("#[0-9a-fA-F]{6}"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = hex, onValueChange = { value ->
            hex = value
            if (value.matches(Regex("#[0-9a-fA-F]{6}"))) onColor((0xff000000L or value.drop(1).toLong(16)).toInt())
        }, label = { Text(label) }, singleLine = true, isError = !valid,
            supportingText = { Text(if (valid) "Hex color · #RRGGBB" else "Enter # and six color digits, for example #B24BFF.") },
            modifier = Modifier.fillMaxWidth())
        val swatches = listOf(0xff17121f.toInt(), 0xffedebe4.toInt(), 0xffb24bff.toInt(),
            0xff71d9db.toInt(), 0xfff6b6cc.toInt(), 0xff22202a.toInt())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.forEach { swatch ->
                val name = String.format(Locale.ROOT, "#%06X", swatch and 0xffffff)
                OutlinedButton(onClick = { onColor(swatch) }, modifier = Modifier.size(48.dp)
                    .semantics { contentDescription = "$label $name${if (swatch == color) ", selected" else ""}" },
                    shape = CircleShape, contentPadding = PaddingValues(10.dp),
                    border = BorderStroke(if (swatch == color) 2.dp else 1.dp,
                        if (swatch == color) SingalongColors.PurpleText else SingalongColors.Line)) {
                    Box(Modifier.size(24.dp).background(Color(swatch), CircleShape))
                }
            }
        }
    }
}
