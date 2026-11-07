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
