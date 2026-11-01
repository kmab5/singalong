package com.kmab.singalong.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.settings.AppSettings
import com.kmab.singalong.ui.LyricsReader
import com.kmab.singalong.ui.theme.*
import kotlin.math.roundToInt

class OverlayWindowHost(private val context: Context, private val container: AppContainer) {
    private val windowContext = context.createWindowContext(
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null,
    )
    private val manager = windowContext.getSystemService(WindowManager::class.java)
    private val density get() = windowContext.resources.displayMetrics.density
    private var view: ComposeView? = null
    private var owner: WindowOwner? = null
    private var params: WindowManager.LayoutParams? = null
    private var dragging = false
    private var lastSettings = AppSettings()
    private var lastPanel = OverlayState()

    fun render(panel: OverlayState, settings: AppSettings, screenAllowed: Boolean): Boolean {
        lastPanel = panel; lastSettings = settings
        if (!screenAllowed || panel.mode == PanelMode.HIDDEN) { hide(); return true }
        if (!Settings.canDrawOverlays(context)) { hide(); return false }
        if (view == null && !attach()) return false
        if (!dragging) {
            val p = params ?: return false
            val safe = safeBounds()
            val minimized = panel.mode != PanelMode.VISIBLE
            p.width = if (minimized) (56 * density).roundToInt() else (settings.widthDp * density).roundToInt().coerceAtMost(safe.width())
            p.height = if (minimized) (56 * density).roundToInt() else ((if (panel.expanded) maxOf(settings.heightDp, 360f) else settings.heightDp) * density).roundToInt().coerceAtMost(safe.height())
            p.x = safe.left + ((safe.width() - p.width).coerceAtLeast(0) * settings.xFraction).roundToInt()
            if (minimized) p.x = if (settings.xFraction < 0.5f) safe.left else safe.right - p.width
            p.y = safe.top + ((safe.height() - p.height).coerceAtLeast(0) * settings.yFraction).roundToInt()
            return update()
        }
        return true
    }

    private fun safeBounds(): android.graphics.Rect {
        val metrics = manager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        return android.graphics.Rect(insets.left, insets.top,
            (metrics.bounds.width() - insets.right).coerceAtLeast(insets.left + 1),
            (metrics.bounds.height() - insets.bottom).coerceAtLeast(insets.top + 1))
    }

