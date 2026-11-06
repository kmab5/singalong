package com.kmab.singalong.cards

/** Normalized reserved regions. Text is measured inside them, never over artwork or other credits. */
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
        fun create(style: CardStyle, hasCover: Boolean): CardComposition {
            val w = style.aspect.width.toFloat()
            val h = style.aspect.height.toFloat()
            val unit = minOf(w, h)
            val margin = unit * .08f
            val gap = unit * .045f
            val footerHeight = unit * .055f
            val footerY = h - margin - footerHeight
            val bottom = footerY - unit * .06f
            val inner = w - 2 * margin
            val creditHeight = unit * .10f
            val side = unit * style.artSize.fraction
            fun r(x: Float, y: Float, width: Float, height: Float) = CardRegion(x / w, y / h, width / w, height / h)
            val brandWidth = unit * .26f
            val brand = r(if (style.footer == CardFooter.START) margin else w - margin - brandWidth,
                footerY, brandWidth, footerHeight)
            if (!hasCover) return CardComposition(
                r(margin, margin, inner, bottom - creditHeight - gap - margin),
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
                    CardComposition(r(margin, lyricY, inner, bottom - creditHeight - gap - lyricY),
                        r(margin, bottom - creditHeight, inner, creditHeight), r((w - side) / 2f, margin, side, side), brand, true)
                }
                CardTemplate.SLEEVE -> {
                    val coverY = bottom - side
                    val creditsY = coverY - gap - creditHeight
                    CardComposition(r(margin, margin, inner, creditsY - gap - margin),
                        r(margin, creditsY, inner, creditHeight), r((w - side) / 2f, coverY, side, side), brand, true)
                }
                CardTemplate.ARTWORK -> {
                    val bannerHeight = unit * if (style.artSize == CardArtSize.STANDARD) .30f else .36f
                    val creditsY = margin + bannerHeight + gap
                    val lyricY = creditsY + creditHeight + gap
                    CardComposition(r(margin, lyricY, inner, bottom - lyricY),
                        r(margin, creditsY, inner, creditHeight), r(margin, margin, inner, bannerHeight), brand)
                }
            }
        }
    }
}
