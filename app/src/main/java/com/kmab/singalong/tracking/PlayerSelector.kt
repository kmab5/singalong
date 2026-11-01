package com.kmab.singalong.tracking

import com.kmab.singalong.model.PlaybackSnapshot
import com.kmab.singalong.model.PlaybackStatus

/** Stateful only for observed transitions; platform order is the initial tie-breaker. */
class PlayerSelector {
    private var previous = emptyMap<String, PlaybackStatus>()
    private var selectedId: String? = null

    fun select(sessions: List<PlaybackSnapshot>, allowed: Set<String>, overrideSessionId: String?): PlaybackSnapshot? {
        val eligible = sessions.filter { it.packageName in allowed }
        val started = eligible.firstOrNull { it.status == PlaybackStatus.PLAYING && previous[it.sessionId] != PlaybackStatus.PLAYING }
        val automatic = started
            ?: eligible.firstOrNull { it.sessionId == selectedId && it.status != PlaybackStatus.STOPPED }
            ?: eligible.firstOrNull { it.status == PlaybackStatus.PLAYING }
            ?: eligible.firstOrNull()
        previous = eligible.associate { it.sessionId to it.status }
        selectedId = automatic?.sessionId
        return eligible.firstOrNull { it.sessionId == overrideSessionId } ?: automatic
    }

    fun reset() { previous = emptyMap(); selectedId = null }
}
