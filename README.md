# singalong

An Android companion for floating lyrics over your music player. Personal app; minimum Android 12 (API 31).

## current phase

P0–P5 source is implemented. P6 automated checks now pass: **35 unit tests, debug lint (zero errors), debug/release builds and instrumentation-test APK compilation**. The signed release APK is available locally at `artifacts/singalong-release.apk`. Physical-device, migration and instrumented test execution remain pending your phone testing. Online translation remains explicitly unavailable. See the [P6 checkpoint](docs/validation/p6-checkpoint.md).

Included:

- KMAB dark theme, bundled Inter variable/optical-display typography and PT Mono.
- Live reader, saved-library browsing, play history and settings.
- Explicit music/overlay/notification permission flows.
- User-started foreground service with a `specialUse` declaration on Android 14+.
- Selected music apps, automatic player arbitration and manual session override.
- Ten seconds of observed playing time qualifies a play; pauses, buffering and seeking do not add listened time.
- Room history/lyric versions and DataStore preferences, including migration from P0 preferences.
- LRCLIB lookup with conservative matching, attribution, offline cache, request pacing and bounded retries.
- Plain/LRC parsing, playback interpolation, manual reading, version selection and saved timing adjustments.
- Process-local tracking gate: persisted preference alone cannot restart observation.
- Floating current lyrics, expanded/manual reading, drag/resize, text size and opacity settings.
- Separate close, manual-minimize and 15-second automatic pause-minimize behavior.
- Notification actions to reopen lyrics or stop tracking.
- Behavior-focused unit, Room and Compose tests for a later verification run.

P4 adds search, favorites, TXT/LRC import preview, personal timed/untimed editing, version selection, protected cleanup and JSON backup/restore. P5 adds opt-in translation settings, version-bound validated cache storage and subordinate English rendering; online translation is explicitly unavailable. No Genius/Musixmatch scraping or key-based provider is included. P0 sample panels remain design-preview source only; runtime screens display actual data or explicit empty/error states.

See the [P4–P5 checkpoint](docs/validation/p4-p5-checkpoint.md) for backup semantics, translation research and pending validation.

## build and release signing

Pinned versions: Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin/Compose compiler 2.2.21, Compose BOM 2025.10.00, Activity 1.11.0, Lifecycle 2.9.4, Core 1.17.0, coroutines 1.10.2. Compile/target SDK 36; minimum SDK 31. JVM bytecode targets Java 17; use JDK 17 or 21 to run Gradle (the machine's default Java 26 is not the selected toolchain).

Local SDK is `D:/Code/android-sdk`. `local.properties` is intentionally ignored. Android Studio can generate it, or set `sdk.dir=D\:\\Code\\android-sdk` locally.

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

After a successful build, the debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. A signed release is also generated at `app/build/outputs/apk/release/app-release.apk`; see [signing continuity](docs/validation/release-signing.md).

For an authorized physical-device test:

```powershell
adb devices -l
adb install -r artifacts/singalong-release.apk
```

**Do not install an emulator, system image or additional SDK package without a new user request.** Automated build checks are authorized and passed; device testing is handled by the user. Instrumented tests have compiled but have not run; they require a debug-signed app installation separate from the release testing workflow.

## development documents

- [Confirmed design](docs/superpowers/specs/2026-11-01-singalong-design.md)
- [Implementation tasks](docs/superpowers/plans/2026-11-01-singalong.md)
- [Phases and checkpoints](docs/superpowers/plans/2026-11-01-development-phases.md)
- [P0 status and phone checklist](docs/validation/platform-feasibility.md)
- [P1–P3 implementation and pending checks](docs/validation/p1-p3-checkpoint.md)
- [Provider feasibility](docs/validation/providers.md)
- [Design baseline](docs/design/p0-visual-baseline.md)

## local data and permissions

Room stores recordings, qualified occurrence IDs, history summaries, immutable lyric-version payloads, selected versions and timing adjustments. DataStore stores setup and display preferences. Media snapshots and live authorization stay in memory. Internet/network-state permission supports lyric lookup; launcher queries support player selection. There is no microphone, broad-storage, all-packages or accessibility-service permission. Song title and artist are sent to LRCLIB when network lookup is needed. Wi-Fi-only downloads are optional.

No boot receiver or sticky restart is used. The listener may be rebound by Android, but observation remains off in a fresh process until an activity opens. Overlay visibility is not persisted. Screen-off/lock suppresses rendering without changing manual close/minimize decisions. Closing the panel leaves authorized collection running; stopping tracking ends the live session. Opening a saved song does not replace the live overlay song.

## assets

Brand colors, typography roles and lambda submark come from [kmab-brand](https://github.com/kmab5/kmab-brand). The Android submark preserves the SVG's path geometry. The app icon uses the lyric-cue motif rather than altering the KMAB logo.

Inter is bundled from [rsms/inter](https://github.com/rsms/inter) and PT Mono from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/ptmono). See `licenses/Inter-OFL.txt` and `licenses/PTMono-OFL.txt`. Inter's optical-size axis provides the display/body distinction. PT Mono is used in its real regular weight, without a fabricated bold file.
