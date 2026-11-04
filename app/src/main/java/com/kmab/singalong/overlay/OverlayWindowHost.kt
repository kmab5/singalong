package com.kmab.singalong.overlay

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.*
import android.view.animation.PathInterpolator
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.*
import com.kmab.singalong.R
import com.kmab.singalong.app.AppContainer
import com.kmab.singalong.settings.AppSettings
import com.kmab.singalong.ui.theme.SingalongTheme
import java.util.function.Consumer
import kotlin.math.roundToInt

/** One Window owns its initial size, blur listener, animations and composition. */
class OverlayWindowHost(private val context: Context, private val container: AppContainer) {
    private val windowContext = context.createWindowContext(
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    private val manager = windowContext.getSystemService(WindowManager::class.java)
    private val density get() = windowContext.resources.displayMetrics.density
    private var dialog: Dialog? = null
    private var view: ComposeView? = null
    private var owner: WindowOwner? = null
    private var animator: ValueAnimator? = null
    private var dragging = false
    private var closing = false
    private var blurRegistered = false
    private var blurEnabled by mutableStateOf(false)
    private var lastSettings = AppSettings()
    private var lastPanel = OverlayState()
    private var lastScreenAllowed = false
    private val blurListener = Consumer<Boolean> { enabled -> blurEnabled = enabled; updateBlur() }

    fun render(panel: OverlayState, settings: AppSettings, screenAllowed: Boolean): Boolean {
        lastPanel = panel; lastSettings = settings
        lastScreenAllowed = screenAllowed
        if (!screenAllowed) { hide(); return true }
        if (panel.mode == PanelMode.HIDDEN) { dismissAnimated(); return true }
        if (!Settings.canDrawOverlays(context)) { hide(); return false }
        if (dialog != null && dialog?.isShowing != true) hide()
        if (closing) { dialog?.window?.decorView?.apply { animate().withEndAction(null).cancel(); alpha = 1f }; closing = false }
        val target = OverlayGeometry.bounds(panel, settings, safeBounds(), density)
        if (dialog == null) return attach(target)
        updateBlur()
        if (!dragging) animateTo(target)
        return true
    }

    private fun safeBounds(): PanelBounds {
        val metrics = manager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        return PanelBounds(insets.left, insets.top, (metrics.bounds.width() - insets.left - insets.right).coerceAtLeast(1),
            (metrics.bounds.height() - insets.top - insets.bottom).coerceAtLeast(1))
    }

    @android.annotation.SuppressLint("RtlHardcoded")
    private fun attach(bounds: PanelBounds): Boolean {
        val lifecycleOwner = WindowOwner()
        val composeView = ComposeView(windowContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        val floating = Dialog(windowContext, R.style.Theme_Singalong_Overlay)
        val window = requireNotNull(floating.window)
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = window.attributes.apply {
            gravity = Gravity.TOP or Gravity.LEFT; title = "Singalong lyrics"
            width = bounds.width; height = bounds.height; x = bounds.x; y = bounds.y
            windowAnimations = 0; setFitInsetsTypes(0)
        }
        floating.setCancelable(false)
        floating.setContentView(composeView)
        composeView.setContent {
            val panel by container.panel.collectAsState()
            val settings by container.settings.state.collectAsState()
            val reader by container.reader.state.collectAsState()
            SingalongTheme {
                FloatingLyricsPanel(panel, settings, reader,
                    onOpen = container::showPanel, onDock = container::minimizePanel, onClose = container::closePanel,
                    onExpand = container::expandPanel, onFollow = container.reader::follow, onRetry = container.reader::request,
                    onMinimal = { container.minimalPanel(true) }, onReveal = container::revealPanelControls,
                    onSync = container.reader::toggleSync,
                    onOpenApp = { context.startActivity(android.content.Intent(context, com.kmab.singalong.app.MainActivity::class.java)
                        .setAction(com.kmab.singalong.app.MainActivity.ACTION_OPEN_NOW)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)) },
                    onDragStart = { animator?.cancel(); dragging = true }, onMove = ::move, onDragEnd = ::persist)
            }
        }
        dialog = floating; owner = lifecycleOwner; view = composeView
        return try {
            // Full bounds are installed BEFORE show/addView. There is no 1px attachment race.
            floating.show()
            lifecycleOwner.resume()
            manager.addCrossWindowBlurEnabledListener(blurListener); blurRegistered = true
            updateBlur()
            if (ValueAnimator.areAnimatorsEnabled()) window.decorView.apply { alpha = 0f; animate().alpha(1f).setDuration(180).start() }
            container.overlayVisible(true)
            true
        } catch (_: WindowManager.BadTokenException) { hide(); true }
        catch (_: SecurityException) { hide(); Settings.canDrawOverlays(context) }
    }

    private fun updateBlur() {
        val window = dialog?.window ?: return
        val docked = lastPanel.mode != PanelMode.VISIBLE
        window.setBackgroundDrawable(GradientDrawable().apply {
            cornerRadius = (if (docked) 16f else 28f) * density
            setColor(android.graphics.Color.argb(1, 0, 0, 0))
        })
        window.setBackgroundBlurRadius(if (blurEnabled && !docked) (36 * density).roundToInt() else 0)
    }

    private fun animateTo(target: PanelBounds) {
        val p = dialog?.window?.attributes ?: return
        val from = PanelBounds(p.x, p.y, p.width, p.height)
        if (from == target) return
        animator?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) { applyBounds(target); return }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280; interpolator = PathInterpolator(.2f, 0f, 0f, 1f)
            addUpdateListener {
                val progress = it.animatedValue as Float
                fun lerp(a: Int, b: Int) = (a + (b - a) * progress).roundToInt()
                applyBounds(PanelBounds(lerp(from.x, target.x), lerp(from.y, target.y), lerp(from.width, target.width), lerp(from.height, target.height)))
            }
            start()
        }
    }
    private fun applyBounds(bounds: PanelBounds) {
        val window = dialog?.window ?: return
        try { window.attributes = window.attributes.apply { x = bounds.x; y = bounds.y; width = bounds.width; height = bounds.height } }
        catch (_: SecurityException) { hide(); if (!Settings.canDrawOverlays(context)) container.closePanel() }
        catch (_: WindowManager.BadTokenException) { hide() }
    }
    private fun move(dx: Float, dy: Float) {
        val p = dialog?.window?.attributes ?: return
        val safe = safeBounds()
        val x = (p.x + dx.roundToInt()).coerceIn(safe.x, maxOf(safe.x, safe.x + safe.width - p.width))
        val y = (p.y + dy.roundToInt()).coerceIn(safe.y, maxOf(safe.y, safe.y + safe.height - p.height))
        applyBounds(PanelBounds(x, y, p.width, p.height))
    }
    private fun persist() {
        dragging = false
        val p = dialog?.window?.attributes ?: return
        val safe = safeBounds()
        // A docked handle never overwrites the full panel's saved dimensions.
        container.geometry((p.x - safe.x).toFloat() / maxOf(1, safe.width - p.width),
            (p.y - safe.y).toFloat() / maxOf(1, safe.height - p.height), lastSettings.widthDp, lastSettings.heightDp)
    }
    fun configurationChanged() { render(lastPanel, lastSettings, lastScreenAllowed) }
    private fun dismissAnimated() {
        val decor = dialog?.window?.decorView ?: return
        if (closing) return
        if (!ValueAnimator.areAnimatorsEnabled()) { hide(); return }
        closing = true
        decor.animate().alpha(0f).setDuration(180).withEndAction { hide() }.start()
    }
    fun hide() {
        closing = false
        animator?.cancel(); animator = null
        if (blurRegistered) { manager.removeCrossWindowBlurEnabledListener(blurListener); blurRegistered = false }
        dialog?.window?.decorView?.animate()?.cancel()
        dialog?.dismiss(); dialog = null
        view?.disposeComposition(); view = null
        owner?.destroy(); owner = null; dragging = false
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
