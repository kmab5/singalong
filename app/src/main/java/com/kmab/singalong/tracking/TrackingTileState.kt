package com.kmab.singalong.tracking

enum class TrackingTileAction { START, STOP, SETUP, WAIT }

/** Uses live session state, never a saved preference that could outlive the process. */
data class TrackingTileState(val action: TrackingTileAction, val subtitle: String) {
    companion object {
        fun from(tracking: Boolean, starting: Boolean, stopping: Boolean, ready: Boolean,
            musicAccess: Boolean, hasPlayers: Boolean): TrackingTileState = when {
            stopping -> TrackingTileState(TrackingTileAction.WAIT, "Stopping…")
            starting -> TrackingTileState(TrackingTileAction.WAIT, "Starting…")
            tracking -> TrackingTileState(TrackingTileAction.STOP, "Tracking on")
            !ready -> TrackingTileState(TrackingTileAction.WAIT, "Loading…")
            !musicAccess -> TrackingTileState(TrackingTileAction.SETUP, "Allow music access")
            !hasPlayers -> TrackingTileState(TrackingTileAction.SETUP, "Select music apps")
            else -> TrackingTileState(TrackingTileAction.START, "Tracking off")
        }
    }
}
