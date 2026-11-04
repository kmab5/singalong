# Glow and timed-selection checkpoint

Date: 2026-11-04. Candidate 1.1.0-rc.2 (11), on `feat/library-folder-and-lyrics-controls`. No merge or public release is authorized by this update.

## Source changes and evidence

- Reviewed sampled frames from the new 76-second user recording locally. Personal video and extracted frames remain ignored.
- Glow uses brighter purple light, small slow word drift/lift and 4.5% maximum word enlargement. Word progression is still estimated from line timestamps. Pause stops drift; Android motion scaling is observed. Original sentence shaping, whitespace and accessibility text remain intact.
- The Now reader reserves a stable layout at the larger applicable font size, then animates drawing instead of rewrapping text every frame. Outgoing focus settles over 650 ms. Adjacent visible-line scrolling uses the same eased duration. Version crossfades retain outgoing content. Nonlinear Android font scaling is accounted for by converting SP to pixels before calculating drawing scale.
- Untimed lyrics with estimated sync disabled do not reserve the timed font size. Settings labels are Gentle and Glow; stored preference identifiers remain compatible.
- Eligible matches rank timed versions first, closest known provider duration second, confidence next and stable identity last. Existing title/artist/edition/duration rejection remains. Optional provider duration is stored in lyric JSON. New automatic selections and the version list share the ordering; explicit selections and personal versions are preserved.
- Real timestamps follow automatically when a new version loads, without falsely upgrading match confidence. The uncertainty label remains. Same-version refresh preserves a manual following pause; untimed estimated sync remains opt-in.
- The user clarified that long-press dragging is correct. No overlay gesture implementation was changed in this commit.
- Translation research is recorded in [translation options](../translation-options.md). No translation provider, permission, SDK or account has been added.
- Superseded local APKs, logs, screen captures, seven old mapping archives and the obsolete screenshot list were removed. Initial cleanup reclaimed about 316 MiB. Current feedback and signing identity were retained; the newest verified APK/mapping replace the previous candidate bundle.

## Verification

Before implementation, the three new lyric-preference tests failed on timed-first ranking, closest-duration ranking and uncertain-timed synchronization. The updated debug and release suites each pass **108 tests**, with zero failures/errors/skips. This also covers preserving a manually paused following state on refresh and bounded/continuous word transforms.

Final debug/release lint and assembly passed, including the instrumentation APK. Lint reports **zero errors and 23 existing warnings** per variant. The R8/resource-shrunk release APK is **9,647,573 bytes**; packaging verified version 1.1.0-rc.2 (11), the existing pinned signing certificate, the matching R8 mapping and 16 KB ZIP alignment. Device tests for stable line bounds and untimed wrapping compiled only; they were not executed in this workspace.

The release-tool suite passed **6 tests**; version validation and all local Markdown links passed. Verification used:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
python tools/check_docs.py
python tools/release_version.py check
python -m unittest discover -s tools -p 'test_*.py'
```

The phone-test APK is `artifacts/singalong-1.1.0-rc.2.apk`. Its release bundle, mapping and checksums are in `artifacts/release/`; current test logs and video-frame references are in `artifacts/current-feedback/`. The previous candidate APK has been removed after verification, reclaiming another 9.2 MiB. Generated files and feedback remain ignored.

Review found two issues: oversized untimed layout and nonlinear accessibility font scaling. Both were corrected and reviewed. No remaining confirmed findings were reported by the Spec or Standards review; neither review claims physical UI verification.

## Phone checks still needed

1. Compare ordinary line handovers to the supplied recording, including multiline lyrics, translations, slow/fast songs, scrubbing and manual scroll/follow.
2. Check Glow drift/brightness and outgoing settling. Pause/resume, change Android animation scale to zero and test large system fonts. Check RTL, emoji and long words for shaping, clipping and rendering performance.
3. Change normal/current font sizes independently. Untimed lyrics without estimated sync should keep their wrapping when only current size changes.
4. Search a song with several versions; confirm timed-first/nearest-duration ordering. Select an uncertain timed version and check automatic following and its uncertainty label. Turn following off, reopen the same version, and confirm refresh does not undo the pause.
5. Confirm saved personal selections and cached lyrics remain intact. Minimal-panel long-press dragging remains the existing behavior.

Use the signed candidate over the existing release-signed app. No emulator or SDK package was installed. Visual smoothness and text-mask drawing cost need device confirmation; successful compilation and unit tests do not establish frame-rate performance.
