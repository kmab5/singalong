package com.kmab.singalong.overlay

import com.kmab.singalong.settings.AppSettings
import kotlin.math.roundToInt

data class PanelBounds(val x: Int, val y: Int, val width: Int, val height: Int)

object OverlayGeometry {
    fun bounds(panel: OverlayState, settings: AppSettings, safe: PanelBounds, density: Float): PanelBounds {
        val docked = panel.mode != PanelMode.VISIBLE
        val width = ((if (docked) 48f else settings.widthDp) * density).roundToInt().coerceIn(1, safe.width)
        val minimumHeight = OverlayControlsLayout.minimumHeight(width / density)
        val heightDp = when {
            docked -> 96f
            settings.minimalMode && !panel.controlsVisible -> settings.minimalHeightDp
            settings.minimalMode && panel.controlsVisible -> maxOf(settings.heightDp, 220f, minimumHeight)
            panel.expanded -> maxOf((settings.heightDp * 1.6f).coerceAtMost(640f), minimumHeight)
            else -> settings.heightDp.coerceAtLeast(minimumHeight)
        }
        val height = (heightDp * density).roundToInt().coerceIn(1, safe.height)
        val fraction = if (docked) if (settings.xFraction < .5f) 0f else 1f else settings.xFraction
        return PanelBounds(safe.x + ((safe.width - width) * fraction).roundToInt(),
            safe.y + ((safe.height - height) * settings.yFraction).roundToInt(), width, height)
    }
}
