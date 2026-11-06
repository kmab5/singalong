package com.kmab.singalong.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kmab.singalong.cards.*
import com.kmab.singalong.ui.theme.SingalongColors
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardStyleSaver = Saver<CardStyle, String>(
    save = { CardStyleState.encode(it) },
    restore = { CardStyleState.decode(it) },
)

private data class CardImageState(val source: String, val bitmap: Bitmap?, val error: String? = null, val colors: CardPaletteColors? = null)

/** Equality ties a preview and an export to exactly the draft and decoded images they represent. */
private data class CardDraftFrame(
    val style: CardStyle,
    val selected: Set<Int>,
    val emphasized: Set<Int>,
    val cover: Bitmap?,
    val background: Bitmap?,
    val coverSource: String,
    val backgroundSource: String,
    val imagesReady: Boolean,
    val artworkColors: CardPaletteColors?,
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
    var emphasis by rememberSaveable(frozen.id) { mutableStateOf(emptyList<Int>()) }
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
            if (coverSource != "none" && bitmap == null) "Couldn't open the album cover. Choose another image or use the card without a cover." else null,
            withContext(Dispatchers.Default) { CardArtworkColors.extract(bitmap) })
    }
    LaunchedEffect(frozen.id, backgroundSource) {
        val bitmap = backgroundUri?.let { CardImages.load(context, Uri.parse(it)) }
        loadedBackground = CardImageState(backgroundSource, bitmap,
            if (backgroundUri != null && bitmap == null) "Couldn't open the background image. Choose another image or a color background." else null)
    }
    val imagesReady = loadedCover?.source == coverSource && loadedBackground?.source == backgroundSource
    val frame = CardDraftFrame(style.sanitized(), selection.toSet(), emphasis.toSet().intersect(selection.toSet()), loadedCover?.takeIf { it.source == coverSource }?.bitmap,
        loadedBackground?.takeIf { it.source == backgroundSource }?.bitmap, coverSource, backgroundSource, imagesReady,
        loadedCover?.takeIf { it.source == coverSource }?.colors)
    val latestFrame by rememberUpdatedState(frame)
    LaunchedEffect(frozen.id, frame, editing) {
        exportJob?.cancel()
        exporting = false
        actionMessage = null
        if (!editing || !frame.imagesReady) return@LaunchedEffect
        delay(100)
        val result = withContext(Dispatchers.Default) {
            renderer.render(frozen.content, frame.selected, frame.style, frame.cover, frame.background, width = 600, emphasized = frame.emphasized,
                artworkColors = frame.artworkColors)
        }
        // A cancelled render cannot publish its result. The previous bitmap stays alive while drawn.
        preview = result.bitmap
        previewError = result.error
        previewFrame = frame
    }

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
                    renderer.render(frozen.content, captured.selected, captured.style, captured.cover, captured.background, emphasized = captured.emphasized,
                        artworkColors = captured.artworkColors)
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
                    savePicker.launch(frozen.content.pngFileName(captured.selected))
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

    if (!editing) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
            containerColor = SingalongColors.Surface, contentColor = SingalongColors.Text) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Lyric line", style = MaterialTheme.typography.headlineSmall)
                Text(frozen.content.lines.getOrNull(frozen.content.initialLine)?.text.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Share lyric card") }
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
            Surface(Modifier.fillMaxSize(), color = SingalongColors.Surface, contentColor = SingalongColors.Text) {
                CardEditorWorkspace(preview, previewError, loading = !imagesReady || previewFrame != frame,
                    title = frozen.content.recording.title, onDismiss = onDismiss,
                    actions = {
                        actionMessage?.let { Text(it, color = SingalongColors.PurpleText, style = MaterialTheme.typography.bodySmall) }
                        if (exporting || saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { export(false) }, enabled = ready && !exporting && !saving,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Share PNG") }
                            ActionButton(onClick = { export(true) }, enabled = ready && !exporting && !saving,
                                modifier = Modifier.weight(1f)) { Text("Save PNG") }
                        }
                    }, controls = { section ->
                        CardEditorControls(section, frozen, style, selection, loadedCover?.error, loadedBackground?.error,
                            hasCover = frame.cover != null, onStyle = {
                                if (it.template != style.template) backgroundUri = null
                                style = it
                            }, onSelection = { selection = it },
                            onCover = { coverPicker.launch("image/*") }, onBackground = { backgroundPicker.launch("image/*") },
                            onResetCover = { coverUri = null }, onResetBackground = { backgroundUri = null; style = style.copy(background = style.template.style().background) },
                            customCover = coverUri != null, customBackground = backgroundUri != null,
                            emphasized = emphasis.toSet(), onEmphasis = { emphasis = if (it in emphasis) emphasis - it else emphasis + it },
                            artworkColors = frame.artworkColors)
                    })
            }
        }
    }
}
