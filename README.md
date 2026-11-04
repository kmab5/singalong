# singalong

An Android companion that keeps lyrics over your music player. Made by kmab. Requires Android 12 or newer; compile/target SDK 36.

## Status and downloads

The latest published version is **1.0.0 (9)**. This branch prepares **1.1.0-rc.2 (11)** for phone testing, with its version source in `version.properties`; it is not a published release. Download the stable APK from [the v1.0.0 release](https://github.com/kmab5/singalong/releases/tag/v1.0.0). See the [latest checkpoint](docs/validation/glow-selection-checkpoint.md) for verification and pending device checks.

[Website](https://kmab5.github.io/singalong/) · [GitHub Releases](https://github.com/kmab5/singalong/releases) · [Feedback](https://kmab5.github.io/kmab-feedback/?product=singalong)

Each GitHub Release includes `singalong.apk`, checksums, build metadata and the matching R8 crash mapping. Signed test builds are also available in successful CI runs. Install release-signed updates over the existing app to preserve data; debug APKs have a different signing key.

## What it does

- Reads song metadata and playback position from selected music apps. No microphone or playback-audio capture; audio recognition is deferred.
- Shows glass floating lyrics in compact, expanded or minimal mode, with bottom icon controls and a draggable edge handle. Tap expanded lyrics to open Now. Missing lyrics dock after a grace period; successful lyrics restore automatically unless you manually docked or closed the panel.
- Searches LRCLIB and lyrics.ovh, including conservative filename-derived title/artist/album hints. Preserves original metadata, source attribution and uncertainty. Lyrics are cached locally; transient failures retry with bounded attempts and provider cooldowns.
- Offers timed following, manual scrolling, optional estimated sync for untimed lyrics, and precise sync offsets. Estimated timing is explicitly approximate.
- Provides independent Now-tab text and current timed-line sizes, alignment, spacing, line animation, dimming and tap-to-focus controls. Optional Glow adds brighter highlights and slow word drift/scale between line timestamps; it does not add exact word timings. Stable line layout avoids rewrapping during focus changes. Floating size/opacity remain separate. Settings panels fill the available width.
- Prefers timed lyric matches and then the closest known duration for new automatic selections. Real timestamps start following automatically, including possible matches that remain labelled as uncertain. Explicit selections and personal versions are retained; untimed estimated sync is opt-in.
- Keeps a local library, favorites and play history. Supports TXT/LRC import, simple text editing, opt-in per-line timing, personal versions and JSON backup/restore. User edits never overwrite originals.
- Sorts saved lyrics by title, artist, album, recent playback or play count. The Library folder action scans a selected folder and subfolders, reads audio metadata/filenames locally and saves found lyrics. Existing saved matches are skipped, misses create no empty entries, and the scan supports progress/cancellation. Keep the app open; reselect the folder after an interruption to continue without saving duplicates.
- Optionally translates opened lyrics into English through MyMemory, progressively saving translations beside originals. Language identification uses on-device ML Kit; its SDK may send usage/performance metrics to Google. Read the [privacy details](PRIVACY.md).
- Offers a quiet tracking notification, About/feedback, and a next-launch prompt to review/save a recorded Java/Kotlin crash log. Feedback is submitted by the user; nothing is automatically uploaded.

Ten seconds of observed playback qualifies a history entry; pauses, buffering and seeking do not count as listening. Tracking resumes after a process restart only when the app is opened. Closing floating lyrics does not stop authorized background collection. Locks/screens-off suppress rendering, and Android may intentionally suppress overlays on sensitive screens. No universal player/device compatibility is claimed; see [compatibility](docs/validation/compatibility.md).

## Build and release signing

Use JDK 21 and existing Android SDK platform 36/build-tools 36.0.0. Dependencies and plugins are pinned in `gradle/libs.versions.toml`; JVM bytecode targets Java 17. Configure an ignored `local.properties` or `ANDROID_HOME` for your SDK.

On the current Windows workspace:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
.\gradlew.bat :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
```

Local SDK: `D:/Code/android-sdk`. Do not install emulators, system images or SDK packages for this workspace. Physical testing is handled by the user; compiling instrumentation tests does not mean they ran.

Debug output: `app/build/outputs/apk/debug/app-debug.apk`. Release output: `app/build/outputs/apk/release/app-release.apk`. Release builds use R8/resource shrinking and require the **existing** ignored signing key and credentials. GitHub uses that same key through encrypted secrets, then checks [the pinned certificate](release-certificate.sha256). See [release/version/CI instructions](docs/RELEASING.md) and [signing continuity](docs/validation/release-signing.md).

## Documentation, contributions and licensing

[Documentation index](docs/README.md) · [Contributing](CONTRIBUTING.md) · [Code of conduct](CODE_OF_CONDUCT.md) · [Security](SECURITY.md) · [Privacy](PRIVACY.md)

Original project code is licensed under [GNU GPL v3](LICENSE) with the user-approved [ML Kit linking permission](LICENSE-EXCEPTION). Third-party SDKs, fonts and retrieved content retain their own terms; see [notices](NOTICE.md). No lyrics or personal data are bundled in this repository.

The static site lives in `docs/`; PNG/SVG icon and wordmark exports are in `docs/assets/` and `public/docs/`. Fonts are self-hosted. See [site maintenance](docs/SITE.md). Historical phase plans/checkpoints remain dated evidence, clearly separated from the current status in the documentation index.
