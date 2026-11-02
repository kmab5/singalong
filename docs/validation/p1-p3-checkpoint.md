# P1–P3 source checkpoint

> Historical checkpoint: results and APK versions below describe that revision. See the [current documentation index](../README.md) and [pre-merge checkpoint](premerge-checkpoint.md) for later changes and verification.

Date: 2026-11-01. Requested scope: implement P1, P2 and P3, commit and push. The user's instruction to skip builds/device verification and use the existing SDK remains in force. No Gradle, lint, emulator, SDK installation, APK installation or test-execution commands were run for this checkpoint.

Implementation commit: `1388692` on `feat/p0-foundation` (the existing development branch retained for continuity). This checkpoint records source delivery, not passed M1/M2/M3 acceptance.

## Implemented source

### P1: collection

- Room recordings, qualified occurrences, history summaries, lyric versions, selections and adjustments. DataStore preferences migrate P0 setup flags.
- Launcher/observed-session player discovery, allowlist, automatic newest-playing selection and manual override. Returning to automatic uses the latest automatic selection, including transitions observed during an override.
- Conservative recording identity with metadata enrichment, version separation and transactional occurrence deduplication. Qualification counts observed elapsed playing time, not seek distance; repeats require a credible end-to-start update.
- Process-local authorization, explicit stop cleanup, network policy and bounded collection retries.

### P2: lookup and reading

- Local-first LRCLIB repository; serialized, paced, identified requests; bounded response bodies; cancellation; Retry-After; Wi-Fi policy. Duplicate concurrent lookups reuse stored results; failures have cooldowns.
- Title/artist comparison tolerates punctuation; album/duration and version qualifiers govern confidence. Substantially different recordings are rejected. Plausible versions are retained; existing selection is preserved.
- Plain/LRC parsing, multiple/repeated timestamps, offsets, Unicode and explicit empty intervals. Monotonic playback interpolation, speed, pause/buffer freeze, duration clamps and unknown-position fallback.
- Shared live reader state, per-version corrections, uncertain-match opt-in, manual scroll/resume, source identification and separate saved-copy reading. Stale responses cannot replace a different live recording.
- Separate loading, offline, not-found, instrumental, rate-limit and unavailable states. Explicit instrumental results are retained for offline use.

### P3: floating lyrics

- Production overlay uses the same live reader as the app, with a purple active-line cue and original-language priority. There is no runtime sample-content path.
- Compact/expanded presentation, manual scrolling, dedicated drag/resize areas and accessibility alternatives; font/opacity settings; safe-area bounds and persisted geometry.
- Hidden, visible, manual-minimized and automatic-minimized states. Pausing for 15 seconds minimizes; only automatic minimization restores on play. Closed/manual-minimized panels remain so through new songs. Minimized handles sit at a display edge.
- New visible songs clear old content and request immediately; background collection uses qualification. Notification reopening targets the live song. Lock/display-off suppresses rendering; no wake lock or automatic process restart.

## Design and implementation choices

The existing KMAB palette, bundled typography, lambda mark, rounded panel and lyric cue remain the visual foundation. Controls use native labelled icons and touch targets. Rendered contrast, RTL, font scaling and geometry still require actual device inspection.

Lyric lines and source attribution are immutable serialized version payloads within Room, rather than separate line/source tables. Selection and adjustment relations remain foreign-key constrained. This keeps the P1–P3 schema small; favorites, aliases, editing/import/backup and translation schemas belong to their later phases. Room schema export is configured, but generated schema files require the deferred KSP/build run.

Provider disposition: [personal offline use basis and remaining uncertainty](providers.md). The prior live request returned HTTP 503; this change does not claim successful live provider verification. Strict artist comparison may leave some multi-artist formatting unmatched; manual search/matching belongs to P4.

## Evidence and pending acceptance

Source review and `git diff --check` were performed. That checks repository formatting, not compilation or runtime correctness.

Authored but **not executed**:

- Qualification, player arbitration and recording-identity tests.
- Room occurrence deduplication, metadata enrichment and selection-protection tests.
- Parser/timeline/clock, matcher, concurrent cache, mocked HTTP and rate-limit tests.
- Reader stale-result/timing-recovery tests and overlay state-transition tests.
- Compose saved-original and unavailable/retry reader tests.

**M1, M2 and M3 acceptance remains pending.** No passing test suite, generated Room schema, APK, screenshots, measured battery behavior or verified player compatibility is claimed. The implementation is ready for the deferred compile/test/device pass; failures found there must be fixed before calling it a usable daily prototype.

## Device script when verification resumes

1. Build/run tests with the existing SDK. Inspect generated Room schema and commit it after review.
2. Install on the S25 FE; grant music/overlay/notification access and select a music app. Start tracking.
3. Play 6 seconds, pause 14, play 4: verify exactly one history play. Seek forward early and verify it does not bypass qualification.
4. Open lyrics; check lookup, persistence, offline reading, seek/pause/speed and timing corrections. Compare live/remix versions and uncertain-match behavior.
5. Switch songs during a slow request; verify old words clear and late responses cannot replace the new song.
6. Drag, resize, rotate and enlarge system text. Read long lines and non-Latin lyrics over another app. Exercise TalkBack move/resize actions.
7. Pause 15 seconds and resume: automatic handle restores. Manually minimize, change songs and resume: handle remains. Close and change songs: panel stays closed. Reopen from notification.
8. Lock/unlock; revoke overlay/music permissions; stop tracking; kill/restart the process. Check window cleanup, retained library, no background restart and initially hidden overlay.
9. Exercise Wi-Fi-only with connectivity changes and queue cancellation. Repeat across the user's players; record versions and actual behavior.

Next planned scope is P4. Editing, import, favorites, backup and translations are not represented as completed here.
