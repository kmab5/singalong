# p0 platform feasibility

Updated 2026-11-01. **Source implemented; M0 verification deferred by explicit user direction.**

## user direction during implementation

The user chose to test the APK later, then requested: do not verify builds now, do not install Android simulation devices, and use the existing SDK at `D:/Code/android-sdk`. No further Gradle/test/device commands were run after that direction. No new simulator will be installed. No final APK is claimed.

Before that direction, the Gradle wrapper was generated from the existing Gradle 8.13 distribution. A deliberate red test run completed: three TrackingGate tests, two failures proving the missing enable/stop implementation. The gate was subsequently implemented. The green run and the final-source compilation have not been performed.

An emulator SDK download had started before the direction and was terminated. No emulator or AVD was installed or launched. The following partial files remain because automatic approval review rejected recursive cleanup outside the workspace:

- `D:/Code/android-sdk/.temp/PackageOperation01/emulator-windows_x64-16740729.zip` (partial download)
- `D:/Code/android-sdk/emulator/.installer/` (installer metadata)

Existing platforms 35, 36 and 36.1 were not replaced. No Android 31 platform/system image was installed.

## source and environment evidence

| Check | Result |
| --- | --- |
| Existing SDK | `D:/Code/android-sdk`; platform 36 available |
| Selected JDK | `D:/Code/java/jdk-21.0.12.1`; bytecode 17 |
| Wrapper generation | Gradle 8.13 wrapper task succeeded |
| Initial gate red tests | 3 executed, 2 expected failures before implementation |
| Final gate/unit tests | Deferred; not claimed passing |
| Final compile/lint/APK | Deferred; not claimed passing |
| Connected phone | None at initial inspection; user will test later |
| Emulator/device tests | Deferred; no AVD exists for this task |
| Compose previews | Four source previews plus in-app gallery; not rendered |
| SDK/API inspection | Existing Android 36 API confirms visual window-context overload |

## implementation choices

- `TrackingGate` is process-local; it never reads disk itself.
- `AppContainer` persists only the tracking preference, separate from gate/service state.
- `MainActivity.onStart` is the resumption entry point. Merely binding the notification listener cannot activate observation.
- A foreground service is started by visible UI or an explicit notification action; it returns `START_NOT_STICKY`.
- Android 14+ declares `specialUse` with a truthful description of media observation and the optional floating panel. No fake `mediaPlayback` type is used.
- Music-access revocation stops observation and the service; callbacks are unregistered and transient snapshots cleared.
- Overlay-only preview needs overlay permission but not notification-listener access. It has no real song lyrics and is labelled accordingly.
- The overlay uses its own display/window context and explicit Compose lifecycle/saved-state owners; close destroys the composition and removes the window.
- Screen-off closes the preview; lock-screen presentation is refused. P3 will supply richer pause/minimize/resume behavior.
- Notification permission is independent of music access. If notifications are denied, in-app opening remains available.
- P0 does not request Internet, microphone, accessibility-service or broad filesystem permissions.

## later physical-device checklist

Use the S25 FE / Android 16 after a verified APK has been produced.

1. Install and launch. Confirm the `now`, `library`, `history`, settings and design previews are readable.
2. Deny music access. Confirm start tracking is disabled and the library/design previews still open.
3. Allow music access through Android's notification-access settings. Allow notifications separately.
4. Start tracking, then play a song in a daily player. Return to Singalong; confirm title, artist, album and reported position/state match the player's exposed values. This checks observation, not lyric synchronization.
5. Pause/seek/resume. Confirm diagnostic callbacks update. Some players do not emit continuous position updates; interpolation is a later phase.
6. Allow display over other apps, open the floating preview, switch to the music app, drag the handle and close the panel. Confirm tracking notification remains if tracking was enabled.
7. Reopen with notification `show preview`. Test notification denial/re-enabling and overlay-permission revocation. Record any OEM-specific behavior.
8. Stop tracking. Confirm notification/window disappear and live media details clear.
9. Start again; force-stop from Android settings, then play music. Confirm Singalong does not restart. Launch Singalong; its saved preference should resume tracking without reopening the panel.
10. Reboot, play music before launching Singalong and confirm no automatic tracking. Open it and confirm resumption.
11. Open overlay-only preview with tracking off. Closing the preview should remove its service/notification. Lock/screen-off should close it too.
12. Check large system font, TalkBack, rotation and a bright underlying app. Record issues before calling M0 verified.

Each result needs device OS, player package/version, observed behavior and test date. No compatibility or battery claim is established yet.

## sources

- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [MediaSessionManager notification-listener access](https://developer.android.com/reference/android/media/session/MediaSessionManager)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Foreground-service background starts](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
