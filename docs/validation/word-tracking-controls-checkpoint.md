# Word tracking controls checkpoint

Candidate: **1.1.0-rc.5 (14)**. The user confirmed rc.4 word syncing works on their phone. This candidate adds two requested controls; merge and official 1.1.0 publication still await their confirmation.

## Implemented source

- Settings → Now → **Word tracking** persists locally and defaults on. Off bypasses coordinator word progress, rendering timelines, segmentation, masks, word frame sampling, estimated sweeps and drift. Line following and line transitions continue. Saved source timing and the selected animation are retained so turning it back on restores word behavior.
- Lyric options → **Open in Google Translate** sends the selected original version, including Unicode and blank lines, with automatic source detection and English output. Saved translations are untouched. The existing external launcher reports a missing browser. Empty lyrics have no action; encoded links above a conservative 64,000-character transport bound are disabled instead of truncated.
- Google documents a [5,000-character text limit](https://support.google.com/translate/answer/6142478?hl=en). Longer lyrics within the transport bound are sent whole with a visible notice that Google may require sections. Google's handling of incoming links is external behavior, not guaranteed by unit tests.

## Evidence

Debug and release unit suites each passed **128 tests**, with zero failures, errors or skips. New coverage checks line-sync continuity while toggling word tracking and exact full-text URL encoding, empty input, long-input notices and oversized rejection. The initial test run failed on the missing toggle/link APIs before implementation. A renderer device regression checks clock reads stop, disabled text remains still despite estimate changes, and enabling tracking resumes clock reads. Device tests are compiled only.

Debug/release lint and assembly passed, including instrumentation APK compilation. Lint reports **zero errors and 24 existing warnings** per variant. Six release-tool tests and local links in 46 Markdown documents passed. Same-key packaging verified candidate **1.1.0-rc.5 (14)**, matching R8 mapping, the pinned certificate and 16 KB ZIP alignment. The optimized APK is **9,737,409 bytes**, available at `artifacts/singalong-1.1.0-rc.5.apk`; matching mapping/checksums/build metadata are in `artifacts/release/`. The superseded rc.4 APK is removed after verification. Build logs remain ignored under `artifacts/current-feedback/word-toggle-*`.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
python tools/check_docs.py
python tools/release_version.py check
python -m unittest discover -s tools -p 'test_*.py'
```

Spec and Standards code reviews reported zero actionable findings. No emulator or SDK package was installed. No signing-key change, merge, tag or public release is part of this work.

## Phone checks

1. Install `artifacts/singalong-1.1.0-rc.5.apk` over the existing signed app and check About shows **1.1.0-rc.5 (14)**.
2. With Glow and a word-synced song, turn Settings → Now → Word tracking off. Words should stop glowing/drifting individually while the current line continues following. Test a line-only song too.
3. Close and reopen the app: the switch should remain off. Enable it again: word highlights should return with their prior timing and styling.
4. Open Lyric options → Open in Google Translate. Check that every original line is present, the source is detected automatically, and the target is English. Try a non-Latin song and a different selected lyric version.
5. Return to Singalong and confirm lyrics, saved translations and source timing are unchanged. Confirm these additions before merging or publishing 1.1.0.

No battery or CPU savings percentage is claimed; removed processing paths are verified in source, while device performance has not been profiled.
