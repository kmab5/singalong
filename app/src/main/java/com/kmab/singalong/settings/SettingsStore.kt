package com.kmab.singalong.settings

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.singalongSettings by preferencesDataStore(
    name = "singalong_settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "foundation", setOf("tracking_enabled", "notification_requested"))) },
)

data class AppSettings(
    val ready: Boolean = false,
    val translationEnabled: Boolean = false,
    val translationSizeSp: Float = 16f,
    val trackingEnabled: Boolean = false,
    val allowedPlayers: Set<String> = emptySet(),
    val wifiOnly: Boolean = false,
    val notificationRequested: Boolean = false,
    val backupLimitMiB: Long = 50,
    val opacity: Float = .85f,
    val widthDp: Float = 344f,
    val heightDp: Float = 260f,
    val xFraction: Float = 0f,
    val yFraction: Float = 0.2f,
    val minimalMode: Boolean = false,
    val minimalHeightDp: Float = 88f,
    val now: NowAppearance = NowAppearance(),
    val floating: FloatingAppearance = FloatingAppearance(),
    val librarySort: com.kmab.singalong.library.LibrarySort = com.kmab.singalong.library.LibrarySort.TITLE,
)

class SettingsStore(context: Context, scope: CoroutineScope) {
    private val store = context.applicationContext.singalongSettings
    private object Keys {
        val translation = booleanPreferencesKey("translation_enabled")
        val translationConsent = booleanPreferencesKey("mymemory_consent")
        val translationSize = floatPreferencesKey("translation_size")
        val tracking = booleanPreferencesKey("tracking_enabled")
        val allowed = stringSetPreferencesKey("allowed_players")
        val wifi = booleanPreferencesKey("wifi_only")
        val notification = booleanPreferencesKey("notification_requested")
        val size = floatPreferencesKey("text_size_sp")
        val opacity = floatPreferencesKey("overlay_opacity")
        val width = floatPreferencesKey("overlay_width_dp")
        val height = floatPreferencesKey("overlay_height_dp")
        val x = floatPreferencesKey("overlay_x_fraction")
        val y = floatPreferencesKey("overlay_y_fraction")
        val minimal = booleanPreferencesKey("overlay_minimal")
        val minimalHeight = floatPreferencesKey("overlay_minimal_height")
        val nowSize = floatPreferencesKey("now_text_size")
        val nowTimedSize = floatPreferencesKey("now_timed_text_size")
        val librarySort = stringPreferencesKey("library_sort")
        val nowAlignment = stringPreferencesKey("now_alignment")
        val nowAnimation = stringPreferencesKey("now_animation")
        val nowSpacing = floatPreferencesKey("now_line_spacing")
        val nowDim = booleanPreferencesKey("now_dim_inactive")
        val nowTap = booleanPreferencesKey("now_tap_focus")
        val nowWords = booleanPreferencesKey("now_word_tracking")
        val floatingCurrentSize = floatPreferencesKey("floating_current_text_size")
        val floatingOtherSize = floatPreferencesKey("floating_other_text_size")
        val floatingAlignment = stringPreferencesKey("floating_alignment")
        val floatingWords = booleanPreferencesKey("floating_word_tracking")
        val backupLimit = longPreferencesKey("backup_limit_mib")
    }
    val state = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::readSettings).stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private fun readSettings(p: Preferences) = AppSettings(
            ready = true, translationEnabled = p[Keys.translation] == true && p[Keys.translationConsent] == true, translationSizeSp = (p[Keys.translationSize] ?: 16f).safe(10f, 22f, 16f), trackingEnabled = p[Keys.tracking] ?: false,
            allowedPlayers = p[Keys.allowed].orEmpty(), wifiOnly = p[Keys.wifi] ?: false,
            notificationRequested = p[Keys.notification] ?: false,
            backupLimitMiB = (p[Keys.backupLimit] ?: 50L).coerceIn(0L, Long.MAX_VALUE / (1024 * 1024)),
            opacity = (p[Keys.opacity] ?: .85f).safe(.20f, .96f, .85f),
            widthDp = (p[Keys.width] ?: 344f).safe(160f, 560f, 344f),
            heightDp = (p[Keys.height] ?: 260f).safe(120f, 640f, 260f),
            xFraction = (p[Keys.x] ?: 0f).safe(0f, 1f, 0f),
            yFraction = (p[Keys.y] ?: 0.2f).safe(0f, 1f, 0.2f),
            minimalMode = p[Keys.minimal] ?: false,
            minimalHeightDp = (p[Keys.minimalHeight] ?: 88f).safe(56f, 240f, 88f),
            floating = FloatingAppearance(
                currentTextSizeSp = p[Keys.floatingCurrentSize] ?: p[Keys.size] ?: 22f,
                otherTextSizeSp = p[Keys.floatingOtherSize] ?: 14f,
                alignment = LyricAlignment.entries.firstOrNull { it.name == p[Keys.floatingAlignment] } ?: LyricAlignment.START,
                wordTrackingEnabled = p[Keys.floatingWords] ?: true,
            ).sanitized(),
            now = NowAppearance(p[Keys.nowSize] ?: p[Keys.size] ?: 22f,
                LyricAlignment.entries.firstOrNull { it.name == p[Keys.nowAlignment] } ?: LyricAlignment.START,
                LyricAnimation.entries.firstOrNull { it.name == p[Keys.nowAnimation] } ?: LyricAnimation.FOCUS,
                p[Keys.nowSpacing] ?: 1.45f, p[Keys.nowDim] ?: true, p[Keys.nowTap] ?: true,
                p[Keys.nowTimedSize] ?: p[Keys.nowSize] ?: p[Keys.size] ?: 22f,
                p[Keys.nowWords] ?: true).sanitized(),
            librarySort = com.kmab.singalong.library.LibrarySort.entries.firstOrNull { it.name == p[Keys.librarySort] } ?: com.kmab.singalong.library.LibrarySort.TITLE,
        )

    suspend fun exportPreferences(): Map<String, String> {
        val preferences = store.data.first()
        return SettingsBackup.export(readSettings(preferences)) + PreferenceBackup.export(preferences)
    }

    suspend fun backupLimit(value: Long) {
        require(value in 0L..Long.MAX_VALUE / (1024 * 1024))
        store.edit { it[Keys.backupLimit] = value }
    }

    suspend fun validateBackupPreferences(values: Map<String, String>) {
        restoredSettings(store.data.first().toMutablePreferences(), values)
    }

    private fun restoredSettings(p: MutablePreferences, values: Map<String, String>): AppSettings {
        if (PreferenceBackup.contains(values)) PreferenceBackup.restoreInto(p, values)
        val current = try { readSettings(p) } catch (_: ClassCastException) {
            throw IllegalArgumentException("A saved setting has the wrong type.")
        }
        return SettingsBackup.merge(current, values)
    }

    /** Merge against the current DataStore transaction, never a potentially stale UI snapshot. */
    suspend fun restorePreferences(values: Map<String, String>) { store.edit { p ->
        val v = restoredSettings(p, values)
        p[Keys.wifi] = v.wifiOnly; p[Keys.translationSize] = v.translationSizeSp
        p[Keys.floatingCurrentSize] = v.floating.currentTextSizeSp; p[Keys.floatingOtherSize] = v.floating.otherTextSizeSp
        p[Keys.floatingAlignment] = v.floating.alignment.name; p[Keys.floatingWords] = v.floating.wordTrackingEnabled
        p[Keys.opacity] = v.opacity; p[Keys.width] = v.widthDp; p[Keys.height] = v.heightDp
        p[Keys.x] = v.xFraction; p[Keys.y] = v.yFraction
        p[Keys.minimal] = v.minimalMode; p[Keys.minimalHeight] = v.minimalHeightDp
        p[Keys.nowSize] = v.now.textSizeSp; p[Keys.nowTimedSize] = v.now.timedTextSizeSp
        p[Keys.nowAlignment] = v.now.alignment.name; p[Keys.nowAnimation] = v.now.animation.name
        p[Keys.nowSpacing] = v.now.lineSpacing; p[Keys.nowDim] = v.now.dimInactive
        p[Keys.nowTap] = v.now.tapToFocus; p[Keys.nowWords] = v.now.wordTrackingEnabled
        p[Keys.librarySort] = v.librarySort.name
    } }

    suspend fun translation(enabled: Boolean) { store.edit { it[Keys.translation] = enabled; it[Keys.translationConsent] = enabled } }
    suspend fun librarySort(value: com.kmab.singalong.library.LibrarySort) { store.edit { it[Keys.librarySort] = value.name } }
    suspend fun nowAppearance(value: NowAppearance) { val v = value.sanitized(); store.edit {
        it[Keys.nowSize] = v.textSizeSp; it[Keys.nowTimedSize] = v.timedTextSizeSp; it[Keys.nowAlignment] = v.alignment.name; it[Keys.nowAnimation] = v.animation.name
        it[Keys.nowSpacing] = v.lineSpacing; it[Keys.nowDim] = v.dimInactive; it[Keys.nowTap] = v.tapToFocus
        it[Keys.nowWords] = v.wordTrackingEnabled
    } }
    suspend fun translationSize(value: Float) { store.edit { it[Keys.translationSize] = value.safe(10f, 22f, 16f) } }
    suspend fun floatingAppearance(value: FloatingAppearance) { val v = value.sanitized(); store.edit {
        it[Keys.floatingCurrentSize] = v.currentTextSizeSp; it[Keys.floatingOtherSize] = v.otherTextSizeSp
        it[Keys.floatingAlignment] = v.alignment.name; it[Keys.floatingWords] = v.wordTrackingEnabled
    } }
    suspend fun minimal(enabled: Boolean) { store.edit { it[Keys.minimal] = enabled } }
    suspend fun minimalHeight(value: Float) { store.edit { it[Keys.minimalHeight] = value.safe(56f, 240f, 88f) } }
    suspend fun tracking(enabled: Boolean) { store.edit { it[Keys.tracking] = enabled } }
    suspend fun notificationRequested() { store.edit { it[Keys.notification] = true } }
    suspend fun wifiOnly(enabled: Boolean) { store.edit { it[Keys.wifi] = enabled } }
    suspend fun allowPlayer(packageName: String, allowed: Boolean) { store.edit {
        it[Keys.allowed] = if (allowed) it[Keys.allowed].orEmpty() + packageName else it[Keys.allowed].orEmpty() - packageName
    } }
    suspend fun opacity(value: Float) { store.edit { it[Keys.opacity] = value.safe(.20f, .96f, .85f) } }
    suspend fun geometry(x: Float, y: Float, width: Float, height: Float) { store.edit {
        it[Keys.x] = x.safe(0f, 1f, 0f); it[Keys.y] = y.safe(0f, 1f, 0.2f)
        it[Keys.width] = width.safe(160f, 560f, 344f); it[Keys.height] = height.safe(120f, 640f, 260f)
    } }
    private fun Float.safe(min: Float, max: Float, fallback: Float) = if (isFinite()) coerceIn(min, max) else fallback
}
