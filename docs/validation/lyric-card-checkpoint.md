# Lyric card checkpoint

Candidate: **1.2.0-rc.1 (16)** on `feat/lyric-share-cards`. No merge, tag or publication is authorized for this new feature. The main branch retains the prepared 1.1 source and the published release remains v1.0.0.

## Implemented source

- Original lines in Now, saved lyrics and standard floating lyrics expose long-press actions, including the upcoming line in compact mode. A draggable bottom context sheet opens a card editor. Floating actions route into the activity; minimal-mode long-press movement remains unchanged.
- Four templates and four aspect ratios, original-order excerpt selection, three font families, text sizing/alignment, hex/palette colors, gradient/solid/image backgrounds, cover selection/visibility/size and independent lyric/cover positioning. One Android Canvas renderer supplies both preview and export; previews retain their previous image while controls update.
- Song credits, source attribution (including personal-copy provenance), Singalong icon/wordmark and lowercase kmab remain present. Oversized excerpts or credits report layout errors rather than truncating selected text.
- Automatic local player artwork is copied/decoded once, checked against metadata again and retained as draft-owned pixels. Rotation cannot re-read a mutable player artwork URI. Draft generations prevent dismissed or superseded loads from reopening the sheet. The draft does not change with playback or modify the original database.
- Local image decoding is bounded to 1,600 pixels per edge. PNG export is bounded by format dimensions, uses a cache-only FileProvider and read grants, and supports Android share/save pickers without broad storage access. Cache cleanup touches only generated card PNGs; user images and saved cards remain untouched.

## Verification

Debug and release unit suites each passed **140 tests**, with zero failures, errors or skips. Unit coverage checks original text/order/Unicode, source preservation, stale recording rejection, layout-setting bounds and template selection. Compiled device regressions cover long-press routing, editor state restoration, empty selections, frozen input, independent artwork pixels, all template/aspect render combinations, text overflow and the narrow export provider. No device tests, emulator or visual phone checks have been executed.

Debug/release lint and assembly passed, including instrumentation APK compilation. Lint reports **zero errors and 32 warnings** per variant: the previous 24 suggestions plus eight KTX suggestions in the new code. The initial editor range literals, test imports and line-break constant lint failure were corrected before the successful run. Six release-tool tests and local links in **51 Markdown documents** passed.

Same-key packaging verified version **1.2.0-rc.1 (16)**, the pinned certificate, matching R8 mapping and 16 KB ZIP alignment. The optimized APK is approximately **9.4 MiB**, at `artifacts/singalong-1.2.0-rc.1.apk`. Mapping, checksums and build information are in `artifacts/release/`. Logs are ignored under `artifacts/current-feedback/card-*`. This records local verification, not hosted CI or device acceptance.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
python tools/check_docs.py
python tools/release_version.py check
python -m unittest discover -s tools -p 'test_*.py'
```

Spec and Standards review identified and resolved player-URI artwork reloading, lost attribution on personal edits, and the upcoming compact line's missing long-press action. Focused rereviews reported no additional actionable findings.

## Phone checks

1. Install `artifacts/singalong-1.2.0-rc.1.apk` over the existing signed app. Confirm the version in About.
2. Long-press original lines in Now and saved lyrics, drag/dismiss the sheet, then create a card. Repeat for current and upcoming lines in standard floating mode. Confirm minimal-mode dragging still works.
3. Try all templates/formats, a longer excerpt, RTL/non-Latin lyrics, custom colors and positions. Compare the preview with the saved PNG; overflow must be explained without partial lyrics.
4. Pick a cover and background, cancel a picker, rotate the phone, change songs and return. The open card must keep its original song/credit/artwork. Missing or inaccessible artwork should leave an understandable image-selection option.
5. Share to an installed receiving app and save using the document picker. Check the complete excerpt, music credits, source and Singalong branding. Reopen original lyrics to confirm they are unchanged.
6. Test large system text, portrait/landscape windows, TalkBack actions and export while rapidly adjusting controls. Exports must correspond to the completed preview.
