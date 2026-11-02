# 0.3.2: request cancellation stability

> Historical checkpoint: results and APK versions below describe that revision. See the [current documentation index](../README.md) and [pre-merge checkpoint](premerge-checkpoint.md) for later changes and verification.

## Confirmed cause and source correction

The user's Musicolet crash log shows `NetworkOnMainThreadException` while a song change cancels a pending lookup. Its R8 map ID `c3d42ed4476e4fabc4cdb17f1cae83af8f5c84e7d57b512bcf594100d6110226` matches the archived 0.3.1 mapping. The obfuscated `h7.m.b:461` frame resolves to `LrclibClient.search` at the response `use` boundary; the close chain includes OkHttp's HTTP/2 framing source and a TLS write/flush. This confirms network cleanup running on `Dispatchers.Main.immediate`, independent of whether the overlay is open.

Both HTTP clients previously returned a live `Response` to the caller coroutine, read it under an IO context and closed it on the caller thread. Early error results left unread bodies, and canceled requests also reached this cleanup boundary. `HttpResponseReader` now reads, parses and closes entirely within OkHttp's callback worker, returning only parsed data. Cancellation still cancels the socket. Callback failures are delivered to the owning coroutine and expected provider errors remain classified there. LRCLIB cooldown state is updated only by the coroutine holding its mutex, not by a callback finishing after cancellation.

Before the correction, two guarded-body regression tests failed with `Response body closed on the caller/UI thread`. Both passed after the correction. A further test cancels a blocked body read, verifies closure happens off the caller thread, then completes a lookup for the next song.

## Other changes

- All app branding/accessibility labels use lowercase kmab. The design-repository action was removed from About.
- Both version selectors and the editor's selected-version button show Timed or Untimed according to stored timestamps. Experimental estimation does not label untimed lyrics as timed.
- The app retains one bounded crash report in private no-backup storage. It contains exception types, code locations, timestamp and app/API versions; messages, lyric text, metadata and thread names are omitted. Android's original uncaught-exception handler still runs. About → Save crash details exports only when requested; nothing uploads automatically. This records future uncaught Java/Kotlin crashes, not historical crashes or native process deaths.

## Android verification

Run with existing JDK `D:/Code/java/jdk-21.0.12.1` and SDK `D:/Code/android-sdk`:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
```

- Final run: **BUILD SUCCESSFUL**, 1m 48s. Local log: `artifacts/stability-final-build.log`.
- **62 tests per variant**, zero failures/errors/skips. Includes original HTTP-threading failures, cancellation/recovery, privacy exclusions and bounded/cyclic crash causes.
- Debug/release lint: zero errors; 21 dependency-update warnings each.
- Debug/release/instrumentation APKs compiled. Instrumentation tests were **not run**; no emulator or SDK package was installed.
- Signed, minified/resource-shrunk release: **0.3.2 (6)**, minimum API 31, target 36; **9,549,265 bytes** (9.55 MB), local `artifacts/singalong-release.apk`.
- APK SHA-256: `01A7FF9CD4976BC398CF57FA72D8AC68FA87516A0807159849C18F090D464E67`.
- Signature verification passed with the unchanged RSA-3072 certificate SHA-256 `8bbe1da96dd01e32f971d516f1619b67c5a15b5222b40864ad3179f62aef6a04`.
- 16 KB ZIP alignment passed; all six 64-bit native libraries have ELF LOAD alignment of at least 16 KB.
- Mapping archived as `artifacts/mapping/01A7FF9CD4976BC398CF57FA72D8AC68FA87516A0807159849C18F090D464E67.txt`.

## Phone checks still required

Install as an update, then rapidly change Musicolet songs before lookups finish, both inside/outside the app and with floating lyrics open/closed. Exercise missing songs and automatic/manual retries on a slow connection. Confirm subsequent songs still load. Check Timed/Untimed labels against actual LRC/plain versions. If another crash occurs, reopen About and save its details for the feedback form. Physical verification of this build remains with the user.
