package com.kmab.singalong.cards

/** Normalized content regions. The brand is an overlay; text wraps around its local footprint. */
data class CardRegion(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right get() = x + width
    val bottom get() = y + height
    fun overlaps(other: CardRegion) = x < other.right - .0001f && right > other.x + .0001f &&
        y < other.bottom - .0001f && bottom > other.y + .0001f
}

data class CardComposition(val lyrics: CardRegion, val credits: CardRegion,
    val cover: CardRegion?, val brand: CardRegion, val centeredCredits: Boolean = false) {
    companion object {
        /** Physical insets are based on the short edge, so landscape never shrinks the album into a badge. */
        fun create(requestedStyle: CardStyle, hasCover: Boolean): CardComposition {
            val style = requestedStyle.sanitized()
            val w = style.aspect.width.toFloat()
            val h = style.aspect.height.toFloat()
            val unit = minOf(w, h)
            val margin = unit * .08f
            val gap = unit * .045f
            val brandHeight = unit * style.logoScale
            val bottom = h - margin
            val inner = w - 2 * margin
            val creditHeight = if (style.details == CardDetails(false, false, false)) 0f else unit * .10f
            val side = unit * style.artScale
            fun r(x: Float, y: Float, width: Float, height: Float) = CardRegion(x / w, y / h, width / w, height / h)
            val brandWidth = brandHeight * if (style.showAppName) 3.8f else 26f / 24f
            val brand = r(if (style.logoPosition.left) margin else w - margin - brandWidth,
                if (style.logoPosition.top) margin else bottom - brandHeight, brandWidth, brandHeight)
            if (!hasCover) return CardComposition(
                r(margin, margin, inner, bottom - creditHeight - (if (creditHeight > 0) gap else 0f) - margin),
                r(margin, bottom - creditHeight, inner, creditHeight), null, brand,
                centeredCredits = style.template in setOf(CardTemplate.SLEEVE, CardTemplate.GALLERY))
            val template = style.template
            return when (template) {
                CardTemplate.GLASS, CardTemplate.POSTCARD -> {
                    val left = template == CardTemplate.GLASS
                    val coverX = if (left) margin else w - margin - side
                    val creditsX = if (left) margin + side + gap else margin
                    val lyricY = margin + side + gap
                    CardComposition(r(margin, lyricY, inner, bottom - lyricY),
                        r(creditsX, margin, inner - side - gap, side), r(coverX, margin, side, side), brand)
                }
                CardTemplate.MIDNIGHT, CardTemplate.PAPER -> {
                    val left = template == CardTemplate.MIDNIGHT
                    val coverX = if (left) margin else w - margin - side
                    val creditsX = if (left) margin + side + gap else margin
                    val mediaY = bottom - side
                    CardComposition(r(margin, margin, inner, mediaY - gap - margin),
                        r(creditsX, mediaY, inner - side - gap, side), r(coverX, mediaY, side, side), brand)
                }
                CardTemplate.DUET -> {
                    val lyricX = margin + side + gap
                    val creditY = margin + side + gap
                    CardComposition(r(lyricX, margin, w - margin - lyricX, bottom - margin),
                        r(margin, creditY, side, bottom - creditY), r(margin, margin, side, side), brand)
                }
                CardTemplate.GALLERY -> {
                    val lyricY = margin + side + gap
                    CardComposition(r(margin, lyricY, inner, bottom - creditHeight - (if (creditHeight > 0) gap else 0f) - lyricY),
                        r(margin, bottom - creditHeight, inner, creditHeight), r((w - side) / 2f, margin, side, side), brand, true)
                }
                CardTemplate.SLEEVE -> {
                    val coverY = bottom - side
                    val creditsY = coverY - gap - creditHeight
                    CardComposition(r(margin, margin, inner, creditsY - gap - margin),
                        r(margin, creditsY, inner, creditHeight), r((w - side) / 2f, coverY, side, side), brand, true)
                }
                CardTemplate.ARTWORK -> {
                    val bannerHeight = unit * style.artScale * .85f
                    val creditsY = margin + bannerHeight + gap
                    val lyricY = creditsY + creditHeight + gap
                    CardComposition(r(margin, lyricY, inner, bottom - lyricY),
                        r(margin, creditsY, inner, creditHeight), r(margin, margin, inner, bannerHeight), brand)
                }
            }
        }
    }
}
