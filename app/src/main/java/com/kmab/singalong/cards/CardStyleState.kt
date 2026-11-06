package com.kmab.singalong.cards

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Older activities may restore a draft containing controls removed by an app update. */
internal object CardStyleState {
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(style: CardStyle): String = json.encodeToString(style)
    fun decode(value: String): CardStyle = json.decodeFromString<CardStyle>(value).sanitized()
}
