package com.kmab.singalong.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingGateTest {
    @Test fun newProcessCannotResumeFromPersistedPreferenceAlone() {
        val first = TrackingGate()
        first.onAppOpened(savedEnabled = true)
        assertTrue(first.active)
        val restarted = TrackingGate()
        assertFalse(restarted.active)
        restarted.onAppOpened(savedEnabled = true)
        assertTrue(restarted.active)
    }

    @Test fun openingWithDisabledPreferenceDoesNotStartTracking() {
        val gate = TrackingGate()
        gate.onAppOpened(savedEnabled = false)
        assertFalse(gate.active)
    }

    @Test fun explicitStopRevokesCurrentProcessAuthorization() {
        val gate = TrackingGate()
        gate.onAppOpened(savedEnabled = true)
        assertTrue(gate.active)
        gate.disable()
        assertFalse(gate.active)
    }
}
