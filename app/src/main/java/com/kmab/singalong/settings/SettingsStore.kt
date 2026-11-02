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
    val textSizeSp: Float = 22f,
    val opacity: Float = .85f,
    val widthDp: Float = 344f,
    val heightDp: Float = 260f,
    val xFraction: Float = 0f,
    val yFraction: Float = 0.2f,
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
    }
    val state = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        AppSettings(
            ready = true, translationEnabled = p[Keys.translation] == true && p[Keys.translationConsent] == true, translationSizeSp = (p[Keys.translationSize] ?: 16f).safe(14f, 22f, 16f), trackingEnabled = p[Keys.tracking] ?: false,
            allowedPlayers = p[Keys.allowed].orEmpty(), wifiOnly = p[Keys.wifi] ?: false,
            notificationRequested = p[Keys.notification] ?: false,
            textSizeSp = (p[Keys.size] ?: 22f).safe(18f, 32f, 22f),
            opacity = (p[Keys.opacity] ?: .85f).safe(.85f, .96f, .85f),
            widthDp = (p[Keys.width] ?: 344f).safe(260f, 560f, 344f),
            heightDp = (p[Keys.height] ?: 260f).safe(180f, 640f, 260f),
            xFraction = (p[Keys.x] ?: 0f).safe(0f, 1f, 0f),
            yFraction = (p[Keys.y] ?: 0.2f).safe(0f, 1f, 0.2f),
        )
    }.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun translation(enabled: Boolean) { store.edit { it[Keys.translation] = enabled; it[Keys.translationConsent] = enabled } }
    suspend fun translationSize(value: Float) { store.edit { it[Keys.translationSize] = value.safe(14f, 24f, 16f) } }
    suspend fun tracking(enabled: Boolean) { store.edit { it[Keys.tracking] = enabled } }
    suspend fun notificationRequested() { store.edit { it[Keys.notification] = true } }
    suspend fun wifiOnly(enabled: Boolean) { store.edit { it[Keys.wifi] = enabled } }
    suspend fun allowPlayer(packageName: String, allowed: Boolean) { store.edit {
        it[Keys.allowed] = if (allowed) it[Keys.allowed].orEmpty() + packageName else it[Keys.allowed].orEmpty() - packageName
    } }
    suspend fun textSize(value: Float) { store.edit { it[Keys.size] = value.safe(18f, 32f, 22f) } }
    suspend fun opacity(value: Float) { store.edit { it[Keys.opacity] = value.safe(.85f, .96f, .85f) } }
    suspend fun geometry(x: Float, y: Float, width: Float, height: Float) { store.edit {
        it[Keys.x] = x.safe(0f, 1f, 0f); it[Keys.y] = y.safe(0f, 1f, 0.2f)
        it[Keys.width] = width.safe(260f, 560f, 344f); it[Keys.height] = height.safe(180f, 640f, 260f)
    } }
    private fun Float.safe(min: Float, max: Float, fallback: Float) = if (isFinite()) coerceIn(min, max) else fallback
}
