package com.kmab.singalong.overlay

import com.kmab.singalong.settings.AppSettings
import kotlin.math.roundToInt

data class PanelBounds(val x: Int, val y: Int, val width: Int, val height: Int)

object OverlayGeometry {
    fun bounds(panel: OverlayState, settings: AppSettings, safe: PanelBounds, density: Float, fontScale: Float = 1f): PanelBounds {
        val docked = panel.mode != PanelMode.VISIBLE
        val width = ((if (docked) 48f else settings.widthDp) * density).roundToInt().coerceIn(1, safe.width)
        val heightDp = if (docked) 96f else if (panel.expanded) maxOf(settings.heightDp, 420f)
            else maxOf(230f, 150f + settings.textSizeSp * fontScale * 3f + if (settings.translationEnabled) settings.translationSizeSp * fontScale * 2 else 0f)
        val height = (heightDp * density).roundToInt().coerceIn(1, safe.height)
        val fraction = if (docked) if (settings.xFraction < .5f) 0f else 1f else settings.xFraction
        return PanelBounds(safe.x + ((safe.width - width) * fraction).roundToInt(),
            safe.y + ((safe.height - height) * settings.yFraction).roundToInt(), width, height)
    }
}
