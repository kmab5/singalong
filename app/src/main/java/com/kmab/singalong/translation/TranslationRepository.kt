package com.kmab.singalong.translation

import com.kmab.singalong.model.LyricVersion
import kotlinx.serialization.Serializable

@Serializable data class TranslationRecord(val versionId: String, val contentHash: String, val provider: String,
    val sourceLanguage: String, val lines: Map<String, String>, val attribution: String)
sealed interface TranslationResult {
    data class Available(val record: TranslationRecord) : TranslationResult
    data object Unavailable : TranslationResult
    data object Unsupported : TranslationResult
    data object Failed : TranslationResult
}
interface TranslationProvider {
    val id: String
    suspend fun translate(version: LyricVersion): TranslationResult
}
interface ProgressiveTranslationProvider : TranslationProvider {
    suspend fun translateProgressively(version: LyricVersion, online: Boolean, onProgress: suspend (TranslationRecord) -> Unit): TranslationResult
}
object UnavailableTranslationProvider : TranslationProvider {
    override val id = "unavailable"
    override suspend fun translate(version: LyricVersion) = TranslationResult.Unavailable
}
interface TranslationStore {
    suspend fun translation(versionId: String): TranslationRecord?
    suspend fun saveTranslation(record: TranslationRecord)
}
class TranslationRepository(private val store: TranslationStore, private val provider: TranslationProvider = UnavailableTranslationProvider) {
    suspend fun resolve(version: LyricVersion, enabled: Boolean, opened: Boolean, online: Boolean,
        onProgress: suspend (TranslationRecord) -> Unit = {}): TranslationResult {
        if (!enabled || !opened) return TranslationResult.Unavailable
        store.translation(version.id)?.takeIf { valid(version, it) }?.let { return TranslationResult.Available(it) }
        if (!online && provider !is ProgressiveTranslationProvider) return TranslationResult.Unavailable
        val result = try {
            if (provider is ProgressiveTranslationProvider) provider.translateProgressively(version, online) { partial ->
                if (validPartial(version, partial) && partial.provider == provider.id) onProgress(partial)
            } else provider.translate(version)
        } catch (_: java.io.IOException) { return TranslationResult.Failed }
        if (result is TranslationResult.Available) {
            if (!valid(version, result.record) || result.record.provider != provider.id) return TranslationResult.Failed
            store.saveTranslation(result.record)
        }
        return result
    }
    companion object {
        fun validPartial(version: LyricVersion, record: TranslationRecord) = record.versionId == version.id && record.contentHash == version.contentHash &&
            record.provider.isNotBlank() && record.sourceLanguage.isNotBlank() && record.attribution.isNotBlank() &&
            record.lines.isNotEmpty() && record.lines.keys.all { key -> version.lines.any { it.id == key && it.text.isNotBlank() } } &&
            record.lines.values.all { it.isNotBlank() && it.length <= 100_000 }
        fun valid(version: LyricVersion, record: TranslationRecord) = record.versionId == version.id && record.contentHash == version.contentHash &&
            record.provider.isNotBlank() && record.sourceLanguage.isNotBlank() && record.attribution.isNotBlank() &&
            record.lines.keys == version.lines.filter { it.text.isNotBlank() }.map { it.id }.toSet() &&
            record.lines.values.all { it.isNotBlank() && it.length <= 100_000 }
    }
}
