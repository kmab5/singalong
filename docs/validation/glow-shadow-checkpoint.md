# Glow hidden-shadow correction

Date: 2026-11-04. Candidate **1.1.0-rc.3 (12)**, on `feat/library-folder-and-lyrics-controls`. This is a phone-test build, not a published release.

## Diagnosis and source correction

The user's new screenshot and six-second recording show black copies of the active sentence following the word transforms. The preceding rc.2 checkpoint reported compilation and unit tests, not physical verification of the text masks; the new feedback demonstrates that those masks were visually incorrect.

Each animated word retains the sentence's original layout, hiding other ranges with transparent spans. Those spans also used `Shadow.None`. That constant has an opaque black color and a zero blur radius. Android's span path converts zero blur to a tiny positive radius and passes the black color to `setShadowLayer`, producing sharp black shadows for otherwise transparent glyphs. Repeating the masked sentence for each moving word compounds the shadows.

The fix sets the hidden spans' shadow color explicitly to transparent. It preserves the visible words' purple glow, drift and scale, and leaves wrapping, accessibility text, timing, selection, translations and overlay gestures unchanged.

Relevant AndroidX source: [Shadow definition](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/Shadow.kt), [span shadow conversion](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-text/src/androidMain/kotlin/androidx/compose/ui/text/platform/extensions/SpannableExtensions.android.kt), [zero-radius adjustment](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-text/src/androidMain/kotlin/androidx/compose/ui/text/platform/extensions/TextPaintExtensions.android.kt), and [native shadow application](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-text/src/androidMain/kotlin/androidx/compose/ui/text/android/style/ShadowSpan.android.kt).

## Verification

The new `AnimatedLyricTextTest` captures the rendered pixels on a neutral background and checks that white text and purple glow remain visible without any dark copies. It covers multiple word-progression positions, soft wrapping, explicit newlines, Arabic text and outgoing focus. The test was written before the correction and compiled successfully, but was **not run on a device**; no automated red/green rendering result is claimed. No emulator or SDK package was installed.

Both debug and release unit suites passed **108 tests each**, with zero failures/errors/skips. Debug/release lint and assembly passed, as did instrumentation APK assembly. Each lint report contains **zero errors and 23 existing warnings**. Release-tool tests passed **6 tests**, version validation passed, and local links in **42 Markdown documents** passed.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
python tools/check_docs.py
python tools/release_version.py check
python -m unittest discover -s tools -p 'test_*.py'
```

The R8/resource-shrunk APK is **9,647,573 bytes**. Packaging verified version **1.1.0-rc.3 (12)**, the existing pinned signing certificate, the matching R8 mapping and 16 KB ZIP alignment. The phone-test APK is `artifacts/singalong-1.1.0-rc.3.apk`; its mapping, build metadata and checksums are in `artifacts/release/`. The superseded rc.2 APK is removed after packaging. Feedback, extracted reference frames, build output and signing credentials remain ignored. Build evidence is in `artifacts/current-feedback/glow-shadow-check.log`.

## Phone checks

Install the signed rc.3 APK over the existing app and confirm About shows **1.1.0-rc.3 (12)**. Enable Glow and replay the passage in the supplied recording. Check that each word remains readable with purple glow and drift, without black copies during active lines or outgoing transitions. Also check wrapped lines, pause/resume and scrubbing. The user handles physical testing.
