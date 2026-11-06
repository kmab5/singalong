package com.kmab.singalong.cards

import com.kmab.singalong.model.LyricLine
import com.kmab.singalong.model.Recording
import kotlinx.serialization.Serializable

data class CardContent(val recording: Recording, val lines: List<LyricLine>, val source: String?, val initialLine: Int,
    val file: CardFileDetails = CardFileDetails.from(recording.fileName)) {
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
@Serializable enum class CardFont(val label: String) {
    INTER("Inter"), LORA("Lora"), OSWALD("Oswald"), CAVEAT("Caveat"), SPACE("Space Grotesk"), PLAYFAIR("Playfair Display"), MONO("PT Mono")
}
@Serializable enum class CardAlignment { START, CENTER, END }
@Serializable enum class CardBackground { SOLID, GRADIENT, IMAGE }
@Serializable enum class CardArtSize(val label: String, val fraction: Float) { STANDARD("Standard", .34f), LARGE("Large", .42f) }
@Serializable enum class CardCorners(val label: String) { SQUARE("Square"), SOFT("Soft"), ROUND("Round") }
@Serializable enum class CardWeight(val label: String, val value: Int) { REGULAR("Regular", 400), BOLD("Bold", 700) }
@Serializable enum class CardSpacing(val label: String, val multiplier: Float) { CLOSE("Close", 1.06f), NATURAL("Natural", 1.18f), AIRY("Airy", 1.32f) }
@Serializable enum class CardPlacement(val label: String, val bias: Float) { TOP("Top", 0f), MIDDLE("Middle", .5f), BOTTOM("Bottom", 1f) }
@Serializable enum class CardFooter(val label: String) { START("Left"), END("Right") }
@Serializable enum class CardPalette(val label: String, val base: Int, val ink: Int, val accent: Int) {
    VIOLET("Iris", 0xff20172f.toInt(), 0xfff6efff.toInt(), 0xffb49be5.toInt()),
    BLUE("Tide", 0xff102c39.toInt(), 0xffeaf5f5.toInt(), 0xff83c9cf.toInt()),
    PAPER("Oat", 0xfff1e9da.toInt(), 0xff302c34.toInt(), 0xff84624d.toInt()),
    ROSE("Rose", 0xfff0dce2.toInt(), 0xff4b2737.toInt(), 0xff99586d.toInt()),
    INK("Ink", 0xff20232b.toInt(), 0xfff4f3ed.toInt(), 0xffb8c1d9.toInt()),
    MOSS("Moss", 0xff243b34.toInt(), 0xfff0f3da.toInt(), 0xffb9cc8c.toInt())
}
@Serializable data class CardDetails(val title: Boolean = true, val artist: Boolean = true, val album: Boolean = true,
    val fileName: Boolean = false, val location: Boolean = false)
enum class CardMetadata { TITLE, ARTIST, ALBUM, FILE_NAME, LOCATION }

fun CardContent.metadata(details: CardDetails): List<Pair<CardMetadata, String>> = listOfNotNull(
    recording.title.takeIf { details.title && it.isNotBlank() }?.let { CardMetadata.TITLE to it },
    recording.artists.joinToString(" · ").takeIf { details.artist && it.isNotBlank() }?.let { CardMetadata.ARTIST to it },
    recording.album?.takeIf { details.album && it.isNotBlank() }?.let { CardMetadata.ALBUM to it },
    file.name?.takeIf { details.fileName && it.isNotBlank() }?.let { CardMetadata.FILE_NAME to it },
    file.location?.takeIf { details.location && it.isNotBlank() }?.let { CardMetadata.LOCATION to it },
)

/** File metadata is optional; never turn a network URL or opaque content ID into a folder. */
data class CardFileDetails(val name: String? = null, val location: String? = null) {
    companion object {
        fun from(raw: String?): CardFileDetails {
            if (raw.isNullOrBlank()) return CardFileDetails()
            val path = if (raw.startsWith("file:")) {
                try { java.net.URI(raw).path ?: return CardFileDetails() } catch (_: java.net.URISyntaxException) { return CardFileDetails() }
            } else {
                val hasScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(raw)
                val drivePath = Regex("^[A-Za-z]:[/\\\\]").containsMatchIn(raw)
                if (hasScheme && !drivePath) return CardFileDetails()
                raw.replace('\\', '/')
            }
            val name = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return CardFileDetails()
            return CardFileDetails(name, path.substringBeforeLast('/', "").takeIf { it.isNotBlank() })
        }
    }
}

@Serializable enum class CardTemplate(val label: String, val description: String) {
    GLASS("Afterglow", "Artwork and credits above an open verse"), MIDNIGHT("Signal", "Bold words above the album"),
    PAPER("Margin", "A quiet verse with an album signature"), ARTWORK("Spotlight", "A wide artwork header"),
    SLEEVE("Sleeve", "Centered words with artwork below"), POSTCARD("Postcard", "Handwritten words beneath a music header"),
    DUET("Duet", "Large artwork beside the verse"), GALLERY("Gallery", "Centered artwork, words and credits");
    fun style(): CardStyle = when (this) {
        GLASS -> CardStyle(template = this)
        MIDNIGHT -> CardStyle(template = this, palette = CardPalette.INK, font = CardFont.OSWALD, background = CardBackground.SOLID)
        PAPER -> CardStyle(template = this, palette = CardPalette.PAPER, font = CardFont.LORA, background = CardBackground.SOLID)
        ARTWORK -> CardStyle(template = this, palette = CardPalette.INK, font = CardFont.SPACE, background = CardBackground.SOLID)
        SLEEVE -> CardStyle(template = this, palette = CardPalette.BLUE, font = CardFont.LORA, alignment = CardAlignment.CENTER, background = CardBackground.SOLID)
        POSTCARD -> CardStyle(template = this, palette = CardPalette.ROSE, font = CardFont.CAVEAT, background = CardBackground.SOLID)
        DUET -> CardStyle(template = this, palette = CardPalette.MOSS, font = CardFont.PLAYFAIR, background = CardBackground.SOLID)
        GALLERY -> CardStyle(template = this, palette = CardPalette.PAPER, font = CardFont.PLAYFAIR, alignment = CardAlignment.CENTER, background = CardBackground.SOLID)
    }
}

@Serializable data class CardStyle(
    val template: CardTemplate = CardTemplate.GLASS,
    val aspect: CardAspect = CardAspect.SQUARE,
    val font: CardFont = CardFont.INTER,
    val alignment: CardAlignment = CardAlignment.START,
    val background: CardBackground = CardBackground.GRADIENT,
    val palette: CardPalette = CardPalette.VIOLET,
    val fontSize: Float = 76f,
    val weight: CardWeight = CardWeight.REGULAR,
    val spacing: CardSpacing = CardSpacing.NATURAL,
    val placement: CardPlacement = CardPlacement.MIDDLE,
    val artSize: CardArtSize = CardArtSize.STANDARD,
    val corners: CardCorners = CardCorners.SOFT,
    val footer: CardFooter = CardFooter.END,
    val photoShade: Float = .78f,
    val details: CardDetails = CardDetails(),
    val showCover: Boolean = true,
    val showAppName: Boolean = false,
) {
    fun sanitized() = copy(fontSize = if (fontSize.isFinite()) fontSize.coerceIn(32f, 112f) else 76f,
        photoShade = if (photoShade.isFinite()) photoShade.coerceIn(.65f, .95f) else .78f)
    fun withTemplate(next: CardTemplate) = next.style().copy(aspect = aspect, details = details,
        showCover = showCover, showAppName = showAppName, footer = footer)
}
