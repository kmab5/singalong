# Folder lookup and lyric controls checkpoint

Date: 2026-11-04. Candidate source: `feat/library-folder-and-lyrics-controls`, version 1.1.0-rc.1 (10). This checkpoint does not authorize merging or publishing a release.

## Implemented source

- Floating actions use available width to wrap into rows with 48 dp targets. Panel minimum height accounts for the extra rows. The edge handle moves on drag and opens on tap, with accessible movement actions retained.
- Library sorting persists title, artist, album, recently played or most played order with stable tie-breakers. History keeps its own recent-first default. Search and favorites combine with sorting.
- The folder action uses Android's document picker and visits subfolders. Metadata and filename hints stay separate; only search details reach existing lyric providers. Serial requests obey network policy, pacing, bounded retries and cooldowns. Progress and cancellation are visible. No audio upload, broad storage permission or persisted folder grant is introduced.
- Successful matches are saved transactionally, with an identity recheck before insertion. Existing personal selections and history are preserved; imports do not count as plays. Repeated scans and equivalent filename/tagged copies skip saved matches. Distinct or ambiguous editions are not merged arbitrarily. Playback can reuse imported filename matches when a player supplies clean tags later.
- Room migration 2 → 3 adds a nullable filename hint; existing lyric tables and selections remain intact. New backups include this hint and older backups remain readable by this version.
- Now has an independent current timed-line size. Optional Word glow interpolates estimated word emphasis between existing line timestamps. It preserves original text/timing, responds to playback position/offset and pause/seek, and respects Android motion scaling. It is not exact word synchronization; untimed lyrics retain their existing behavior.
- Settings selection panels fill the available width, with growing and wrapping chips.

## Automated verification

Final candidate checks passed with the existing JDK 21 and Android SDK, using:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
python tools/package_release.py
python tools/check_docs.py
python tools/release_version.py check
python -m unittest discover -s tools -p 'test_*.py'
```

- Debug and release: **102 unit tests each**, zero failures/errors/skips. Coverage includes sorting, filename fallback/identity, duplicate prevention, pending and pre-dispatch cancellation, network-policy changes, bounded retries, access loss, word progression and narrow overlay geometry.
- Debug/release lint: **zero errors, 23 existing warnings** per variant (dependency-update notices and a pre-existing KTX suggestion).
- Debug APK, R8/resource-shrunk release APK and instrumentation APK assembled. Device tests for narrow controls, tap/drag, storage deduplication and migration compiled but **were not executed**.
- Packaging verified version 1.1.0-rc.1 (10), the pinned existing signing certificate, the matching R8 mapping and 16 KB ZIP alignment. Signed APK size: approximately **9.66 MB**. APK, mapping, checksums and build metadata stay in ignored `artifacts/release/`; the phone-test copy is `artifacts/singalong-1.1.0-rc.1.apk`.
- Release-tool tests: **6 passed**. Version validation and local documentation links passed.

Review: the Spec review found no confirmed requirement defects. The Standards review found pre-dispatch cancellation and in-flight network-policy cancellation gaps; both were fixed and the fixes reviewed, with two additional unit tests included in the totals above. Physical acceptance remains pending.

## Physical checks still required

No emulator or SDK package was installed. Instrumentation compilation is not execution. On the S25 FE:

1. Update the existing release-signed app; confirm saved lyrics, selections, translations, history and favorites survive the database migration.
2. Use the smallest floating width, expanded/compact modes, portrait/landscape and larger Android text. Every bottom action should be visible and tappable. Drag the edge handle vertically and across the screen without opening it; tap to open it.
3. Scan a folder with tagged audio, missing tags, filename hints, subfolders, duplicate files and an unavailable song. Rescan it; saved matches should be skipped without adding duplicate library entries. Play an imported song with cleaner player metadata and confirm the saved version is reused.
4. Cancel during lookup, disconnect networking, enable Wi-Fi-only on mobile data and deny folder access. Check progress/status and preservation of completed saves. A process kill requires reselecting the folder; unfinished scans are not scheduled background work.
5. Switch each library sort, filter favorites/search, restart the app and confirm the library sort persists. History ordering remains separately selectable.
6. Set current timed-line size independently, try Word glow, pause/resume/seek/change speed and adjust sync offset. Check long lines, translations and Android animation scale zero. Confirm estimated glow is not presented as exact word timing.
7. Open Now/appearance settings at narrow widths and large font scales; selection panels should fill the row and choices should remain usable.

Provider coverage, arbitrary document-provider support and device animation quality require these phone checks; a successful build alone does not establish them.
