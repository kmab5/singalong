# Singalong Implementation Plan

> **For agentic workers:** Use the executing-plans skill to implement this plan task-by-task, inline. Steps use checkbox syntax for tracking. Do not dispatch agents unless the user authorizes delegation. This document does not itself authorize starting app implementation.

**Goal:** Deliver the confirmed personal Android lyrics companion with automatic collection, a synchronized floating reader, local editing and backup.

**P0 execution update (2026-11-01):** Foundation source and provider/design notes are implemented on `feat/p0-foundation`. The user explicitly deferred build verification and prohibited emulator/device-image installation. Do not run builds or install SDK components until requested. Final compilation, green tests and physical validation remain pending; no P0 completion gate or APK is claimed. See `../../validation/platform-feasibility.md`.

**Architecture:** One app module with feature packages and a shared immutable playback/lyrics state. Platform adapters feed pure selection, qualification, synchronization and overlay reducers. Repositories own versioned local data and replaceable provider access; Compose screens and the overlay render the same state.

**Tech Stack:** Kotlin, Jetpack Compose/Material 3 with KMAB tokens, coroutines/StateFlow, Room, DataStore, OkHttp, Kotlin serialization, JUnit, coroutine test utilities, AndroidX test and Compose UI tests. Pin stable compatible versions during Task 1. Use a small explicit dependency container initially; no backend or dependency-injection framework is required.

## Global constraints

- Minimum Android 12, API 31.
- Primary physical test device: Samsung S25 FE, Android 16.
- Work locally; do not create a remote or push.
- Dark-only KMAB theme; canonical fonts are Inter Display, Inter and PT Mono.
- No paid services, accounts or API keys for lyrics or translation.
- Original lyrics always remain usable independently of translation.
- Translation availability does not block the first release.
- No playback controls, microphone/audio capture, timestamp authoring or word-timing generation.
- Tracking after process death/reboot resumes only after opening the app.
- The approved design is `../specs/2026-11-01-singalong-design.md`.

The user has initialized the repository and connected `origin` to `https://github.com/kmab5/singalong.git`. The inspected baseline is commit `304d17f`; planning documents were untracked at inspection. Proposed application ID is `com.kmab.singalong`. File paths below are new files unless marked as modifications to an earlier task. Preserve the existing repository and user changes. Commit completed development tasks locally during execution; pushing or publishing is separate from this planning request.

## Package map

All Kotlin paths below are relative to `app/src/main/java/com/kmab/singalong/`; test paths mirror packages under `app/src/test/java/com/kmab/singalong/`. Instrumented tests live under `app/src/androidTest/java/com/kmab/singalong/`.

| Package | Responsibilities |
| --- | --- |
| `app` | Application container, activity, navigation |
| `model` | Recording, playback, lyrics and provider result contracts |
| `tracking` | Notification listener, service lifecycle, player selection, qualification |
| `lyrics` | Provider adapters, matching, parsing, version selection |
| `sync` | Position estimation and timed-line resolution |
| `overlay` | State reducer, window host, panel/handle UI |
| `library` | Room entities/DAOs, history, favorites, deletion |
| `editor` | Import and timed/untimed editing |
| `translation` | Optional provider, alignment and cached translations |
| `backup` | Versioned export, validation, transactional merge |
| `settings` | Preference store and settings UI |
| `ui` | Shared theme, now/library/history/reader screens and components |

## Shared contracts

Create these small immutable types in `model/Models.kt`, splitting into named files as implementations grow. UUIDs are represented as strings for persistence/serialization. `durationMs` and position are nullable, never sentinel zero. These signatures are the contract, not a requirement to put unrelated behavior in one file.

```kotlin
data class Recording(
    val id: String, val title: String, val artists: List<String>,
    val album: String?, val durationMs: Long?, val versionLabel: String?
)
enum class PlaybackStatus { PLAYING, PAUSED, BUFFERING, STOPPED, UNKNOWN }
data class PlaybackSnapshot(
    val sessionId: String, val packageName: String, val recording: Recording?,
    val status: PlaybackStatus, val positionMs: Long?,
    val observedAtMs: Long, val speed: Float
)
data class LyricLine(val id: String, val startMs: Long?, val text: String)
enum class LyricOrigin { PROVIDER, IMPORT, PERSONAL }
enum class MatchConfidence { CONFIDENT, UNCERTAIN }
data class LyricVersion(
    val id: String, val recordingId: String, val contentHash: String,
    val origin: LyricOrigin, val lines: List<LyricLine>,
    val sourceName: String?, val sourceUrl: String?,
    val confidence: MatchConfidence
)
sealed interface LookupResult {
    data class Found(val versions: List<LyricVersion>) : LookupResult
    data object NotFound : LookupResult
    data object Instrumental : LookupResult
    data class Unavailable(val reason: String, val retryAfterMs: Long?) : LookupResult
}
interface LyricsProvider {
    suspend fun lookup(recording: Recording): LookupResult
}
interface PlaybackObserver {
    val sessions: kotlinx.coroutines.flow.StateFlow<List<PlaybackSnapshot>>
    fun start()
    fun stop()
}
```

