# Releasing Singalong

Merge and publish releases only when the maintainer explicitly approves them. The maintainer authorized the first stable release, v1.0.0 (9), on 2026-11-02. Workflow preparation and signed CI artifacts alone are not a public release.

## One version source

`version.properties` supplies `versionName` and the monotonically increasing Android `versionCode` to Gradle. Use `MAJOR.MINOR.PATCH`, or `MAJOR.MINOR.PATCH-rc.N` for candidates. Every distributed build needs a larger code. APK names alone do not identify the installed version; check About and `build-info.json`.

For the next patch release, increment the version with a focused change, for example:

```sh
python3 tools/release_version.py set 1.0.1 --code 10
```

Use the next unused code if additional candidates have already consumed 10. Update `RELEASE_NOTES.md`, run checks, and commit. After an authorized merge to main, create an annotated tag matching the version exactly (`v1.0.1` in this example) and push that tag only when publication is authorized. Never reuse/move a published version tag. Ordinary branch checks allow the current published name/code unchanged; creating a new version still requires both to increase.

## CI checks and signing

`.github/workflows/ci.yml` runs debug tests/lint/build and compiles instrumentation tests on branch pushes and pull requests. It checks version rules and local documentation links. Trusted pushes to this repository also build/test/lint a signed candidate and upload an Actions artifact; pull-request jobs receive no signing credentials. Instrumentation execution still needs a device and is not claimed by this workflow.

Both CI and local builds use the same existing `.signing/singalong-release.jks` key. GitHub stores encrypted secrets named `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Base64 encodes the binary keystore; GitHub Secrets encryption protects its stored value. Passwords are supplied only in the job environment. The temporary keystore is removed in an always-run cleanup step. No workflow generates a key.

`tools/package_release.py` verifies APK signing, version name/code, and 16 KB ZIP alignment before publishing artifacts. It rejects a certificate different from [the pinned fingerprint](../release-certificate.sha256). The output directory contains `singalong.apk`, `mapping.txt`, `build-info.json` and `SHA256SUMS`. Keep the mapping with its matching APK; obfuscated crash build IDs distinguish older installs from newer fixes.

Local credentials remain in ignored `signing.properties`. The Gradle environment overrides are `SINGALONG_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Never print those values or commit them. Maintain an independent secure backup of the existing key and credentials. See [signing details](validation/release-signing.md).

## Tagged publication

`.github/workflows/release.yml` runs on `v*` tags. Before accessing signing secrets it verifies that the tagged commit is an ancestor of `origin/main`, the tag matches the version file, and version name/code exceed all earlier versioned tags. Tests/lint/build and certificate verification must pass. It then creates a GitHub Release, marks `-rc.N` tags as prereleases, and attaches the APK, mapping, metadata and checksums. An existing release causes failure; assets are never silently overwritten.

No production tag or release is created during pre-merge preparation. GitHub Pages should deploy from main `/docs`. The website finds a published stable release containing `singalong.apk`; before one exists or when GitHub is unavailable, it links to the actual releases page without advertising an unavailable APK.

The Ubuntu 24.04 runner image supplies Android SDK 36/build-tools 36.0.0. The workflows select JDK 21. They do not install emulators, system images or extra SDK packages. If runner images stop supplying the required SDK, update the build environment explicitly.
