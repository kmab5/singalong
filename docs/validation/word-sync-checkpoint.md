# Provider word-sync checkpoint

Date: 2026-11-04. Candidate **1.1.0-rc.4 (13)**, on `feat/library-folder-and-lyrics-controls`. The user confirmed the rc.3 animation is working and approved connecting supplied word timestamps to it. **Merge and official 1.1.0 release await further user approval after testing.**

## Implemented source

- LRCLIB's `lyricsfile` is parsed as bounded, safe YAML. Supported Lyricsfile 1.0 line/word text, absolute millisecond starts and optional ends are preserved in JSON-backed lyric versions. No database schema migration is needed; old lines default to empty word data.
- Text segments retain whitespace and Unicode boundaries. Invalid segment text/timing uses the original line text and line timing. Unsafe or unsupported documents fall back to legacy LRC/plain responses. Nonzero draft global offsets are unsupported because their semantics are unresolved. Parsing runs on a background dispatcher.
- The existing Glow renderer uses a frame-sampled playback clock for real word timing, respecting pause, seek, speed and user delay without the estimated sweep's smoothing delay. Explicit word ends take precedence, gaps do not advance future words, and overlapping words advance independently. Missing ends use a later start or line boundary only while drawing, never as saved data. A final word without any known end reveals at its known start.
- Existing line-only estimated progression, glow styling, drift, enlargement, stable layout and transparent hidden shadows are retained. Extremely large lines (more than 128 segments or 4,096 characters) display as ordinary readable lines instead of allocating excessive animation masks; saved timing is unaffected. The reader still follows one current lyric line, rather than presenting simultaneous vocal tracks.
- Version labels distinguish word/partial-word/line/untimed lyrics; word timing breaks ties between otherwise equally matching versions. Existing selections, personal versions and timing adjustments remain intact. Cached songs can fetch additional versions through **Find, import or edit lyrics → find lyrics** and select a word-synced result.
- Backups retain/validate word and end timing. Unchanged personal-copy lines preserve it, while changed text/start times drop stale word timing from the changed line. Multiline provider segments retain their original representation; the simple editor offers explicit untimed conversion before editing those segments.

Format sources: [LRCLIB API](https://lrclib.net/docs.txt), [Lyricsfile 1.0 draft](https://github.com/tranxuanthang/lyricsfile/blob/main/SPECIFICATION.md). SnakeYAML 2.5 is pinned and listed in the project's third-party notices. No new service, account, permission or signing identity is introduced.

## Evidence

The initial regression failed because the existing matcher discarded word timing. The final debug and release unit suites each passed **123 tests**, with zero failures/errors/skips. This includes parsing/fallback, actual durations and gaps, overlaps, optional ends, Unicode, pause/seek/speed/delay, version identity, backup compatibility and personal-edit preservation.

Debug/release assembly and lint passed; the instrumentation APK compiled. Lint reports **zero errors and 24 warnings** per variant: the prior 23 dependency/KTX suggestions plus an available-update notice for the pinned SnakeYAML dependency. Six release-tool tests, version validation and links in 44 Markdown documents passed. Device tests were **compiled only**, not executed.

The signed R8/resource-shrunk APK is **9,733,589 bytes**. Packaging verified version **1.1.0-rc.4 (13)**, the pinned existing certificate, matching R8 mapping and 16 KB ZIP alignment. The parser's Apache 2.0 license is bundled and was verified inside the APK. Final phone-test output is `artifacts/singalong-1.1.0-rc.4.apk`; mapping, checksums and build metadata are in `artifacts/release/`. Superseded rc.3 is removed only after the new APK is verified. Feedback and build output remain ignored.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
python tools/check_docs.py
python tools/release_version.py check
python -m unittest discover -s tools -p 'test_*.py'
```

Logs: `artifacts/current-feedback/word-sync-red.log`, `word-sync-final-check.log` and `word-sync-package.log`.

Live, identified search requests verified that LRCLIB returns `hasWordSync` and `lyricsfile`. Three searches (Vossi Bop, Golden and Never Gonna Give You Up) returned 60 records in total; none advertised word timing. This verifies the response schema, not word-sync coverage or live timed rendering. Unit fixtures use synthetic text structured according to the published format, not copied song lyrics.

Spec and Standards review found and resolved repeated per-frame end inference and ambiguous multiline editing. Device pixel tests cover keeping the approved Glow rendering and overriding a deliberately wrong estimate with supplied timestamps. Instrumentation execution and new phone behavior remain unverified until device testing.

## Phone checks

1. Install the same-key candidate over the existing app and confirm About shows **1.1.0-rc.4 (13)**.
2. Enable Glow. Fetch/select a version labelled **Word-synced** (or **Word-synced · some lines**). The reader says **Glow · word-synced** for a line with valid supplied words; other lines explicitly say estimated progression.
3. Listen for short/long words and silent gaps. Pause/resume, seek forward/backward, adjust the lyric delay and try another playback speed if the player reports it.
4. Replay a line-only song and confirm the previously approved animation still works. Check translations, multiline and non-Latin lyrics, manual scroll/follow and song changes.
5. Reopen cached lyrics offline. Export/restore a backup and verify word timing remains. Save a personal edit and confirm the changed line falls back to line timing while the original stays word-synced.

No emulator, system image or SDK package is installed. No merge, tag or public release is performed.