Source attribution/retention fields and provider IDs belong in a related `SourceRecord`, persisted with versions. Never hard-code network permission assumptions inside the domain types. Use a monotonic clock dependency for playback/time tests; use wall time only for user-facing history timestamps.

## Task 1: Reproducible app shell and native feasibility gate

**Files:** root `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper, `.gitignore`, `README.md`; `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`; `app/SingalongApplication.kt`, `app/MainActivity.kt`, `app/AppContainer.kt`; `model/Models.kt`; `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`; `tracking/TrackingGate.kt`, `TrackingService.kt`, `MediaNotificationListener.kt`; `docs/validation/platform-feasibility.md`.

**Produces:** buildable shell and `TrackingGate` whose `active` property starts false in each process; `onAppOpened(savedEnabled: Boolean)` and `disable()` methods.

- [ ] Inventory installed JDK, Android SDK, emulator and connected devices. Pin compatible Gradle/AGP/Kotlin/Compose/Room versions using official release documentation. Record exact versions in the version catalog and README. Generate the official Gradle wrapper; never download arbitrary wrapper binaries.
- [ ] Preserve the existing Git repository; ignore `local.properties`, build outputs, signing material and personal exports. Add API 31 minimum and a stable current target/compile SDK.
- [ ] Write `tracking/TrackingGateTest.kt` before implementing the process authorization gate:

```kotlin
@Test fun processRestartDoesNotResumeTracking() {
    val first = TrackingGate()
    first.onAppOpened(savedEnabled = true)
    assertTrue(first.active)
    val restarted = TrackingGate()
    assertFalse(restarted.active)
    restarted.onAppOpened(savedEnabled = true)
    assertTrue(restarted.active)
}
```

- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*TrackingGateTest'`; confirm the behavioral test fails before the implementation, then implement the boolean gate and verify it passes.
- [ ] Build a minimal activity with permission-status controls, user-started service and session diagnostics. Declare notification-listener binding correctly and keep it inactive unless the process gate is open. Verify candidate `specialUse` service declaration and explanatory subtype against official docs; no `mediaPlayback` misclassification. Use a non-sticky lifecycle and no boot auto-start.
- [ ] Exercise visible-app start, notification interaction, notification denial, overlay denial and fresh-process listener binding on API 31 and API 36 where available. Record actual results and any unavailable device checks. Stop and resolve a lifecycle failure before Task 4; do not hide it with battery exemption requests.
- [ ] Add KMAB colors/font resources and a minimal three-destination shell. Fetch fonts from official sources with licenses. Read relevant brand guidelines/assets before use. Build with `./gradlew.bat :app:assembleDebug :app:lintDebug` and commit the independently buildable shell.

## Task 2: Local recordings, versions, preferences and history

**Files:** `library/SingalongDatabase.kt`, `Entities.kt`, `LibraryDao.kt`, `LibraryRepository.kt`; `settings/SettingsStore.kt`; `library/RecordingIdentity.kt`; instrumented `library/LibraryRepositoryTest.kt`.

**Consumes:** model contracts. **Produces:** `LibraryRepository.observeRecordings(): Flow<List<Recording>>`, `saveVersion(version: LyricVersion)`, `selectVersion(recordingId: String, versionId: String)`, `recordQualifiedPlay(recordingId: String, occurrenceId: String, playedAtMs: Long)`, `selectedVersion(recordingId: String): LyricVersion?`. Suspending operations run off the UI thread. DAO transactions enforce invariants.

