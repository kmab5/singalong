# singalong

An Android companion for floating lyrics over your music player. Personal app; minimum Android 12 (API 31).

## current phase

The latest revision (`0.3.2`) fixes the response-cleanup crash when changing songs or retrying a request, adds Timed/Untimed version labels and keeps local crash details available for manual export. See the [stability checkpoint](docs/validation/stability-checkpoint.md) for evidence and phone checks. The signed, minified release APK is kept locally at `artifacts/singalong-release.apk`. Earlier [compact-controls](docs/validation/compact-controls-checkpoint.md), [feedback](docs/validation/feedback-checkpoint.md) and [P6](docs/validation/p6-checkpoint.md) checkpoints record prior baselines.

The static landing page is in [docs/index.html](docs/index.html); [GitHub Pages setup and site checks](docs/SITE.md) describe hosting and local preview. Downloadable SVG/PNG icons and wordmarks live in `docs/assets/` and `public/docs/`.

Included:

- kmab dark theme, bundled Inter variable/optical-display typography and PT Mono.
- Live reader, saved-library browsing, play history and settings.
- Explicit music/overlay/notification permission flows.
- User-started foreground service with a `specialUse` declaration on Android 14+.
- Selected music apps with locally loaded icons, automatic player arbitration and manual session override.
- Ten seconds of observed playing time qualifies a play; pauses, buffering and seeking do not add listened time.
- Room history/lyric versions and DataStore preferences, including migration from P0 preferences.
- LRCLIB lookup plus lyrics.ovh plain-lyric fallback, conservative matching, attribution and offline caching. Visible readers retry transient failures automatically with a bounded budget and server cooldowns.
- Plain/LRC parsing, playback interpolation, manual reading and version selection. Optional experimental sync estimates untimed lyric positions from line length and duration without changing original timestamps. Controls include 50ms/250ms/1s steps, direct offsets and line-to-position alignment.
- Process-local tracking gate: persisted preference alone cannot restart observation.
- Glass floating lyrics with native background blur where supported, compact/expanded and lyric-only minimal modes. Tap minimal lyrics to reveal controls; long-press and drag to move. Settings allow 12sp originals, 10sp subtext, 20% opacity, 160dp width and 56dp minimal height; low-opacity text gains a shadow. Readability depends on the background and selected sizes.
- A rectangular edge handle restores lyrics by tap or inward swipe; its position is independent of the full panel's dimensions.
- Animated navigation, lyric following/highlights, panel docking/expansion and status changes; respects Android animation settings.
- Separate close, manual-minimize and 15-second automatic pause-minimize behavior.
- Notification tap/action opens floating lyrics over the current app; separate actions open Singalong or stop tracking.
- Silent tracking notification with a channel-settings shortcut. Android controls status-bar icon visibility.
- About page with version, author, source/privacy details and the kmab feedback form prefilled for `singalong`. Reports and attachments are submitted by the user in the browser. A bounded private crash report can be exported manually; it excludes exception messages, song metadata and lyric text.
- Behavior-focused unit tests and compiled Room/Compose device tests; see the checkpoint for executed checks.

Search, favorites, TXT/LRC import preview, personal timed/untimed editing, version selection, protected cleanup and JSON backup/restore remain available. English subtext uses opt-in MyMemory translation with bundled on-device language detection, a local quota guard, reusable line cache and version-bound saved translations. Original lyrics publish first; cached/new English lines appear progressively, with at most two requests in flight. Same-version refreshes preserve active translation progress. No direct Genius/Musixmatch or key-based integration is included. Sample panels are explicitly labelled previews; runtime readers display actual data or explicit empty/error states.

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
- [Phone-feedback revision plan](docs/superpowers/plans/2026-11-02-feedback-overhaul.md)
- [Compact controls plan](docs/superpowers/plans/2026-11-02-compact-controls.md)

## local data and permissions

Room stores recordings, qualified occurrence IDs, history summaries, immutable lyric versions, translations, selected versions and timing adjustments. DataStore stores setup and display preferences. Media snapshots and live authorization stay in memory. Song title/artist go to LRCLIB and, when needed, lyrics.ovh. Only explicitly enabled translation sends opened lyric lines to MyMemory, which stores submitted text; the consent screen explains this. Language identification stays on-device. A bounded private preferences cache reuses completed translation segments and persists the daily request budget. Wi-Fi-only downloads are optional. There is no microphone, broad-storage, all-packages or accessibility-service permission.

No boot receiver or sticky restart is used. The listener may be rebound by Android, but observation remains off in a fresh process until an activity opens. Overlay visibility is not persisted. Screen-off/lock suppresses rendering without changing manual close/minimize decisions. Closing the panel leaves authorized collection running; stopping tracking ends the live session. Opening a saved song does not replace the live overlay song.

## assets

Brand colors, typography roles and lambda submark follow the kmab visual identity. The Android submark preserves the original SVG's path geometry. The app icon uses a lyric-cue motif; its PNG/SVG exports and outlined wordmark are available in `public/docs/` and on the landing page.

Inter is bundled from [rsms/inter](https://github.com/rsms/inter) and PT Mono from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/ptmono). See `licenses/Inter-OFL.txt` and `licenses/PTMono-OFL.txt`. Inter's optical-size axis provides the display/body distinction. PT Mono is used in its real regular weight, without a fabricated bold file.
