package com.kmab.singalong.tracking

/** Main-thread request ownership. Only the current request may clear pending startup. */
class TrackingStartRequests(private val pendingChanged: (Boolean) -> Unit) {
    var current: Long = 0
        private set

    fun begin(): Long {
        current++
        pendingChanged(true)
        return current
    }

    fun isCurrent(request: Long): Boolean = request == current

    fun finish(request: Long) {
        if (isCurrent(request)) pendingChanged(false)
    }

    fun cancel() {
        current++
        pendingChanged(false)
    }
}