- [ ] Create Room tables for recordings, aliases/provider identifiers, versions, lines, selections, source records, adjustments, favorites, history summaries and occurrence deduplication. Create DataStore preferences for tracking, allowed packages, network policy, overlay geometry/text/opacity and translation enabled.
- [ ] Add a database test that records the same occurrence twice and asserts count 1; add a distinct occurrence and assert count 2. Verify selecting a personal version survives saving a new provider version. Verify same title/different artist and studio/live recordings do not merge.
- [ ] Run `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kmab.singalong.library.LibraryRepositoryTest` to establish the failing case; implement transactional updates and rerun.
- [ ] Preserve original strings while matching normalized comparison keys. Enrich incomplete metadata without creating a second history occurrence. Define conservative ambiguity behavior: keep separate recordings when identity cannot be established.
- [ ] Export Room schemas, test reopening the database and confirm selected-version foreign keys stay valid. Commit storage and preferences.

## Task 3: Player discovery, arbitration and qualification

**Files:** `tracking/AndroidPlaybackObserver.kt`, `PlayerSelector.kt`, `CollectionQualifier.kt`, `TrackingCoordinator.kt`, `PlayerSelectionScreen.kt`; modify listener/service/container; tests `PlayerSelectorTest.kt`, `CollectionQualifierTest.kt`.

**Consumes:** `PlaybackObserver`, settings, `recordQualifiedPlay`. **Produces:** `PlayerSelector.select(sessions: List<PlaybackSnapshot>, allowed: Set<String>, overrideSessionId: String?): PlaybackSnapshot?`; `CollectionQualifier.observe(snapshot: PlaybackSnapshot, nowMs: Long): Qualification?`, where `Qualification(recordingId: String, occurrenceId: String)` is emitted once per qualified occurrence.

- [ ] Write clock-driven tests for 9,999 versus 10,000 ms playing, pause exclusion, buffering exclusion, seek exclusion, duplicate updates, track change, restart and end-to-start repeat. Use this explicit timeline: playing 0–6 s, paused 6–20 s, playing 20–24 s qualifies at 24 s, not 10 s.
- [ ] Write arbitration tests: excluded app never selected; most recent playing transition wins; paused current player remains until another starts; manual override persists until removed/session destroyed. On initial attachment, platform priority breaks ties.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*CollectionQualifierTest' --tests '*PlayerSelectorTest'`, observe failures, implement pure logic and rerun.
- [ ] Adapt `MediaSessionManager`/controllers into immutable snapshots. Register/unregister callbacks once; clear destroyed sessions; copy only media metadata. Discover candidate apps using launcher queries and observed sessions, not broad all-packages permission. Resolve actual app labels and package IDs on device.
- [ ] Connect qualification to idempotent history updates and lookup scheduling. Manual minimize uses qualification delay; visible panel requests immediate lookup. Tracking disabled cancels collection and retries. Verify no hidden collection after process recreation.
- [ ] Record diagnostics with no lyrics or unrelated notification payloads. Commit the tracking pipeline.

## Task 4: Provider access, matching and offline lookup

**Files:** `lyrics/LrclibProvider.kt`, `LyricsRepository.kt`, `LyricsMatcher.kt`, `LrcParser.kt`, `SourceRecord.kt`; tests `LyricsMatcherTest.kt`, `LrcParserTest.kt`, `LyricsRepositoryTest.kt`; `docs/validation/providers.md`.

**Consumes:** recording/version models, local storage and network policy. **Produces:** `LrcParser.parse(text: String): List<LyricLine>`; `LyricsRepository.resolve(recording: Recording, immediate: Boolean): LookupResult`; `LyricsMatcher.rank(recording: Recording, candidates: List<LyricVersion>): List<LyricVersion>`.

- [ ] Verify current LRCLIB documented request/response schema, limits, attribution and retention conditions. Record sources and observed availability. If retention is unresolved, mark the release gate explicitly; do not implement permanent collection under an invented license. Use synthetic test lyrics, not downloaded copyrighted fixtures.
- [ ] Write parser tests for multiple tags, repeated chorus, offset metadata, empty timed lines, malformed tags, Unicode and untimed text. Write matcher tests for punctuation differences versus substantive live/remix/duration differences.
- [ ] Write repository tests proving personal selection wins, offline cache works, unrelated results yield not-found, repeated requests deduplicate, 429 honors retry-after, and a response for song A cannot replace current song B.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*LrcParserTest' --tests '*LyricsMatcherTest' --tests '*LyricsRepositoryTest'`; implement parsing, matching and fakeable HTTP adapter, then rerun.
- [ ] Implement local-first lookup and independent current-request identity. Preserve uncertain results but disable automatic timing. Display network error separately from not-found/instrumental. Use a bounded retry policy; clear queued work when tracking stops.
- [ ] Perform a small manual API smoke test within published limits; ordinary tests remain deterministic and offline. Commit lyrics retrieval.

