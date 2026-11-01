# singalong

An Android companion for floating lyrics over your music player. Personal app; minimum Android 12 (API 31).

## current phase

P0 foundation source is implemented. **The final source has not been built or device-verified**, at the user's request. No installable P0 APK is claimed yet.

Included:

- KMAB dark theme, bundled Inter variable/optical-display typography and PT Mono.
- `now`, `library`, `history`, permission settings and design-preview screens.
- Explicit music/overlay/notification permission flows.
- User-started foreground service with a `specialUse` declaration on Android 14+.
- Media-session diagnostics: player, song, artist, album, reported position and state.
- Process-local tracking gate: persisted preference alone cannot restart observation.
- A draggable, closable overlay with clearly labelled original sample text.
- Notification actions to reopen the preview or stop the session.
- Unit tests for restart/stop behavior and Compose navigation/permission tests for a later verification run.

This build **does not fetch lyrics, record history, select eligible players, synchronize lyrics or translate text**. Media diagnostics are transient and do not read notification messages. The sample panel is a layout feasibility preview, not song content. The subsequent phases add collection and real lyrics.

## build when verification resumes

Pinned versions: Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin/Compose compiler 2.2.21, Compose BOM 2025.10.00, Activity 1.11.0, Lifecycle 2.9.4, Core 1.17.0, coroutines 1.10.2. Compile/target SDK 36; minimum SDK 31. JVM bytecode targets Java 17; use JDK 17 or 21 to run Gradle (the machine's default Java 26 is not the selected toolchain).

Local SDK is `D:/Code/android-sdk`. `local.properties` is intentionally ignored. Android Studio can generate it, or set `sdk.dir=D\:\\Code\\android-sdk` locally.

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

After a successful build, the debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. This is a future output path, not a currently verified artifact.

For an authorized physical-device test:

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

**Do not install an emulator, system image or additional SDK package without a new user request.** Build and device verification are currently deferred. Use the existing SDK and the user's phone when they are ready.

## development documents

- [Confirmed design](docs/superpowers/specs/2026-11-01-singalong-design.md)
- [Implementation tasks](docs/superpowers/plans/2026-11-01-singalong.md)
- [Phases and checkpoints](docs/superpowers/plans/2026-11-01-development-phases.md)
- [P0 status and phone checklist](docs/validation/platform-feasibility.md)
- [Provider feasibility](docs/validation/providers.md)
- [Design baseline](docs/design/p0-visual-baseline.md)

## local data and permissions

P0 persists only tracking preference and whether notification permission has been requested, in app-private preferences. Library persistence and DataStore migration belong to P1. Media snapshots stay in memory and clear when tracking stops. The application has no Internet, microphone, broad-storage or accessibility-service permission. Network research happened during development, not inside this app.

No boot receiver or sticky restart is used. The listener may be rebound by Android, but observation remains off in a fresh process until an activity opens. An overlay-only preview can run without music access; closing it also removes its foreground service if tracking is off. Screen-off closes the preview. Full P3 minimize/resume behavior is intentionally absent.

## assets

Brand colors, typography roles and lambda submark come from [kmab-brand](https://github.com/kmab5/kmab-brand). The Android submark preserves the SVG's path geometry. The app icon uses the lyric-cue motif rather than altering the KMAB logo.

Inter is bundled from [rsms/inter](https://github.com/rsms/inter) and PT Mono from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/ptmono). See `licenses/Inter-OFL.txt` and `licenses/PTMono-OFL.txt`. Inter's optical-size axis provides the display/body distinction. PT Mono is used in its real regular weight, without a fabricated bold file.
