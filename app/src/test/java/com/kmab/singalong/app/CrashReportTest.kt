package com.kmab.singalong.app

import org.junit.Assert.*
import org.junit.Test

class CrashReportTest {
    @Test fun reportRetainsCodeLocationsButExcludesMessagesAndPrivateMetadata() {
        val error = IllegalStateException("PRIVATE_SONG_SECRET_89", java.io.IOException("PRIVATE_LYRIC_SECRET_89"))
        error.stackTrace = arrayOf(StackTraceElement("com.example.Reader", "load", "Reader.kt", 42))
        val report = CrashReport.format(error, "0.3.2", 36, "2026-11-02T12:00:00Z")
        assertTrue(report.contains("IllegalStateException"))
        assertTrue(report.contains("Reader.kt:42"))
        assertTrue(report.contains("IOException"))
        assertFalse(report.contains("PRIVATE_SONG_SECRET_89"))
        assertFalse(report.contains("PRIVATE_LYRIC_SECRET_89"))
    }
    @Test fun cyclicAndLargeExceptionsStayBounded() {
        val first = IllegalStateException()
        val second = IllegalArgumentException(first)
        first.initCause(second)
        first.stackTrace = Array(5000) { StackTraceElement("Example", "run", "Example.kt", it) }
        val report = CrashReport.format(first, "test", 36, "fixture")
        assertTrue(report.length <= 24_000)
        assertEquals(1, Regex("java.lang.IllegalStateException").findAll(report).count())
        assertFalse(report.contains("Example.kt:4999"))
    }
}
