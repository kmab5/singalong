package com.kmab.singalong.lyrics

import com.kmab.singalong.library.LyricsStore
import com.kmab.singalong.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class LookupResult(val status: String, val version: LyricVersion? = null, val retryAfterMs: Long? = null)

class LyricsRepository(private val store: LyricsStore, private val provider: LyricsProvider) {
    private val locks = Array(32) { Mutex() }
    private val recent = ConcurrentHashMap<String, Pair<Long, LookupResult>>()
    suspend fun lookup(recording: Recording, permitsNetwork: () -> Boolean, nowMs: () -> Long = System::currentTimeMillis): LookupResult =
        locks[(recording.id.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            store.selectedVersion(recording.id)?.let { return@withLock LookupResult("available", it) }
            if (store.storedStatus(recording.id) == "instrumental") return@withLock LookupResult("instrumental")
            if (!permitsNetwork()) {
                store.setLookupStatus(recording.id, "offline")
                return@withLock LookupResult("offline")
            }
            recent[recording.id]?.takeIf { it.first > nowMs() }?.let { return@withLock it.second }
            val result = when (val response = provider.search(recording)) {
                is ProviderResult.Failed -> LookupResult(response.reason, retryAfterMs = response.retryAfterMs)
                is ProviderResult.Songs -> {
                    val versions = LyricsMatcher.versions(recording, response.songs)
                    if (versions.isNotEmpty()) {
                        store.saveCandidates(recording, versions)
                        LookupResult("available", store.selectedVersion(recording.id))
                    } else if (response.songs.any { it.instrumental && LyricsMatcher.confidence(recording, it) == MatchConfidence.CONFIDENT })
                        LookupResult("instrumental") else LookupResult("not_found")
                }
            }
            store.setLookupStatus(recording.id, result.status)
            if (recent.size > 256) recent.clear()
            recent[recording.id] = (nowMs() + (result.retryAfterMs ?: if (result.status in listOf("not_found", "instrumental")) 3_600_000 else 30_000)) to result
            result
        }
}