## Task 5: Position estimation and synchronized reader

**Files:** `sync/PlaybackClock.kt`, `LyricTimeline.kt`, `ReaderState.kt`; `ui/reader/ReaderScreen.kt`, `ReaderViewModel.kt`; `sync/PlaybackClockTest.kt`, `LyricTimelineTest.kt`.

**Produces:** `PlaybackClock.position(snapshot: PlaybackSnapshot, nowMs: Long): Long?`; `LyricTimeline.lineIndex(lines: List<LyricLine>, playerPositionMs: Long, delayMs: Long): Int?`. Reader state tracks following/manual mode and selected version separately from live overlay state.

- [ ] Add arithmetic and unknown-position tests. A snapshot at position 12,000, observed at 1,000, playing at 1.5x produces 15,000 at time 3,000; paused produces 12,000. Unknown stays unknown.
- [ ] Add offset tests using synthetic lines:

```kotlin
@Test fun positiveDelayShowsLineLater() {
    val lines = listOf(LyricLine("one", 10_000, "Sample line"))
    assertNull(LyricTimeline.lineIndex(lines, 10_500, delayMs = 1_000))
    assertEquals(0, LyricTimeline.lineIndex(lines, 11_000, delayMs = 1_000))
}
```

- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*PlaybackClockTest' --tests '*LyricTimelineTest'`; implement monotonic interpolation, clamps and timestamp lookup; rerun.
- [ ] Add per-version delay storage, 0.25-second controls, reset and explicit try-sync for uncertain matches. No automatic timing for plain lyrics. Unknown timing switches to manual; recovery requires `resume sync` when manual reading is active.
- [ ] Implement a full-screen reader with original text, optional translation space, user scroll suspension and `back to current line`. Saved non-playing recordings cannot alter active playback or overlay selection.
- [ ] Show pre-first-line or explicitly marked instrumental countdown only; test that a long line-start gap alone does not produce an instrumental claim. Commit reader and synchronization.

## Task 6: Floating panel, notification action and lifecycle transitions

**Files:** `overlay/OverlayState.kt`, `OverlayReducer.kt`, `OverlayWindowHost.kt`, `LyricsPanel.kt`, `EdgeHandle.kt`; `tracking/TrackingNotification.kt`; tests `OverlayReducerTest.kt`; instrumented `overlay/OverlayInteractionTest.kt`.

**Consumes:** shared live reader state and authorization. **Produces:** `OverlayReducer.reduce(state: OverlayState, event: OverlayEvent): OverlayState`. States are `Hidden`, `Visible`, `ManuallyMinimized`, `AutomaticallyMinimized`; visible presentation remembers compact/expanded and geometry. Events distinguish explicit close/minimize/show from pause timer, resume and track change.

- [ ] Write transition tests directly from the spec table. Include stale pause-timer firing after close, manual minimize followed by resume, automatic minimize followed by resume, track-change clear-before-fetch and process recreation hidden.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*OverlayReducerTest'`, implement the reducer and rerun. Keep timers outside the reducer and attach generation IDs so stale events cannot reverse user actions.
- [ ] Build `TYPE_APPLICATION_OVERLAY` host with Compose lifecycle/saved-state owners and deterministic cleanup. Add drag/resize plus accessible alternatives. Clamp positions to current display bounds and keep touch interception confined to the panel. Respect lock-screen/display-off behavior.
- [ ] Implement compact original/translation pair, expanded context, manual scroll for plain lyrics, 15-second paused collapse and separate manual handle. Freeze countdown while paused. No playback controls.
- [ ] Wire notification `show lyrics` to current state and allowed user-interaction service start. Handle revoked permission via visible recovery. Closing the overlay must not stop collection.
- [ ] Run instrumented checks for rotation, drag bounds, large text, permission revocation and opening over another app. Verify service cleanup removes windows and callbacks. Commit the first usable floating prototype.

## Task 7: Search, import and protected personal editing

**Files:** `editor/LyricsImport.kt`, `LyricsEditor.kt`, `EditorScreen.kt`; `lyrics/MatchPickerScreen.kt`; tests `LyricsEditorTest.kt`, `LyricsImportTest.kt`; modify reader/navigation/repository.

