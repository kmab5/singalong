# Pre-merge release candidate

Historical candidate checkpoint for 1.0.0-rc.1 (8). The maintainer subsequently authorized merging and publishing v1.0.0 (9) on 2026-11-02. The evidence below belongs to the candidate commit; stable release verification is recorded in its GitHub Actions run and release artifacts.

## Source changes and diagnosis

All four supplied NetworkOnMainThreadException traces contain the 0.3.1 R8 map ID `c3d42ed4476e4fabc4cdb17f1cae83af8f5c84e7d57b512bcf594100d6110226`. Archived 0.3.2 and 0.3.3 mappings differ. The user subsequently confirmed About shows 0.3.3 (7) and said the original crash appears fixed. No further networking rewrite was justified by those older traces. Existing guarded cleanup/cancellation tests remain in the suite.

New uncaught Java/Kotlin reports prompt review/export on the next launch. Acknowledging a report prevents repeated prompts for the same report; a later crash is eligible again. The universal feedback form does not accept automatic log attachments through its URL contract, so the flow saves a reviewed log and opens the form for the user to attach and submit it. No automatic report upload is claimed.

Floating options are bottom icons; narrow widths scroll the row. Tapping full lyrics/title opens Now. Lookup failures dock after eight seconds without active retry, and automatic docking retains reader demand so successful lyrics can restore the panel. Manual docking/close remains respected. Unknown playback state is not treated as a pause. Transient window detach/token errors retain requested visibility and are reconciled by the owning service; real permission loss closes the window. Lock-screen hiding remains enforced. Android can intentionally suppress overlays on sensitive screens, including some Settings screens; this is not bypassed.

Now has independent size, alignment, spacing, inactive-line dimming, None/Fade/Gentle focus animation, and tap-to-focus controls. These do not alter saved lyrics or playback timing.

Pre-merge work includes version/tag validation, signed CI artifacts, tag-driven releases guarded by main ancestry, a website release link, GPL v3 with the user-approved ML Kit linking exception, conduct/contributing/security/privacy/notices, and a documentation index distinguishing historical evidence from current guidance.

## Evidence

Verified source commit: `bff224f94826375406f315b659ca628c098ed6b3`, version **1.0.0-rc.1 (8)**. These are automated results, not physical-device acceptance.

- Local Gradle debug/release unit tests, debug/release lint, debug APK, instrumentation APK compilation and signed release APK assembly passed. Each unit-test variant ran 82 tests with zero failures, errors or skips. Lint reported zero errors and 22 warnings per variant: 21 dependency-update notices and one `UseKtx` style suggestion.
- Five release-tool tests, version/tag validation, 35-file Markdown link check, and actionlint workflow validation passed.
- Website browser checks passed at widths 320, 390, 768, 1024 and 1440, including keyboard navigation, reduced motion, no JavaScript, release selection and network failure fallback. GitHub Pages deployment passed, and the live page returned HTTP 200 with its release download control.
- APK signature, package/version, 16 KB ZIP alignment and matching R8 mapping checks passed. All packaged 64-bit native library load segments have at least 16 KB alignment. The local APK is 9,598,421 bytes.
- Local APK SHA-256: `9a04bc2f080ca73bac00d16493f07bbff6acb8d79fa5197b5b56d7b9310465ec`.
- Signing certificate SHA-256: `8bbe1da96dd01e32f971d516f1619b67c5a15b5222b40864ad3179f62aef6a04`. This is the existing installation key, also configured in the repository's encrypted Actions secrets. No new key was generated.
- Local R8 map ID: `b1872a0a11f64cd93570dae3837e305fc40b57e8b1c4bd86403c331f148cd7ec`. The matching mapping and build metadata are packaged beside the APK.

The [candidate CI run](https://github.com/kmab5/singalong/actions/runs/41508618345) passed both debug checks and the signed-candidate job, including release unit tests/lint/build, certificate verification and artifact upload. This exercises the configured GitHub secrets with the existing signing key. Public tag-triggered publication has deliberately not been exercised before release authorization.

Device interaction and acceptance remain with the user. Instrumentation tests were compiled but not executed; no emulator or SDK packages were installed.

## Phone checks

1. Confirm the candidate version in About after updating the existing signed installation.
2. Try floating icons at narrow and normal widths, tap expanded lyrics to open Now, and verify scrolling still works.
3. Use a missing song, wait for automatic docking, then play a known song and check automatic return. Manual docking/close must remain respected. Retry before timeout and check that the panel stays available during the request.
4. Enter and leave Android Settings/permission screens, lock/unlock, rotate, and switch music apps. Secure-screen suppression is expected; the panel should return on ordinary screens if still requested.
5. Change Now alignment/animation/size/spacing and try tap-to-focus/Follow lyrics. Confirm these settings persist independently of floating sizes.
6. If a new crash occurs, reopen and review the prompt, save the log, attach it to feedback, and verify the same report does not repeatedly prompt after dismissal. Native crashes/OS kills are not guaranteed by this handler.
