package com.kmab.singalong.cards

/** Normalized reserved regions. Text is measured inside them, never over artwork or other credits. */
data class CardRegion(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right get() = x + width
    val bottom get() = y + height
    fun overlaps(other: CardRegion) = x < other.right - .0001f && right > other.x + .0001f &&
        y < other.bottom - .0001f && bottom > other.y + .0001f
}

data class CardComposition(val lyrics: CardRegion, val title: CardRegion, val details: CardRegion,
    val cover: CardRegion?, val brand: CardRegion) {
    companion object {
        fun create(template: CardTemplate, aspect: CardAspect, hasCover: Boolean): CardComposition {
            fun r(x: Float, y: Float, w: Float, h: Float) = CardRegion(x, y, w, h)
            val layout = when (template) {
                CardTemplate.GLASS -> CardComposition(r(.08f,.30f,.84f,.43f),r(.08f,.08f,.64f,.16f),r(.08f,.78f,.64f,.13f),r(.78f,.08f,.14f,.14f),r(.77f,.84f,.15f,.07f))
                CardTemplate.MIDNIGHT -> CardComposition(r(.08f,.25f,.84f,.46f),r(.08f,.08f,.84f,.12f),r(.08f,.77f,.62f,.13f),r(.77f,.75f,.15f,.15f),r(.08f,.93f,.40f,.04f))
                CardTemplate.PAPER -> CardComposition(r(.20f,.35f,.72f,.39f),r(.34f,.08f,.58f,.18f),r(.20f,.80f,.70f,.10f),r(.08f,.08f,.20f,.20f),r(.08f,.93f,.40f,.04f))
                CardTemplate.ARTWORK -> CardComposition(r(.08f,.38f,.84f,.34f),r(.30f,.08f,.62f,.17f),r(.08f,.78f,.62f,.13f),r(.08f,.08f,.16f,.16f),r(.77f,.84f,.15f,.07f))
                CardTemplate.SLEEVE -> CardComposition(r(.08f,.49f,.84f,.35f),r(.48f,.08f,.44f,.17f),r(.48f,.28f,.44f,.15f),r(.08f,.08f,.33f,.33f),r(.08f,.91f,.40f,.05f))
                CardTemplate.POSTCARD -> CardComposition(r(.08f,.49f,.84f,.32f),r(.08f,.10f,.53f,.14f),r(.08f,.27f,.53f,.15f),r(.68f,.10f,.23f,.23f),r(.64f,.90f,.28f,.05f))
                CardTemplate.DUET -> CardComposition(r(.51f,.13f,.43f,.68f),r(.06f,.55f,.37f,.15f),r(.06f,.74f,.37f,.16f),r(.06f,.13f,.37f,.37f),r(.51f,.90f,.43f,.05f))
                CardTemplate.GALLERY -> CardComposition(r(.12f,.57f,.76f,.23f),r(.12f,.43f,.76f,.10f),r(.12f,.84f,.62f,.10f),r(.34f,.07f,.32f,.32f),r(.79f,.87f,.15f,.07f))
            }
            // Fit a square cover inside its reserved region for every output orientation.
            val cover = layout.cover?.takeIf { hasCover }?.let {
                val side = minOf(it.width * aspect.width, it.height * aspect.height)
                val width = side / aspect.width
                it.copy(x = if (template == CardTemplate.GALLERY) it.x + (it.width - width) / 2f else it.x,
                    width = width, height = side / aspect.height)
            }
            return if (hasCover) layout.copy(cover = cover) else when (template) {
                CardTemplate.GALLERY -> layout.copy(cover = null, title = r(.12f,.12f,.76f,.12f), lyrics = r(.12f,.30f,.76f,.49f))
                CardTemplate.DUET -> layout.copy(cover = null, title = r(.08f,.08f,.84f,.15f), lyrics = r(.08f,.30f,.84f,.40f), details = r(.08f,.77f,.84f,.12f))
                else -> layout.copy(cover = null)
            }
        }
    }
}
