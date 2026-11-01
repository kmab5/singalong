package com.kmab.singalong.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Gravity
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kmab.singalong.ui.theme.SingalongTheme
import kotlin.math.roundToInt

/** P0 window feasibility. Full overlay state machine is introduced in P3. */
class OverlayWindowHost(private val context: Context, private val onVisibility: (Boolean) -> Unit) {
    // A Service is not a visual context. Window metrics and Compose must use the
    // overlay's own display/window configuration, especially on Android 12+.
    private val windowContext = context.createWindowContext(
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        null,
    )
    private val manager = windowContext.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private var owner: WindowOwner? = null

    fun show(onClose: () -> Unit): Boolean {
        if (view != null) return true
        if (!Settings.canDrawOverlays(context)) return false
        val density = windowContext.resources.displayMetrics.density
        val bounds = manager.currentWindowMetrics.bounds
        val params = WindowManager.LayoutParams(
            minOf((344 * density).roundToInt(), bounds.width() - (24 * density).roundToInt()),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = (12 * density).roundToInt()
            y = (120 * density).roundToInt()
            title = "Singalong lyrics preview"
        }
        val lifecycleOwner = WindowOwner()
        val composeView = ComposeView(windowContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        fun move(dx: Float, dy: Float) {
            val current = manager.currentWindowMetrics.bounds
            params.x = (params.x + dx.roundToInt()).coerceIn(0, (current.width() - params.width).coerceAtLeast(0))
            params.y = (params.y + dy.roundToInt()).coerceIn(0, (current.height() - composeView.height).coerceAtLeast(0))
            if (composeView.isAttachedToWindow) manager.updateViewLayout(composeView, params)
        }
        composeView.setContent {
            SingalongTheme {
                PreviewLyricsPanel(
                    bilingual = true,
                    onClose = onClose,
                    dragModifier = Modifier
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Move panel up") { move(0f, -48 * density); true },
                                CustomAccessibilityAction("Move panel down") { move(0f, 48 * density); true },
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag -> change.consume(); move(drag.x, drag.y) }
                        },
                )
            }
        }
        return try {
            manager.addView(composeView, params)
            view = composeView
            owner = lifecycleOwner
            lifecycleOwner.resume()
            onVisibility(true)
            true
        } catch (_: WindowManager.BadTokenException) {
            composeView.disposeComposition()
            lifecycleOwner.destroy()
            false
        } catch (_: SecurityException) {
            composeView.disposeComposition()
            lifecycleOwner.destroy()
            false
        }
    }

    fun hide() {
        view?.let {
            if (it.isAttachedToWindow) manager.removeViewImmediate(it)
            it.disposeComposition()
        }
        owner?.destroy()
        owner = null
        view = null
        onVisibility(false)
    }

    private class WindowOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry get() = saved.savedStateRegistry
        init {
            saved.performAttach()
            saved.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        fun resume() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
