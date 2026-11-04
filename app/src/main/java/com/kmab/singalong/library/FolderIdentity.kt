package com.kmab.singalong.library

import com.kmab.singalong.lyrics.SearchQueries
import com.kmab.singalong.model.Recording

/** Compare search hints without overwriting the original metadata or merging distinct editions. */
object FolderIdentity {
    private fun forms(recording: Recording) = (listOf(recording) + SearchQueries.plan(recording).map { it.recording }).distinct()
    fun same(a: Recording, b: Recording) = forms(a).any { left -> forms(b).any { right -> RecordingIdentity.compatible(left, right) } }
    fun match(existing: List<Recording>, incoming: Recording): Recording? {
        val matches = existing.filter { same(it, incoming) }
        return matches.singleOrNull() ?: matches.singleOrNull {
            RecordingIdentity.normalize(it.title) == RecordingIdentity.normalize(incoming.title) &&
                RecordingIdentity.artists(it) == RecordingIdentity.artists(incoming) &&
                RecordingIdentity.normalize(it.album.orEmpty()) == RecordingIdentity.normalize(incoming.album.orEmpty()) &&
                it.durationMs == incoming.durationMs
        }
    }
}
