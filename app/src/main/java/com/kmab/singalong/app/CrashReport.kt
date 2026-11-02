package com.kmab.singalong.app

/** Deliberately excludes exception messages, metadata, lyrics, URLs and thread names. */
object CrashReport {
    fun format(error: Throwable, version: String, api: Int, occurredAt: String): String = buildString {
        appendLine("singalong $version · Android API $api")
        appendLine("Occurred: $occurredAt")
        appendLine("Exception types and code locations only; no exception messages or listening history.")
        var cause: Throwable? = error
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
        repeat(4) {
            val current = cause ?: return@repeat
            if (!seen.add(current)) { cause = null; return@repeat }
            appendLine(current.javaClass.name)
            current.stackTrace.take(48).forEach { frame -> appendLine("  at $frame") }
            cause = current.cause
        }
    }.take(24_000)
}
