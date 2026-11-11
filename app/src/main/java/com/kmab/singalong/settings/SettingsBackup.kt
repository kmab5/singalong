package com.kmab.singalong.settings

import com.kmab.singalong.library.LibrarySort

/** Legacy reading-settings representation, retained alongside the complete typed preference snapshot. */
object SettingsBackup {
    fun export(p: AppSettings): Map<String, String> = mapOf(
        "wifiOnly" to p.wifiOnly.toString(), "translationSize" to p.translationSizeSp.toString(),
        "textSize" to p.floating.currentTextSizeSp.toString(), "floatingOtherSize" to p.floating.otherTextSizeSp.toString(),
        "floatingAlignment" to p.floating.alignment.name, "floatingWords" to p.floating.wordTrackingEnabled.toString(),
        "opacity" to p.opacity.toString(), "width" to p.widthDp.toString(), "height" to p.heightDp.toString(),
        "x" to p.xFraction.toString(), "y" to p.yFraction.toString(), "minimal" to p.minimalMode.toString(),
        "minimalHeight" to p.minimalHeightDp.toString(), "librarySort" to p.librarySort.name,
        "nowSize" to p.now.textSizeSp.toString(), "nowTimedSize" to p.now.timedTextSizeSp.toString(),
        "nowAlignment" to p.now.alignment.name, "nowAnimation" to p.now.animation.name,
        "nowSpacing" to p.now.lineSpacing.toString(), "nowDim" to p.now.dimInactive.toString(),
        "nowTap" to p.now.tapToFocus.toString(), "nowWords" to p.now.wordTrackingEnabled.toString(),
    )

    fun merge(current: AppSettings, values: Map<String, String>): AppSettings {
        fun number(key: String, old: Float, min: Float, max: Float): Float =
            values[key]?.toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: old
        fun flag(key: String, old: Boolean) = values[key]?.toBooleanStrictOrNull() ?: old
        val floating = current.floating.copy(
            currentTextSizeSp = number("textSize", current.floating.currentTextSizeSp, 12f, 32f),
            otherTextSizeSp = number("floatingOtherSize", current.floating.otherTextSizeSp, 12f, 32f),
            alignment = LyricAlignment.entries.firstOrNull { it.name == values["floatingAlignment"] } ?: current.floating.alignment,
            wordTrackingEnabled = flag("floatingWords", current.floating.wordTrackingEnabled),
        ).sanitized()
        return current.copy(
            wifiOnly = flag("wifiOnly", current.wifiOnly),
            translationSizeSp = number("translationSize", current.translationSizeSp, 10f, 22f),
            floating = floating,
            opacity = number("opacity", current.opacity, .2f, .96f),
            widthDp = number("width", current.widthDp, 160f, 560f), heightDp = number("height", current.heightDp, 120f, 640f),
            xFraction = number("x", current.xFraction, 0f, 1f), yFraction = number("y", current.yFraction, 0f, 1f),
            minimalMode = flag("minimal", current.minimalMode), minimalHeightDp = number("minimalHeight", current.minimalHeightDp, 56f, 240f),
            librarySort = LibrarySort.entries.firstOrNull { it.name == values["librarySort"] } ?: current.librarySort,
            now = current.now.copy(
                textSizeSp = number("nowSize", current.now.textSizeSp, 12f, 40f),
                timedTextSizeSp = number("nowTimedSize", current.now.timedTextSizeSp, 12f, 56f),
                alignment = LyricAlignment.entries.firstOrNull { it.name == values["nowAlignment"] } ?: current.now.alignment,
                animation = LyricAnimation.entries.firstOrNull { it.name == values["nowAnimation"] } ?: current.now.animation,
                lineSpacing = number("nowSpacing", current.now.lineSpacing, 1.1f, 2f),
                dimInactive = flag("nowDim", current.now.dimInactive), tapToFocus = flag("nowTap", current.now.tapToFocus),
                wordTrackingEnabled = flag("nowWords", current.now.wordTrackingEnabled),
            ),
        )
    }
}
