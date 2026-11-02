# 0.3.3: filename search and optional line timing

> Historical checkpoint: results and APK versions below describe that revision. See the [current documentation index](../README.md) and [pre-merge checkpoint](premerge-checkpoint.md) for later changes and verification.

## Implemented source

- Automatic and manual lyric search share a bounded query planner. It understands common audio/video file extensions, underscores, numbered prefixes, bracketed upload decorations, `Artist - Title`, title-first names when the known artist corroborates them, and `Artist - Album - 03 - Title`.
- Recognized filename hints are searched first, then the original metadata if necessary. At most two query variants are currently generated, each using existing LRCLIB/lyrics.ovh fallback. Provider failures and cooldowns stop further variants. Cancellation and network policy are checked between requests; late canceled results cannot save candidates.
- Original recording identity and metadata remain intact. Guessed results retain provider attribution and are marked uncertain. Version qualifiers, artist matching and duration safeguards remain enforced. Album is inferred only from the explicit four-part layout. Ambiguous filenames and titles without an identifiable artist still require manual details; no audio recognition occurs.
- Search details show a reviewable suggestion. The default lyric editor remains a text field. **Edit timing** opens a separate panel with a line picker, `mm:ss.mmm` entry, 0.1-second nudges, clear time and stamp-and-next.
- Stamping reads the current position only for the recording being edited; another track, missing position or stopped playback disables it. It uses actual playback position, not the reader's estimated timing or offset. Paused playback supports precise stamping.
- Hiding timing controls retains valid and unfinished input. Draft text/timing survive activity rotation in the existing ViewModel. Invalid input, backwards times and structural line-count mismatches prevent saving. Creating an untimed version explicitly clears timing and allows structural changes. Partial timing can be saved; untimed lines have no automatic timing.
- Saving creates a personal version without changing the source. Edited timing is marked uncertain and can be followed using **Try sync**. No permissions or external services were added.

## Verification

Final command with the existing JDK and SDK:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
```

- **BUILD SUCCESSFUL**, 4m 25s. Ignored local log: `artifacts/search-timing-final-build.log`.
- **78 unit tests in each variant**, zero failures/errors/skips. The initial filename lookup and late-cancellation tests failed against the previous implementation; both pass with the coordinator. Additional coverage includes Unicode and explicit album layouts, known-artist disambiguation, preservation of numeric titles/version qualifiers, wrong-match rejection, cooldown/network boundaries, timestamp parsing/order, matching-track stamping, partial timing and draft structure/content preservation.
- Debug/release lint: zero errors; 21 existing dependency-update warnings each.
- Debug, release and instrumentation APKs compiled. The two new Compose tests exercise manual timestamp entry and disabling stamping on a song change. Instrumentation tests were **not run**; no emulator or SDK package was installed. Physical UI/rotation/provider checks remain pending.
- Signed, minified/resource-shrunk APK: **0.3.3 (7)**, minimum API 31 / target 36, **9,565,649 bytes**. Local copies: `artifacts/singalong-0.3.3.apk` and `artifacts/singalong-release.apk`.
- APK SHA-256: `5BF92FC6B9DFFA0AFD87A2E6BF6A92A2B6DC096FC3074E2E1409D1DA0CA6DD4B`.
- Signature verified against the unchanged RSA-3072 certificate SHA-256 `8bbe1da96dd01e32f971d516f1619b67c5a15b5222b40864ad3179f62aef6a04`.
- 16 KB ZIP alignment passed; all six 64-bit native libraries have ELF LOAD alignment of at least 16 KB.
- Mapping archived locally at `artifacts/mapping/5BF92FC6B9DFFA0AFD87A2E6BF6A92A2B6DC096FC3074E2E1409D1DA0CA6DD4B.txt`.

## Phone acceptance still required

1. Update the existing signed installation. Play a file with metadata like `03 - Artist_Name - Song_Title (Official Audio).mp3`, then an ordinary well-tagged song. Confirm results and attribution, with original library details preserved. Check a live/remix file does not silently match the studio version.
2. Open lyric editing. Confirm simple text editing is the default; enable **Edit timing**, enter a timestamp, nudge it, clear it, and choose another line. Hide/reopen controls and rotate the phone to check draft retention.
3. With the same song playing in Musicolet, stamp several consecutive lines. Pause and stamp precisely. Change songs while the editor is open and confirm stamping disables. Complete some lines and save a partial personal version; check it remains selectable alongside the original.
4. Enter a malformed timestamp or a time before the preceding line. Confirm a readable error and disabled save. Return to text editing and add a line to a timed version; confirm the structural warning. Use **create an untimed version** and confirm saving works without changing the original.
5. Retest the earlier crash scenario: quickly change tracks before requests finish, both inside/outside the app and with floating lyrics open/closed. Exercise unknown songs and slow/offline connections. Export About → Save crash details if a crash recurs.

## Explicitly deferred

Audio recognition is for a future version. The user must confirm all phone issues are fixed before merging to main, preparing v1.0.0, adding GPL v3/community documents, auditing all documentation, or configuring tagged GitHub APK Releases and the website download link. This checkpoint does not authorize or claim those tasks.
