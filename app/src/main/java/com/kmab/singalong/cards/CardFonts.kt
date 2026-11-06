package com.kmab.singalong.cards

import com.kmab.singalong.R

val CardFont.resource: Int get() = when (this) {
    CardFont.INTER -> R.font.inter_variable
    CardFont.MONO -> R.font.pt_mono
    CardFont.LORA -> R.font.lora
    CardFont.OSWALD -> R.font.oswald
    CardFont.CAVEAT -> R.font.caveat
    CardFont.SPACE -> R.font.spacegrotesk
    CardFont.PLAYFAIR -> R.font.playfairdisplay
}
