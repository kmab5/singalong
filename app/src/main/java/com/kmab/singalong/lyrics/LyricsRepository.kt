package com.kmab.singalong.lyrics

import com.kmab.singalong.library.LyricsStore
import com.kmab.singalong.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap

data class LookupResult(val status: String, val version: LyricVersion? = null, val retryAfterMs: Long? = null)

class LyricsRepository(private val store: LyricsStore, provider: LyricsProvider, parsingDispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private val search = LyricSearch(provider, parsingDispatcher)
    private val locks = Array(32) { Mutex() }
    private val recent = ConcurrentHashMap<String, Pair<Long, LookupResult>>()
    suspend fun lookup(recording: Recording, permitsNetwork: () -> Boolean, nowMs: () -> Long = System::currentTimeMillis, forceRefresh: Boolean = false): LookupResult =
        locks[(recording.id.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            store.selectedVersion(recording.id)?.let { return@withLock LookupResult("available", it) }
            if (store.storedStatus(recording.id) == "instrumental") return@withLock LookupResult("instrumental")
            if (!permitsNetwork()) {
                store.setLookupStatus(recording.id, "offline")
                return@withLock LookupResult("offline")
            }
            recent[recording.id]?.takeIf { it.first > nowMs() }?.let { (deadline, cached) ->
                // Explicit retries may bypass misses/transient failures, never a server cooldown.
                if (!forceRefresh || cached.retryAfterMs != null)
                    return@withLock cached.copy(retryAfterMs = (deadline - nowMs()).coerceAtLeast(0))
            }
            val response = search.search(recording, permitsNetwork)
            val result = if (response.versions.isNotEmpty()) {
                store.saveCandidates(recording, response.versions)
                LookupResult("available", store.selectedVersion(recording.id))
            } else LookupResult(response.status, retryAfterMs = response.retryAfterMs)
            store.setLookupStatus(recording.id, result.status)
            if (recent.size > 256) recent.clear()
            if (result.version == null) recent[recording.id] = (nowMs() + (result.retryAfterMs ?: if (result.status in listOf("not_found", "instrumental")) 600_000 else 2_000)) to result
            result
        }
}
