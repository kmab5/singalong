# Releasing

`version.properties` is the source of the Android version name and code. Each distributed app change needs a new name and a higher code. Current source targets **1.2.2 (22)**.

For the next release, use `python tools/release_version.py set VERSION --code CODE` with a later version and code. Don’t rerun the command with the current version.

Update release notes and the changelog, run the [build checks](BUILDING.md), then commit and push main. A new main-branch version automatically runs tests, builds, signs, tags and publishes. Matching explicit `v*` tags and manual dispatch are also supported. Website and documentation changes can keep the existing version.

## Publication rules

- An unpublished version builds and publishes, creating its tag if needed.
- A complete release with unchanged app source is skipped.
- Changed app source under a published version requires a version bump.
- Conflicting tags or incomplete releases fail for inspection.

Never move a published tag or replace its assets. Runs queue sequentially. Retry the original run if an unpublished tag points to an older commit; don’t move that tag.

Stable versions become the latest release; `-rc.N` versions are prereleases. Pushing a commit or tag alone doesn’t prove publication succeeded.

## Release contents

One release contains exactly five public APK attachments:

| File | Use |
| --- | --- |
| `singalong.apk` | Universal; use when unsure |
| `singalong-arm64-v8a.apk` | 64-bit ARM |
| `singalong-armeabi-v7a.apk` | 32-bit ARM |
| `singalong-x86.apk` | 32-bit x86 |
| `singalong-x86_64.apk` | 64-bit x86 |

All APKs share the app version and signing identity. R8 mapping, build metadata and checksums stay in CI artifacts for **90 days**, outside public release attachments. Keep a separate durable copy of the mapping and metadata for any release that needs longer crash support.

Local packaging separates `artifacts/release/apks/` from `artifacts/release/diagnostics/`. Public releases upload only `apks/*.apk`; candidate CI artifacts can include both directories.

## Signing and checks

Every packaged APK must match [release-certificate.sha256](../release-certificate.sha256). CI uses the existing key through `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` secrets. Temporary signing files are removed after the job. Pull requests receive no signing secrets.

Local signing uses ignored `.signing/singalong-release.jks` and `signing.properties`. Keep a secure independent backup; never commit or print credentials.

Release checks include debug/release unit tests and lint, instrumentation-test compilation, signed builds and packaging verification. Compilation doesn’t establish device behavior; see [testing](TESTING.md).

GitHub Pages serves main `/docs`. The universal download always falls back to the latest stable APK; architecture links appear when the release provides them. See [website maintenance](SITE.md).
