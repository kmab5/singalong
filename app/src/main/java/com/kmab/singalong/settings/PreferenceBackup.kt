package com.kmab.singalong.settings

import androidx.datastore.preferences.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/** Typed snapshot of every persisted preference, including keys added in later app versions. */
object PreferenceBackup {
    private const val KEY = "savedPreferences"
    @Serializable private data class Value(val type: String, val value: JsonElement)
    private val json = Json { allowSpecialFloatingPointValues = true }
    private val deviceChoices = setOf("tracking_enabled", "translation_enabled", "mymemory_consent")

    fun contains(values: Map<String, String>) = KEY in values

    fun export(preferences: Preferences): Map<String, String> {
        val snapshot = preferences.asMap().entries.associate { (key, value) ->
            key.name to when (value) {
                is Boolean -> Value("boolean", JsonPrimitive(value))
                is Int -> Value("int", JsonPrimitive(value))
                is Long -> Value("long", JsonPrimitive(value))
                is Float -> Value("float", JsonPrimitive(value))
                is Double -> Value("double", JsonPrimitive(value))
                is String -> Value("string", JsonPrimitive(value))
                is Set<*> -> Value("strings", JsonArray(value.map { JsonPrimitive(it as String) }.sortedBy { it.content }))
                is ByteArray -> Value("bytes", JsonPrimitive(java.util.Base64.getEncoder().encodeToString(value)))
                else -> error("Unsupported stored preference type: ${key.name}")
            }
        }
        return mapOf(KEY to json.encodeToString(snapshot))
    }

    fun decode(values: Map<String, String>): Preferences {
        val snapshot = json.decodeFromString<Map<String, Value>>(requireNotNull(values[KEY]))
        val result = mutablePreferencesOf()
        snapshot.forEach { (name, item) ->
            require(name.isNotEmpty()) { "Invalid preference name." }
            when (item.type) {
                "boolean" -> result[booleanPreferencesKey(name)] = item.value.jsonPrimitive.boolean
                "int" -> result[intPreferencesKey(name)] = item.value.jsonPrimitive.int
                "long" -> result[longPreferencesKey(name)] = item.value.jsonPrimitive.long
                "float" -> result[floatPreferencesKey(name)] = item.value.jsonPrimitive.float
                "double" -> result[doublePreferencesKey(name)] = item.value.jsonPrimitive.double
                "string" -> { require(item.value.jsonPrimitive.isString); result[stringPreferencesKey(name)] = item.value.jsonPrimitive.content }
                "strings" -> result[stringSetPreferencesKey(name)] = item.value.jsonArray.map { require(it.jsonPrimitive.isString); it.jsonPrimitive.content }.toSet()
                "bytes" -> result[byteArrayPreferencesKey(name)] = java.util.Base64.getDecoder().decode(item.value.jsonPrimitive.content)
                else -> throw IllegalArgumentException("Unsupported saved preference type. Update Singalong before restoring.")
            }
        }
        return result
    }

    /** Export includes these choices; applying a file never starts tracking or grants network consent. */
    @Suppress("UNCHECKED_CAST")
    fun restoreInto(target: MutablePreferences, values: Map<String, String>) {
        val decoded = decode(values)
        val retained = target.asMap().filterKeys { it.name in deviceChoices }
        target.clear()
        (decoded.asMap().filterKeys { it.name !in deviceChoices } + retained).forEach { (key, value) ->
            target[key as Preferences.Key<Any>] = value
        }
    }
}