**Produces:** `LyricsEditor.editTimed(source: LyricVersion, replacements: Map<String, String>): LyricVersion` and `LyricsEditor.createUntimed(source: LyricVersion, text: String): LyricVersion`, both creating new personal versions. Unknown line IDs and multiline replacements in timed mode are rejected explicitly.

- [ ] Write tests that timed editing preserves every line ID/time and count; inserted/deleted lines are rejected; untimed copies allow arbitrary paragraphs; provider original is unchanged; selection moves to personal version; new content hash invalidates translation association.
- [ ] Write import tests for UTF-8 BOM, Unicode, plain text, LRC, empty/oversized input and malformed timestamps. Set and document a 2 MiB per-lyrics import limit; show a readable size error before loading excessive content.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*LyricsEditorTest' --tests '*LyricsImportTest'`; implement validation and version creation, then rerun.
- [ ] Add document-picker preview, recording association, manual metadata search and candidate picker. Plain editor has explicit save/discard and unsaved-change handling. Timed editor exposes fixed line fields plus `create untimed version`.
- [ ] Verify provider refresh cannot replace the user's selected edit; selecting another version remains explicit. Commit import and editing.

## Task 8: Optional translation capability and cache

**Files:** `translation/TranslationProvider.kt`, `TranslationRepository.kt`, `TranslationRecord.kt`, `TranslationSettings.kt`; tests `TranslationRepositoryTest.kt`; modify settings/reader/overlay; update `docs/validation/providers.md`.

**Produces:** `TranslationProvider.translate(version: LyricVersion): TranslationResult`, with `Available(lines: Map<String, String>, sourceLanguage: String, attribution: String)`, `Unsupported`, `Unavailable` and `Failed` results. English is the fixed target. Persist only validated mappings bound to version ID/content hash and provider identity.

- [ ] Investigate documented free no-key online providers. Verify actual supported use, attribution, caching, limits and language coverage. Do not equate a public demo endpoint with permission to depend on it. Record the chosen provider or explicit unavailable outcome.
- [ ] Write tests: disabled never calls network; unopened collected lyrics never translate; valid cache avoids network; edits invalidate cache; translation failure preserves original; missing/duplicate line IDs reject alignment; responses for old versions never attach to current versions.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*TranslationRepositoryTest'`; implement the repository against a fake provider, then a verified adapter if one exists, and rerun. A production unavailable adapter is valid when the feasibility gate fails; label it accurately.
- [ ] Add off-by-default setting with clear network disclosure and unavailable state. Translate when opened/enabled, never all background collection. Reuse saved translations offline. Do not show internal provider failures over the original text.
- [ ] Visually check long translations, mixed scripts, wrapping and font scaling. Commit optional translation support without claiming coverage beyond evidence.

## Task 9: Library, history, settings and storage controls

**Files:** `ui/now/NowScreen.kt`, `ui/library/LibraryScreen.kt`, `ui/history/HistoryScreen.kt`, `settings/SettingsScreen.kt`, `library/DeletionPolicy.kt`; instrumented `ui/LibraryNavigationTest.kt`; tests `library/DeletionPolicyTest.kt`.

**Consumes:** persisted versions/history/preferences. **Produces:** complete app navigation with independent library and live-reader selection.

- [ ] Implement search, favorites, offline indicators, unique recent-history rows and play counts. Display missing/uncertain/manual states without conflating them. Now has player selection, tracking state and `sing along`.
- [ ] Add settings for allowed players, tracking, Wi-Fi-only, translation, original/translation text size, panel opacity/size and storage usage. Persist geometry independently from visibility. Denied permissions have focused recovery actions.
- [ ] Test deletion policies before implementing bulk removal: clearing history retains lyrics; clearing downloads preserves personal versions unless explicitly included; selected-version deletion cannot leave an invalid foreign key; preserve-favorites works.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*DeletionPolicyTest'` and navigation instrumentation. Verify opening a saved song leaves the overlay's live song unchanged.
- [ ] Review all UI copy against the KMAB medium register; preserve proper casing in metadata/lyrics. Remove developer terminology from ordinary controls. Commit the full navigation and settings surface.

## Task 10: Backup export, validation and idempotent restore

**Files:** `backup/BackupDocument.kt`, `BackupCodec.kt`, `BackupValidator.kt`, `BackupMerger.kt`, `BackupScreen.kt`; tests `BackupValidatorTest.kt`, `BackupMergerTest.kt`; instrumented `backup/BackupTransactionTest.kt`.

