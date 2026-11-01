package com.kmab.singalong.translation

import com.kmab.singalong.editor.LyricsEditor
import com.kmab.singalong.model.LyricVersion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TranslationRepositoryTest {
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
