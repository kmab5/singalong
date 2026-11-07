# Releasing Singalong

The maintainer authorized v1.2.0 tagging/publication and automatic versioned main-branch releases on November 7, 2026. Work stays on main. The same existing signing identity is mandatory for every APK update.

## One version source

`version.properties` supplies the Gradle `versionName` and monotonically increasing Android `versionCode`. Names use `MAJOR.MINOR.PATCH` or `MAJOR.MINOR.PATCH-rc.N`. Current source is **1.2.1 (21)**; the next distributed app build needs a later name and an unused code above 21.

Use `python tools/release_version.py set <version> --code <code>`, update `RELEASE_NOTES.md` and the changelog, run checks, then commit and push main. New app versions publish automatically. You may explicitly create and push an annotated matching tag; never move/reuse a published tag. Website/documentation changes do not need a new app version or tag.

APK filenames do not establish installed version: inspect About and the release's `build-info.json`. A GitHub Actions candidate artifact is not a public release.

## Main, tag and manual publication

`.github/workflows/release.yml` runs on main pushes, `v*` tag pushes and manual dispatch. The plan job checks main ancestry, version rules, documentation links and release-helper tests before deciding whether to publish:

- An unpublished version builds, verifies, tags the checked commit if necessary, then creates its release.
- An existing complete release with unchanged app source is skipped. This makes repeated main/tag runs and website-only pushes harmless.
- Changed app source with the same published version fails and requires a version bump.
- A conflicting tag or incomplete release fails for inspection. Tags and assets are never silently replaced.

Publication runs queue sequentially, including simultaneous main/tag pushes. The workflow uses GitHub's [expanded concurrency queue](https://github.blog/changelog/2026-05-07-github-actions-concurrency-groups-now-allow-larger-queues/) so a later push does not discard a pending release. A retry of a failed build can use manual dispatch on main, provided the unpublished tag still identifies that commit. If main has moved beyond an unpublished tag, rerun the original tagged run; do not move the tag.

Release checks run debug/release unit tests and lint, compile instrumentation tests and assemble the signed release. `tools/package_release.py` validates version, certificate, R8 mapping and 16 KB ZIP alignment. Stable tags become the latest release; `-rc.N` tags become prereleases. Assets are `singalong.apk`, `mapping.txt`, `build-info.json` and `SHA256SUMS`. Keep each crash mapping with its matching APK.

## CI and signing continuity

`.github/workflows/ci.yml` also runs ordinary branch/PR checks. Trusted upstream pushes build a signed candidate; pull requests receive no signing credentials. Instrumentation compilation does not mean device tests executed. Physical testing remains the maintainer's responsibility.

GitHub stores the existing key in encrypted secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Base64 encodes the keystore; GitHub Secrets encryption protects its stored value. Passwords are passed only in the job environment. The temporary key is removed in an always-run cleanup step. No workflow generates a key.

Every packaged release must match [release-certificate.sha256](../release-certificate.sha256). Local signing uses ignored `.signing/singalong-release.jks` and `signing.properties`; never commit or print either. Preserve a secure independent backup. See [signing details](validation/release-signing.md).

The Ubuntu 24.04 runner supplies SDK 36/build-tools 36.0.0 and workflows select JDK 21. They do not install emulators, system images or extra SDK packages. If the runner image stops supplying that SDK, update the environment explicitly.

## Website and final verification

GitHub Pages deploys main `/docs`. The site links directly to the latest stable `singalong.apk`, even without JavaScript. Its optional API request binds the version and exact asset together. Keep that asset name consistent.

After pushing, verify the release job, published assets, APK certificate/version/checksums and Pages deployment. Do not report publication merely because a tag or commit was pushed. Record outcomes in a dated validation checkpoint.
