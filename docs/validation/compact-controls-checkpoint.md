# Compact controls: 0.3.1

## Implemented source

- Minimal floating mode shows only lyrics/subtext. Tap reveals controls; long-press drag moves the window. Accessible actions reveal, dock, close and move it. The existing edge handle remains available. Mode and minimal height persist; revealing controls temporarily grows the window without rewriting saved dimensions.
- Originals 12–32sp, English 10–22sp (capped below original size), opacity 20–96%, width 160–560dp, ordinary height 120–640dp and minimal height 56–240dp. Blur availability follows Android; lower opacity is honored with a text shadow. Very small settings and busy backgrounds can reduce readability.
- Sync controls render for a single version. Experimental untimed sync is explicitly opt-in per version, estimates positions from nonblank character weights and song duration, and never writes generated timestamps to lyric records. It requires playback position and duration, may drift across intros/instrumentals, and can be disabled from the floating panel or Controls tab. Existing uncertain timed versions also retain their opt-in. Saved offset controls offer 50ms/250ms/1s steps, numeric seconds and alignment of a selected line with the current playback position.
- Installed player icons load off the UI thread. Secondary text actions now have raised, outlined surfaces and native touch targets. The Now source/player footer has additional bottom padding.
- Translation publishes validated partial results immediately, restores all cached segments first, reuses repeated lines and limits HTTP concurrency to two. Complete version records are still saved only after validation; incomplete segments remain reusable offline. Same-version refreshes preserve ongoing translation. Cancellation, quota guards and original-first display remain intact.
- Settings → About includes build, author, platform, sources, privacy, credits and feedback. The browser form receives only `product=singalong`, `source=android`, version, type and `ctx_screen=about`. No lyrics, screenshots, device identifiers or reports are submitted automatically. Integration follows the local KMAB feedback documentation. Public registry access failed with TLS/network errors; live product registration and successful form submission remain unverified.
- Tracking explicitly disables sound, vibration, lights and badges; it retains the existing channel and valid foreground-service importance. A shortcut opens channel settings. Existing user settings are respected, and status-bar icon hiding is controlled by Android/Samsung.

## Service decisions

Official [Google Cloud Translation setup](https://docs.cloud.google.com/translate/docs/setup) requires authentication and billing setup. [DeepL API](https://www.deepl.com/en/developers) requires an API key even for its free plan. [Deep-translator](https://deep-translator.readthedocs.io/en/latest/usage.html) is a Python client library, not another hosted translation API. This release stays within the approved keyless-service scope and improves the existing MyMemory path; it does not claim a new Google/DeepL integration or guaranteed provider latency.

Android documents [foreground-service notification requirements](https://developer.android.com/develop/background-work/services/fgs) and [notification importance](https://developer.android.com/reference/android/app/NotificationManager#IMPORTANCE_MIN). A silent LOW channel is appropriate for the tracking service; forcing MIN can trigger Android's own higher-priority service disclosure. This release does not use an invisible icon or replace channels to override user choices.

## Verification

Verified November 2 using the existing `D:/Code/android-sdk` and `D:/Code/java/jdk-21.0.12.1`:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
```

- Final command: **BUILD SUCCESSFUL**, 4m 45s. Ignored local log: `artifacts/compact-final-build.log`.
- **57 unit tests per variant**, zero failures, errors or skipped tests. Coverage includes single-version estimated sync on/off and saved adjustments, precise offset/alignment, unchanged original timestamps, compact/revealed geometry, progressive translations with a deliberately blocked second request, offline partial cache, stale result rejection, same-version refresh continuity and feedback URL encoding/context.
- Debug/release lint: **zero errors**, 21 dependency-update warnings each.
- Debug, release and instrumentation APKs built. Nine Room/Compose device tests compiled, including minimal-mode control reveal; **none were executed on a device**.
- Release package `com.kmab.singalong`, version **0.3.1 (5)**, minimum API 31, target API 36. R8 and resource shrinking remain enabled; universal APK retains all four ABIs.
- Local APK: `artifacts/singalong-release.apk`, **9,532,881 bytes** (9.53 MB).
- APK SHA-256: `F5F4CA42795117BD77A204E6F4B8389F4A6B19D81EA955B1D8C79A7DB86B8495`.
- `apksigner verify --verbose --print-certs` passed. Existing RSA-3072 certificate SHA-256: `8bbe1da96dd01e32f971d516f1619b67c5a15b5222b40864ad3179f62aef6a04`; the signing key was reused.
- `zipalign -c -P 16 4` passed. ELF LOAD alignment is at least 16 KB for all six packaged 64-bit native libraries.
- Mapping archived at `artifacts/mapping/F5F4CA42795117BD77A204E6F4B8389F4A6B19D81EA955B1D8C79A7DB86B8495.txt`.

No emulator, system image or SDK package was installed. No physical-device behavior or live translation speed is claimed verified by these build/tests.

## Phone checks still required

1. Update the existing release installation; check library, translations and preferences survive.
2. Enter minimal mode from the floating More menu and Settings. Tap to reveal, hide controls, expand, dock/restore, long-press drag and close. Check TalkBack actions and both screen orientations, including the smallest sizes and large system fonts.
3. On a song with just one plain lyric result, enable/disable experimental sync in the floating panel and Controls. Seek, pause/resume, change songs and revisit the version. Try 50ms offsets, direct negative values and line alignment. Confirm original content/timestamps stay intact.
4. Check player icons, secondary button spacing, Now footer and About links. Open the feedback form, confirm Singalong/version prefill and submit only a report you intend to send; product registration must exist in KMAB Feedback.
5. Open a non-English song on a slow connection: originals should remain readable, English lines should arrive incrementally, and interrupted cached lines should reappear offline. Test rapid track changes, toggling translation and network recovery.
6. Verify tracking has no sound/vibration/badge. Use Music access → Quiet tracking notification for channel settings; status-bar icon visibility remains a phone setting. Confirm notification actions still open lyrics and stop tracking.

Feedback media, signing credentials, build output and mappings remain local and ignored. Physical visual/gesture/frame-pacing verification belongs to the phone test, not the successful compilation of this source.
