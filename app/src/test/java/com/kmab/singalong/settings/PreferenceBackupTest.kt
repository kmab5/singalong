package com.kmab.singalong.settings

import androidx.datastore.preferences.core.*
import org.junit.Assert.*
import org.junit.Test

class PreferenceBackupTest {
    @Test fun `exports every saved preference including players runtime choices and future keys`() {
        val original = preferencesOf(
            stringSetPreferencesKey("allowed_players") to setOf("player.one", "player.two"),
            booleanPreferencesKey("tracking_enabled") to true,
            booleanPreferencesKey("translation_enabled") to true,
            booleanPreferencesKey("mymemory_consent") to true,
            booleanPreferencesKey("notification_requested") to true,
            floatPreferencesKey("overlay_opacity") to .6f,
            longPreferencesKey("backup_limit_mib") to 0L,
            stringPreferencesKey("future_preference") to "saved choice",
        )
        val snapshot = PreferenceBackup.export(original)
        assertEquals(original.asMap(), PreferenceBackup.decode(snapshot).asMap())
        val target = mutablePreferencesOf(booleanPreferencesKey("tracking_enabled") to false)
        PreferenceBackup.restoreInto(target, snapshot)
        assertEquals(setOf("player.one", "player.two"), target[stringSetPreferencesKey("allowed_players")])
        assertEquals("saved choice", target[stringPreferencesKey("future_preference")])
        assertEquals(true, target[booleanPreferencesKey("notification_requested")])
        assertEquals(false, target[booleanPreferencesKey("tracking_enabled")])
        assertNull(target[booleanPreferencesKey("mymemory_consent")])
    }

    @Test fun `snapshot restores defaults by removing settings absent from source`() {
        val target = mutablePreferencesOf(floatPreferencesKey("overlay_opacity") to .3f,
            booleanPreferencesKey("translation_enabled") to true, booleanPreferencesKey("mymemory_consent") to true)
        PreferenceBackup.restoreInto(target, PreferenceBackup.export(emptyPreferences()))
        assertNull(target[floatPreferencesKey("overlay_opacity")])
        assertEquals(true, target[booleanPreferencesKey("translation_enabled")])
        assertEquals(true, target[booleanPreferencesKey("mymemory_consent")])
    }

    @Test fun `typed preferences preserve numeric types empty sets and binary values`() {
        val original = preferencesOf(intPreferencesKey("integer") to 7,
            longPreferencesKey("large") to Long.MAX_VALUE, doublePreferencesKey("precise") to .125,
            stringSetPreferencesKey("empty") to emptySet(), byteArrayPreferencesKey("binary") to byteArrayOf(0, -1, 30))
        val decoded = PreferenceBackup.decode(PreferenceBackup.export(original))
        assertEquals(7, decoded[intPreferencesKey("integer")])
        assertEquals(Long.MAX_VALUE, decoded[longPreferencesKey("large")])
        assertEquals(.125, decoded[doublePreferencesKey("precise")]!!, 0.0)
        assertEquals(emptySet<String>(), decoded[stringSetPreferencesKey("empty")])
        assertArrayEquals(byteArrayOf(0, -1, 30), decoded[byteArrayPreferencesKey("binary")])
    }

    @Test fun `invalid snapshot does not remove existing preferences`() {
        val target = mutablePreferencesOf(stringPreferencesKey("keep") to "original")
        assertThrows(IllegalArgumentException::class.java) {
            PreferenceBackup.restoreInto(target, mapOf("savedPreferences" to """{"x":{"type":"unknown","value":true}}"""))
        }
        assertEquals("original", target[stringPreferencesKey("keep")])
    }
}
