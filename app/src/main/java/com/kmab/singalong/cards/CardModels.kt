package com.kmab.singalong.cards

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.Recording
import kotlinx.serialization.Serializable

data class CardContent(val recording: Recording, val lines: List<LyricLine>, val source: String?, val initialLine: Int) {
    companion object {
        fun capture(recording: Recording?, version: com.kmab.singalong.model.LyricVersion, index: Int): CardContent? {
            if (recording == null || recording.id != version.recordingId || version.lines.getOrNull(index)?.text.isNullOrBlank()) return null
            val credit = listOfNotNull(version.sourceName, version.attribution).filter { it.isNotBlank() }.distinct().joinToString(" · ").ifBlank { null }
            return CardContent(recording.copy(artists = recording.artists.toList()), version.lines.toList(), credit, index)
        }
    }
    fun excerpt(selected: Set<Int>): String = lines.filterIndexed { index, _ -> index in selected }.joinToString("\n") { it.text }
    fun selectionError(selected: Set<Int>): String? {
        val text = excerpt(selected)
        return when {
            text.isBlank() -> "Select at least one lyric line."
            text.length > 6000 -> "Select a shorter excerpt for this card (up to 6,000 characters)."
            else -> null
        }
    }
}

@Serializable enum class CardAspect(val label: String, val width: Int, val height: Int) {
    SQUARE("Square", 1080, 1080), PORTRAIT("Portrait", 1080, 1350), STORY("Story", 1080, 1920), LANDSCAPE("Landscape", 1600, 900)
}
@Serializable enum class CardFont(val label: String) { SANS("Sans"), SERIF("Serif"), MONO("Mono") }
@Serializable enum class CardAlignment { START, CENTER, END }
@Serializable enum class CardBackground { SOLID, GRADIENT, IMAGE }
@Serializable enum class CardTemplate(val label: String) {
    GLASS("Glass"), MIDNIGHT("Midnight"), PAPER("Paper"), ARTWORK("Artwork");
    fun style(): CardStyle = when (this) {
        GLASS -> CardStyle(template = this)
        MIDNIGHT -> CardStyle(template = this, backgroundColor = 0xff071d2b.toInt(), accentColor = 0xff71d9db.toInt(), background = CardBackground.SOLID, font = CardFont.MONO)
        PAPER -> CardStyle(template = this, backgroundColor = 0xffeee8dd.toInt(), textColor = 0xff22202a.toInt(), accentColor = 0xff6b3294.toInt(), background = CardBackground.SOLID, font = CardFont.SERIF)
        ARTWORK -> CardStyle(template = this, background = CardBackground.IMAGE, aspect = CardAspect.PORTRAIT, alignment = CardAlignment.CENTER, showCover = false)
    }
}

@Serializable data class CardStyle(
    val template: CardTemplate = CardTemplate.GLASS,
    val aspect: CardAspect = CardAspect.SQUARE,
    val font: CardFont = CardFont.SANS,
    val alignment: CardAlignment = CardAlignment.START,
    val background: CardBackground = CardBackground.GRADIENT,
    val backgroundColor: Int = 0xff17121f.toInt(),
    val textColor: Int = 0xffedebe4.toInt(),
    val accentColor: Int = 0xffb24bff.toInt(),
    val fontSize: Float = 64f,
    val textX: Float = .5f,
    val textY: Float = .55f,
    val textWidth: Float = .84f,
    val showCover: Boolean = true,
    val coverSize: Float = .20f,
    val coverX: Float = 1f,
    val coverY: Float = 0f,
    val imageShade: Float = .6f,
) {
    fun sanitized() = copy(fontSize = fontSize.safe(24f, 112f, 64f),
        textX = textX.safe(0f, 1f, .5f), textY = textY.safe(0f, 1f, .55f), textWidth = textWidth.safe(.4f, .88f, .84f),
        coverSize = coverSize.safe(.1f, .4f, .20f), coverX = coverX.safe(0f, 1f, 1f), coverY = coverY.safe(0f, 1f, 0f),
        imageShade = imageShade.safe(0f, .9f, .6f))
    private fun Float.safe(min: Float, max: Float, fallback: Float) = if (isFinite()) coerceIn(min, max) else fallback
}
