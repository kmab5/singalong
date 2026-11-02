# November 2 phone-feedback revision

Historical `0.3.0-feedback` evidence. The [0.3.1 checkpoint](compact-controls-checkpoint.md) supersedes sizing, opacity, sync and translation behavior below.

## Implemented source

- New dark KMAB glass treatment, higher-contrast foregrounds, dedicated lyric stage, compact collection rows and focused settings subpages. Editor/backup/import/version functionality is retained.
- Contrast arithmetic at the minimum floating opacity (85%) over a white background gives 8.20:1 for primary text, 4.56:1 for secondary text, 4.61:1 for accent controls and 5.76:1 for translation text. Lower opacity is deliberately unavailable. Transparent containers explicitly provide their foreground color; action groups wrap at large font scales.
- Overlay now uses an owned translucent `Dialog` window. Full safe-area bounds are assigned before `show()`, replacing the old 1×1 initial layout and attachment-dependent resize. Background blur uses the public Android 12 Window API with a registered/removed availability listener and opaque fallback.
- A rectangular edge tab replaces the square minimized control. Dock/expand window geometry animates; tap/inward swipe restores; drag and accessible move actions reposition. Closing, screen lock, permission revocation and service teardown clean up the window and callbacks. Notification tap/show action targets the running service directly.
- Opening floating lyrics resumes following selected apps if paused, after the existing access checks. Generation checks prevent delayed startup from overriding Stop.
- Reader navigation, status/content transitions, line emphasis and synchronized scrolling animate. Platform animation scale is respected. Device frame pacing is not established by compiling these changes.
- Original lyrics appear before translation. Track/version changes and visibility cancel obsolete work. Improved metadata retries a cached miss. Transient retrieval failures retry at 2/5/10 seconds (four total attempts), with server cooldowns taking precedence; manual Retry bypasses ordinary negative caches.
- LRCLIB remains preferred; lyrics.ovh supplies attributed, untimed, possible-match fallback. A source outage never becomes a false definitive “not found”.
- MyMemory English subtext is opt-in, with fresh consent even if the old unavailable toggle was enabled. Bundled ML Kit identifies language locally. Repeated/completed segments are cached, anonymous usage is guarded at 5,000 characters per UTC day, and complete validated translations stay with their source version. Long segments over the provider's 500-byte limit and undetermined/English language results are skipped. Provider availability and translation accuracy vary.

## Verification

Verified on November 2 with JDK `D:/Code/java/jdk-21.0.12.1` and the existing SDK `D:/Code/android-sdk`:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
```

- Final command: **BUILD SUCCESSFUL**, 3m 32s. Local log: ignored `artifacts/feedback-build.log`.
- **49 unit tests per variant**, no failures, errors or skipped tests. Includes provider fallback/encoding, cached cooldowns, automatic retry budget/cancellation, metadata recovery, original-first translation, stale result rejection, translation quota/alignment and initial/docked geometry.
- Debug/release lint: **zero errors**, 21 dependency-update notices each.
- Debug, minified/resource-shrunk release and instrumentation-test APKs built. Instrumentation tests were compiled, not executed.
- Release `com.kmab.singalong`, version code **4**, name **0.3.0-feedback**, minimum SDK **31**, target **36**. Universal APK retains all four supported ABIs.
- Local install candidate: `artifacts/singalong-release.apk`, **9,483,737 bytes** (9.48 MB). Bundled language identification and its dependencies increase size relative to the previous 3.57 MB release; R8/resource shrinking remain enabled.
- APK SHA-256: `044EB257BB6D50F3170B79CC17F5F8FBD2DDF6EEB6BA32894120481BDB136F4C`.
- `apksigner verify --verbose --print-certs`: passed, existing RSA-3072 certificate SHA-256 `8bbe1da96dd01e32f971d516f1619b67c5a15b5222b40864ad3179f62aef6a04`. The key was reused, not replaced.
- `zipalign -c -P 16 4`: passed. All six packaged 64-bit native libraries also have ELF LOAD alignment of at least 16 KB.
- R8 mapping archived at `artifacts/mapping/044EB257BB6D50F3170B79CC17F5F8FBD2DDF6EEB6BA32894120481BDB136F4C.txt`.

No emulator, system image or new Android SDK package was installed. Feedback images, recordings, extracted review sheets, keys and artifacts remain ignored and are excluded from commits. MyMemory/Lingva smoke checks used only a synthetic greeting; the lyrics.ovh check used a synthetic nonexistent song.

## Remaining phone checks

1. Install the signed release as an update; existing library and preferences must survive.
2. Play music in Samsung Music, Spotify, SoundCloud, Musicolet and other selected players. Open floating lyrics from Now and the notification without opening Singalong over the player.
3. Dock/restore by tap and inward swipe on both edges; drag vertically; expand/compact; change width, lyric size and system font scale; rotate. Confirm readable text, native blur/fallback and smooth frame pacing.
4. Pause beyond 15 seconds, resume, manually dock, close, lock/unlock, revoke/regrant overlay permission and stop tracking. A manually docked panel should stay docked on resume; an automatically docked panel should reopen.
5. Seek and change songs during slow lookups/translations. Check offline/Wi-Fi recovery, automatic retry countdown and preserved original words.
6. Enable English subtext after reviewing consent; open a supported non-English song, then reopen offline. Test quotas/unavailability without losing original lyrics.
7. Exercise favorites/search/history, saved-version selection, import/edit (including a new untimed copy), backup/restore and protected cleanup. Room migration and Compose instrumentation tests are compiled but still need device execution.

This is a new source/build candidate, not a claim that the Samsung visual or overlay bugs are physically verified fixed.
