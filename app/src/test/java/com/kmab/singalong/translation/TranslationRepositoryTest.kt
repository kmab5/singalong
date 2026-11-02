package com.kmab.singalong.translation

import com.kmab.singalong.editor.LyricsEditor
import com.kmab.singalong.model.LyricVersion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TranslationRepositoryTest {
    @Test fun partialResultsAreValidatedAndNeverPersistedAsACompleteVersion() = runTest {
        var saved: TranslationRecord? = null
        val original = LyricsEditor.import("r", "Bir\nİki")
        val partial = TranslationRecord(original.id, original.contentHash, "fixture", "tr", mapOf(original.lines.first().id to "One"), "Fixture")
        val received = mutableListOf<TranslationRecord>()
        val repository = TranslationRepository(object : TranslationStore {
            override suspend fun translation(versionId: String) = saved
            override suspend fun saveTranslation(record: TranslationRecord) { saved = record }
        }, object : ProgressiveTranslationProvider {
            override val id = "fixture"
            override suspend fun translate(version: LyricVersion) = error("Use progressive translation")
            override suspend fun translateProgressively(version: LyricVersion, online: Boolean, onProgress: suspend (TranslationRecord) -> Unit): TranslationResult {
                onProgress(partial.copy(versionId = "another version"))
                onProgress(partial.copy(lines = mapOf("unknown" to "Wrong line")))
                onProgress(partial)
                return TranslationResult.Unavailable
            }
        })
        assertEquals(TranslationResult.Unavailable, repository.resolve(original, true, true, false) { received += it })
        assertEquals(listOf(partial), received)
        assertNull(saved)
    }
    @Test fun optInAndOpenedGateNetworkAndValidatedCacheSurvivesOffline() = runTest {
        var calls = 0
        var cache: TranslationRecord? = null
        val store = object : TranslationStore {
            override suspend fun translation(versionId: String) = cache
            override suspend fun saveTranslation(record: TranslationRecord) { cache = record }
        }
        val provider = object : TranslationProvider {
            override val id = "fixture"
            override suspend fun translate(version: LyricVersion): TranslationResult {
                calls++
                return TranslationResult.Available(TranslationRecord(version.id, version.contentHash, id, "ja", version.lines.associate { it.id to "Synthetic meaning" }, "Test provider"))
            }
        }
        val repository = TranslationRepository(store, provider)
        val original = LyricsEditor.import("r", "世界")
        repository.resolve(original, false, true, true)
        repository.resolve(original, true, false, true)
        assertEquals(0, calls)
        assertTrue(repository.resolve(original, true, true, true) is TranslationResult.Available)
        assertTrue(repository.resolve(original, true, true, false) is TranslationResult.Available)
        assertEquals(1, calls)
        val edited = LyricsEditor.edit(original, "New words", false)
        assertEquals(TranslationResult.Unavailable, repository.resolve(edited, true, true, false))
        assertFalse(TranslationRepository.valid(original, cache!!.copy(lines = emptyMap())))
    }
}
