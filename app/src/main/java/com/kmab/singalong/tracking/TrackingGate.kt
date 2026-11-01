package com.kmab.singalong.tracking

/** Process-local authorization, independent of the persisted user preference. */
class TrackingGate {
    var active: Boolean = false
        private set

    fun onAppOpened(savedEnabled: Boolean) {
        active = savedEnabled
    }

    fun disable() {
        active = false
    }
}