    private fun attach(): Boolean {
        val lifecycleOwner = WindowOwner()
        val composeView = ComposeView(windowContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        val layout = WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT; title = "Singalong lyrics"
            setFitInsetsTypes(0)
        }
        params = layout
        composeView.setContent {
            val panel by container.panel.collectAsState()
            val settings by container.settings.state.collectAsState()
            val reader by container.reader.state.collectAsState()
            SingalongTheme {
                Surface(Modifier.fillMaxSize(), color = SingalongColors.Surface.copy(alpha = settings.opacity), shape = RoundedCornerShape(16.dp)) {
                    if (panel.mode != PanelMode.VISIBLE) {
                        IconButton(onClick = container::showPanel, modifier = Modifier.fillMaxSize()) { Icon(Icons.Outlined.MusicNote, "Restore floating lyrics") }
                    } else Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f).height(48.dp).semantics {
                                contentDescription = "Move floating lyrics"
                                customActions = listOf(
                                    CustomAccessibilityAction("Move up") { move(0f, -48 * density); persist(); true },
                                    CustomAccessibilityAction("Move down") { move(0f, 48 * density); persist(); true },
                                    CustomAccessibilityAction("Move left") { move(-48 * density, 0f); persist(); true },
                                    CustomAccessibilityAction("Move right") { move(48 * density, 0f); persist(); true },
                                )
                            }.pointerInput(Unit) {
                                detectDragGestures(onDragStart = { dragging = true }, onDragEnd = { persist() }, onDragCancel = { persist() }) { change, drag ->
                                    change.consume(); move(drag.x, drag.y)
                                }
                            }.padding(12.dp)) { Icon(Icons.Outlined.DragHandle, null, tint = SingalongColors.Muted) }
                            IconButton(onClick = container::expandPanel) { Icon(if (panel.expanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore, if (panel.expanded) "Compact lyrics" else "Expand lyrics") }
                            IconButton(onClick = container::minimizePanel) { Icon(Icons.Outlined.Minimize, "Minimize lyrics") }
                            IconButton(onClick = container::closePanel) { Icon(Icons.Outlined.Close, "Close lyrics") }
                        }
                        LyricsReader(reader, Modifier.weight(1f), compact = !panel.expanded, textSize = settings.textSizeSp,
                            onFollow = container.reader::follow, onRetry = container.reader::request)
                        Row(Modifier.fillMaxWidth()) {
                            Text(reader.version?.sourceName ?: "singalong", Modifier.weight(1f).padding(12.dp), style = MaterialTheme.typography.labelSmall, color = SingalongColors.Muted)
                            Box(Modifier.size(48.dp).semantics {
                                contentDescription = "Resize floating lyrics"
                                customActions = listOf(
                                    CustomAccessibilityAction("Make larger") { resize(24 * density, 24 * density); persist(); true },
                                    CustomAccessibilityAction("Make smaller") { resize(-24 * density, -24 * density); persist(); true },
                                )
                            }.pointerInput(Unit) {
                                detectDragGestures(onDragStart = { dragging = true }, onDragEnd = { persist() }, onDragCancel = { persist() }) { change, drag -> change.consume(); resize(drag.x, drag.y) }
                            }.padding(12.dp)) { Icon(Icons.Outlined.OpenInFull, null, tint = SingalongColors.Muted) }
                        }
                    }
                }
            }
        }
        return try {
            manager.addView(composeView, layout)
            view = composeView; owner = lifecycleOwner; lifecycleOwner.resume(); container.overlayVisible(true); true
        } catch (_: WindowManager.BadTokenException) {
            composeView.disposeComposition(); lifecycleOwner.destroy(); params = null; false
        } catch (_: SecurityException) {
            composeView.disposeComposition(); lifecycleOwner.destroy(); params = null; false
        }
    }

    private fun move(dx: Float, dy: Float) {
        params?.let { p -> val safe = safeBounds()
            p.x = (p.x + dx.roundToInt()).coerceIn(safe.left, maxOf(safe.left, safe.right - p.width))
            p.y = (p.y + dy.roundToInt()).coerceIn(safe.top, maxOf(safe.top, safe.bottom - p.height))
            update()
        }
    }
    private fun resize(dx: Float, dy: Float) {
        params?.let { p -> val safe = safeBounds()
            p.width = (p.width + dx.roundToInt()).coerceIn(minOf((260 * density).roundToInt(), safe.width()), minOf((560 * density).roundToInt(), safe.width()))
            p.height = (p.height + dy.roundToInt()).coerceIn(minOf((180 * density).roundToInt(), safe.height()), minOf((640 * density).roundToInt(), safe.height()))
            move(0f, 0f)
        }
    }
    private fun persist() {
        dragging = false
        params?.let { p -> val safe = safeBounds()
            container.geometry((p.x - safe.left).toFloat() / maxOf(1, safe.width() - p.width),
                (p.y - safe.top).toFloat() / maxOf(1, safe.height() - p.height), p.width / density, p.height / density)
        }
    }
    private fun update(): Boolean {
        val current = view ?: return false
        return try { if (current.isAttachedToWindow) manager.updateViewLayout(current, params); true }
        catch (_: SecurityException) { hide(); false }
        catch (_: WindowManager.BadTokenException) { hide(); false }
    }
    fun configurationChanged() { render(lastPanel, lastSettings, view != null) }
    fun hide() {
        view?.let { if (it.isAttachedToWindow) manager.removeViewImmediate(it); it.disposeComposition() }
        owner?.destroy(); owner = null; view = null; params = null; dragging = false
        container.overlayVisible(false)
    }
    private class WindowOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry get() = saved.savedStateRegistry
        init { saved.performAttach(); saved.performRestore(null); registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE) }
        fun resume() { registry.handleLifecycleEvent(Lifecycle.Event.ON_START); registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
