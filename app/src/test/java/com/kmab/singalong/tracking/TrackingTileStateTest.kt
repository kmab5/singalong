package com.kmab.singalong.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingTileStateTest {
    @Test fun preparedButStoppedSessionOffersStart() {
        assertEquals(TrackingTileAction.START, state().action)
    }

    @Test fun runningSessionCanAlwaysBeStoppedEvenIfSetupChanges() {
        assertEquals(TrackingTileAction.STOP, state(tracking = true, access = false, players = false).action)
    }

    @Test fun setupProblemsOpenAppInsteadOfPretendingToStart() {
        assertEquals(TrackingTileAction.SETUP, state(access = false).action)
        assertEquals(TrackingTileAction.SETUP, state(players = false).action)
    }

    @Test fun loadingAndPendingTransitionsIgnoreRepeatedClicks() {
        assertEquals(TrackingTileAction.WAIT, state(ready = false).action)
        assertEquals(TrackingTileAction.WAIT, state(starting = true).action)
        assertEquals(TrackingTileAction.WAIT, state(stopping = true).action)
    }

    private fun state(tracking: Boolean = false, starting: Boolean = false, stopping: Boolean = false,
        ready: Boolean = true, access: Boolean = true, players: Boolean = true) =
        TrackingTileState.from(tracking, starting, stopping, ready, access, players)
}