**Produces:** `BackupCodec.encode(document: BackupDocument): String`, `decode(json: String): BackupDocument`; `BackupMerger.merge(document: BackupDocument): MergeSummary` as a suspend transaction. Schema version starts at 1; `MergeSummary` counts added, duplicate and preserved-conflict records.

- [ ] Define the complete schema from the spec, excluding permissions/credentials/artwork/active authorization. Enforce 50 MiB size, supported schema, required IDs, foreign-key consistency and bounded individual strings before writes.
- [ ] Add synthetic round-trip and merge fixtures. Restoring the same file twice must leave row counts/play counts unchanged. Conflicting personal versions survive; explicit current selections/preferences win. History count uses max and timestamp uses latest.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*BackupValidatorTest' --tests '*BackupMergerTest'`; implement validation/merge decisions and rerun. Test malformed input changes nothing and newer schemas fail with an actionable message.
- [ ] Add instrumented rollback test with failure injected partway through merge; confirm the database is unchanged. Use system create/open document pickers and stream sizes safely. Never activate tracking on restore.
- [ ] Show a preview and completion summary, including that current settings were retained. Document count underestimation for disjoint histories. Commit backup/restore.

## Task 11: Compatibility, accessibility and release verification

**Files:** `docs/validation/compatibility.md`, `docs/validation/release-checklist.md`; modify README and affected production/test files only when findings require fixes.

- [ ] Create one row per actual player package with device/OS, app version/date, metadata, duration, position, seeks, pauses, track changes, repeats, screen-off collection and known limitations. Start each status `not tested`; fill only from observation. Record account/availability constraints without inventing results.
- [ ] On the S25 FE, test each available named player with normal playback, 10-second threshold, rapid skips, pause/resume, seek, backgrounding, screen off/on and process termination. Verify no restart collection until app launch. Measure baseline and tracking-on idle/listening periods before making battery claims.
- [ ] On API 31 emulator, verify startup, permission denial/recovery, overlay interaction, import/edit and restore. Use a controllable test media session for deterministic seek/speed/repeat cases absent from installed players.
- [ ] Check dark-mode contrast, 48 dp targets, TalkBack labels, large system font, RTL text, long translations, keyboard/editor behavior, rotation, display cutouts and reduced motion. Inspect real screenshots against KMAB tokens; no web-like hero or decorative playback UI.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`, then `./gradlew.bat :app:connectedDebugAndroidTest` on the named test device/emulator. Inspect actual results, not just process launch. Fix failures and rerun affected checks.
- [ ] Install the final APK and smoke-test detect → lookup → overlay → close → notification reopen → edit → backup/restore. Record APK path/checksum, build instructions and limitations. Keep signing material local and preserve signing continuity for upgrades; do not embed secrets in source.
- [ ] Commit verified delivery artifacts/docs. Report which gates passed, which device/provider checks remain unavailable, and whether translation is available. No claim of universal support or completed physical tests without evidence.

## Milestones and checkpoints

The execution order and detailed gates are defined in [development phases](2026-11-01-development-phases.md). It expands these tasks into seven phases, moving provider feasibility and basic overlay verification earlier to resolve risk before building the full UI:

1. P0: technical feasibility and visual baseline.
2. P1: selected-player tracking and durable collection.
3. P2: lyrics retrieval and full-screen synchronization.
4. P3: reliable floating sing-along experience.
5. P4: library, personal editing and backup/restore.
6. P5: optional translation and final UI refinement.
7. P6: compatibility validation and personal release.

The earlier 6–9 session estimate preceded the full editor/backup/lifecycle detail. Use these milestones as the progress measure; refine effort after the native feasibility gate. Do not compress verification to preserve an initial estimate.

## Plan self-review

- Tracking qualification, selection and restart behavior: Tasks 1–3.
- Matching, source rules, persistence and uncertain sync: Tasks 2, 4–5.
- Overlay defaults, notification, pause/minimize/close: Task 6.
- Manual search/import/two editor modes and protected versions: Task 7.
- Free optional translation, cache and invalidation: Task 8.
- Screens, favorites, history, settings and deletion: Task 9.
- Local backup and safe idempotent merge: Task 10.
- Brand/accessibility and named-device compatibility: Tasks 1, 6, 9 and 11.
- Shared model signatures and package paths are defined above; fixture tests must use synthetic content.

Next action is review of these written documents. Execution is inline by default when the user asks to begin; a separate delegation choice is unnecessary.
