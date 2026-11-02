package com.kmab.singalong.lyrics

import com.kmab.singalong.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class LyricSearchResult(val status: String, val versions: List<LyricVersion> = emptyList(), val retryAfterMs: Long? = null)

/** Coordinates bounded queries while retaining strict matching and recording identity. */
class LyricSearch(private val provider: LyricsProvider) {
    suspend fun search(recording: Recording, permitsNetwork: () -> Boolean): LyricSearchResult {
        for (query in SearchQueries.plan(recording)) {
            currentCoroutineContext().ensureActive()
            if (!permitsNetwork()) return LyricSearchResult("offline")
            val response = provider.search(query.recording)
            currentCoroutineContext().ensureActive()
            when (response) {
                is ProviderResult.Failed -> return LyricSearchResult(response.reason, retryAfterMs = response.retryAfterMs)
                is ProviderResult.Songs -> {
                    val versions = LyricsMatcher.versions(query.recording, response.songs).map { version ->
                        if (!query.inferred) version else version.copy(
                            confidence = MatchConfidence.UNCERTAIN,
                            attribution = "${version.attribution} · Search details inferred",
                        )
                    }
                    if (versions.isNotEmpty()) return LyricSearchResult("available", versions)
                    if (!query.inferred && response.songs.any {
                        it.instrumental && LyricsMatcher.confidence(query.recording, it) == MatchConfidence.CONFIDENT
                    }) return LyricSearchResult("instrumental")
                }
            }
        }
        return LyricSearchResult("not_found")
    }
}
