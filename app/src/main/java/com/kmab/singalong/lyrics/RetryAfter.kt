package com.kmab.singalong.lyrics

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal fun retryAfterMillis(value: String?, nowMs: Long): Long? {
    value ?: return null
    value.toLongOrNull()?.let { return it.coerceIn(0, Long.MAX_VALUE / 1000) * 1000 }
    return try { (ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - nowMs).coerceAtLeast(0) }
    catch (_: DateTimeParseException) { null }
}
