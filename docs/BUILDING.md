# Building

Use JDK 21, Android SDK platform 36 and build-tools 36.0.0. The minimum runtime is Android 12; JVM bytecode targets Java 17. Dependencies are pinned in `gradle/libs.versions.toml`.

Set `JAVA_HOME` and either `ANDROID_HOME` or an ignored `local.properties` containing `sdk.dir`.

## Debug build

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/check_docs.py
```

On Windows, use `gradlew.bat` and `python`. The universal debug APK is `app/build/outputs/apk/debug/app-universal-debug.apk`; architecture variants are in the same directory. Debug signing cannot update a release-signed installation.

## Signed release build

Release signing requires the existing ignored `.signing/singalong-release.jks` and `signing.properties`. Do not generate a replacement key or commit credentials.

```sh
./gradlew :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
python3 tools/package_release.py
```

The universal release build is `app/build/outputs/apk/release/app-universal-release.apk`. Packaging writes five APKs to `artifacts/release/apks/` and matching metadata, checksums and R8 mapping to `artifacts/release/diagnostics/`. It verifies versions, ABI contents, certificates, mapping and 16 KB ZIP alignment.

Release builds use shrinking and optimization. Preserve the matching mapping for crash investigation. See [releasing](RELEASING.md) for distribution and [testing](TESTING.md) for checks that need a phone.


## Google Play bundle

Use `./gradlew :app:bundleRelease -PplayBundle=true` with the configured JDK and existing signing credentials. This disables ABI APK splits for the invocation; ordinary GitHub release builds still produce universal and architecture APKs. Output is `app/build/outputs/bundle/release/app-release.aab`. Google Play's app-signing certificate must match the existing release identity to preserve updates from GitHub installs; the upload certificate alone is insufficient.

Play bundle builds use `.signing/upload-signing.properties` (the separate upload key). Ordinary APK builds use `signing.properties` (the original app-signing key). The Play property selects both bundle packaging and upload signing. Optional Play CI overrides use `PLAY_SINGALONG_KEYSTORE_PATH`, `PLAY_ANDROID_KEYSTORE_PASSWORD`, `PLAY_ANDROID_KEY_ALIAS` and `PLAY_ANDROID_KEY_PASSWORD`; APK signing variables cannot override the Play upload identity.


For 1.3.2, the local Android build uses the already-installed NDK 28.2.13676358 and requests full native symbols. Precompiled dependency libraries may already be stripped; Gradle cannot reconstruct their debug information. Check `app/build/outputs/native-debug-symbols/release/` rather than assuming a symbols ZIP exists. Local deliverables are collected in ignored `artifacts/local/` (APKs) and `artifacts/playstore/` (Play bundle and available symbols). Keep R8 mappings separately under `artifacts/diagnostics/`.
