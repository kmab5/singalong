# Quick Settings tracking tile implementation plan

**Goal:** Add the user-confirmed tile beside Wi-Fi/Bluetooth, leaving the existing notification unchanged. Ship 1.2.1 (21) from main with the existing release key.

**Architecture:** A system-bound TileService observes existing container/settings state only while listening. A small Android-independent presentation policy chooses the label/action from actual tracking state and setup readiness. Explicit clicks use the same enable/disable methods as the app; merely binding the tile never authorizes tracking. Startup/shutdown are exposed as transient states to avoid overlapping commands. Locked-device clicks use Android's unlock flow. Missing setup opens Singalong using the API-appropriate activity handoff.

**Constraints:** SDK 31–36; no new runtime permissions, dependencies, microphone use or background polling. BIND_QUICK_SETTINGS_TILE protects the exported system service; it is not a requested app permission. Stay on main. Keep the notification unchanged. The tile is added through the shade's Edit controls.

- [x] Add regression tests for off/on, missing permissions/player selection, initial settings loading and transitional commands. Verify they fail before implementation.
- [x] Implement the presentation policy and TileService, lifecycle-scoped observation and locked/setup handling. Integrate explicit tile start authorization and shared startup/shutdown state with the existing container/service.
- [x] Bump to 1.2.1 (21); update release notes, current guides and website setup copy. Record tests separately from physical-device checks.
- [x] Run debug/release unit tests, lint and builds, compile instrumentation tests, verify existing signing certificate and mapping, and review the diff.
- [ ] Commit/tag/push main and monitor publication.

Reference: [Android Quick Settings guidance](https://developer.android.com/develop/ui/views/quicksettings-tiles). Use non-active listening so reopening the shade reflects process restarts without relying on a stale cached active state. Cancel state collection when the panel stops listening; no tracking restarts from observation.

Phone checks (maintainer): add/remove tile; toggle from another app; stop via the unchanged notification and check tile state; repeat after process death; verify missing music access/player selection, unlock cancellation, and rapid taps.
