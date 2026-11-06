package com.kmab.singalong.cards

import android.graphics.Bitmap
import java.util.UUID

/** Transient snapshot, kept only while the user has a card open. Never persisted to the library. */
data class CardRequest(val content: CardContent, val cover: Bitmap? = null, val id: String = UUID.randomUUID().toString())
