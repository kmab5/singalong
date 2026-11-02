package com.kmab.singalong.app

import android.content.Context
import android.os.Build
import com.kmab.singalong.BuildConfig
import java.io.File
import java.io.IOException
import java.time.Instant

/** Keep one private report for manual export; Android's original fatal handler still runs. */
object CrashDiagnostics {
    private fun file(context: Context) = File(context.noBackupFilesDir, "last-crash.txt")
    private fun reportId(report: String) = java.security.MessageDigest.getInstance("SHA-256").digest(report.toByteArray()).joinToString("") { "%02x".format(it) }
    fun pending(context: Context): String? = read(context)?.takeIf {
        context.getSharedPreferences("crash_review", Context.MODE_PRIVATE).getString("reviewed", null) != reportId(it)
    }
    fun reviewed(context: Context, report: String) {
        context.getSharedPreferences("crash_review", Context.MODE_PRIVATE).edit().putString("reviewed", reportId(report)).apply()
    }
    fun read(context: Context): String? = file(context).takeIf { it.isFile }?.inputStream()?.bufferedReader()?.use { reader ->
        val buffer = CharArray(24_000)
        val count = reader.read(buffer)
        if (count > 0) String(buffer, 0, count) else null
    }
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler() ?: return
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                file(context).writeText(CrashReport.format(error, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", Build.VERSION.SDK_INT, Instant.now().toString()))
            } catch (_: IOException) { /* Reporting cannot prevent Android from handling the crash. */ }
            catch (_: SecurityException) { /* Storage may be unavailable during shutdown. */ }
            finally { previous.uncaughtException(thread, error) }
        }
    }
}
