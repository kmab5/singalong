package com.kmab.singalong.tracking

import org.junit.Assert.*
import org.junit.Test

class TrackingStartRequestsTest {
    @Test fun supersedingStartWithFailedSetupClearsPending() {
        var pending = false
        val requests = TrackingStartRequests { pending = it }
        val original = requests.begin()
        val showLyrics = requests.begin()
        assertFalse(requests.isCurrent(original))
        assertTrue(pending)
        // The replacement cannot start because music access/player setup changed.
        requests.finish(showLyrics)
        assertFalse(pending)
    }

    @Test fun staleCompletionCannotClearNewerStart() {
        var pending = false
        val requests = TrackingStartRequests { pending = it }
        val old = requests.begin()
        val latest = requests.begin()
        requests.finish(old)
        assertTrue(pending)
        assertTrue(requests.isCurrent(latest))
        requests.finish(latest)
        assertFalse(pending)
    }

    @Test fun stoppingInvalidatesSuspendedStartAndAllowsFreshRequest() {
        var pending = false
        val requests = TrackingStartRequests { pending = it }
        val suspended = requests.begin()
        requests.cancel()
        assertFalse(pending)
        assertFalse(requests.isCurrent(suspended))
        val restarted = requests.begin()
        requests.finish(suspended)
        assertTrue(pending)
        assertTrue(requests.isCurrent(restarted))
    }
}
