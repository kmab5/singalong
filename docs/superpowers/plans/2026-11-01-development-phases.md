# singalong — development phases and checkpoints

Status: P0–P5 source implemented (P5 online translation unavailable); build, lint, test execution and device verification deferred by the user. P6 not started. Updated 2026-11-01. See [P1–P3 checkpoint](../../validation/p1-p3-checkpoint.md).

This roadmap sequences the [implementation tasks](2026-11-01-singalong.md) against the [confirmed design](../specs/2026-11-01-singalong-design.md). It adds delivery checkpoints without changing confirmed product behavior.

## Repository baseline

- Existing repository: `D:/Code/singalong`.
- Remote: `origin`, `https://github.com/kmab5/singalong.git`.
- Inspected initial commit: `304d17f`.
- Planning documents are currently local and untracked. No application code exists yet.
- Preserve existing Git configuration. This planning task neither pushes changes nor creates remote issues, milestones or releases.

Implementation update: P0 source is on local branch `feat/p0-foundation`. The user directed that builds and emulator installation be skipped for now and that development use the existing SDK. No final APK or passing M0 is claimed. See [P0 evidence](../../validation/platform-feasibility.md). The original baseline statements above describe the pre-implementation inspection.

## How execution will work

Phases are ordered by dependency. Provider feasibility and a basic native overlay are tested first, before substantial library or visual polish work. The first useful end-to-end APK arrives at P3; P4 adds the tools needed to build and protect a personal collection.

Each phase ends with a local checkpoint report containing:

1. Delivered behavior and relevant local commit IDs.
2. Commands/tests run and their actual results.
3. APK path when the phase produces an installable build.
4. Screenshots for visible changes.
5. Known limitations and any checks blocked by unavailable hardware/accounts.
6. The next phase and any scope decision required.

Checkpoints are verification gates, not automatic permission prompts. Once implementation is authorized, continue through routine passing gates. Ask only when a finding requires a product decision, credentials/device interaction, or changes the agreed scope. Do not claim a gate passed when its evidence is missing. Work independent of a pending physical-device check may continue, but that check remains open.

No phase is complete merely because its code compiles. Keep completed work in small local commits that pass the relevant checks. Do not mix unrelated user changes into commits. Remote publishing remains a separate action.

## P0 — feasibility and visual baseline

**Maps to:** Task 1; early provider investigation from Tasks 4 and 8; minimal overlay host from Task 6.

**Deliverable:** installable diagnostic shell, pinned build environment, permission flow, KMAB theme baseline and a tested basic overlay.

- Verify the JDK/SDK/toolchain and pin compatible dependency versions; minimum API 31.
- Implement a user-started tracking lifecycle and a notification-listener connection.
- Verify metadata/position access from at least one eligible player or a controlled test session.
- Open and close a small real overlay through the proposed user/notification interaction.
- Verify process recreation does not silently reactivate tracking.
- Check one documented free lyrics source for access, attribution, limits and retention suitability.
- Investigate free no-key online translation briefly; record a viable candidate or unavailable outcome. Do not let translation research consume the critical path.
- Produce Compose previews for compact lyrics, bilingual lyrics, now and library states using the canonical brand tokens. These establish hierarchy before detailed screens are built.

**M0 acceptance:** app installs; visible start and stop work; listener and overlay permissions behave correctly; background collection remains off after fresh-process recreation until app launch; a source is technically viable and its retention policy has a documented disposition. Visual previews respect original-lyrics priority, dark brand tokens and readable translation text.

**Checkpoint:** build/lint and process-gate tests; API 31 emulator checks; API 36 physical lifecycle evidence when connected. Document any missing device evidence explicitly. A service-lifecycle failure blocks dependent tracking work; a translation-provider failure does not.

## P1 — selected-player tracking and durable collection

**Maps to:** Tasks 2–3.

**Deliverable:** selected-player monitoring, stable recording identity, local history and collection scheduling.

- Add Room/DataStore and user-selected eligible apps.
- Select the most recently started eligible player with manual override.
- Implement 10-second actual-play qualification excluding pauses/buffering/seeks.
- Keep history as one row per recording with last-play time and play count.
- Ensure duplicate callbacks, metadata enrichment and pause/resume do not inflate counts.
- Support tracking disable, foreground-only resume after interruption and network policy.

**M1 acceptance:** 6 seconds playing, 14 seconds paused, then 4 seconds playing qualifies once at 24 seconds wall time. Excluded players never collect. Seeking forward never bypasses the threshold. Existing entries survive app restart. Live session objects are not persisted as resumable authorization.

**Checkpoint:** pure arbitration/clock tests, Room transaction tests and one actual-player tracking smoke test. Notification and overlay visibility remain separate from tracking preference.

## P2 — lyrics retrieval and synchronized reader

**Maps to:** Tasks 4–5; minimal now/reader surfaces from Task 9.

**Deliverable:** a usable in-app reader with local-first lookup, reliable timing and fallback states.

- Integrate the verified provider and parse plain/LRC lyrics.
- Match recordings using title, artist, album, duration and version qualifiers.
- Save the best plausible candidate; flag uncertainty rather than silently presenting it as certain.
- Reuse cached lyrics offline and distinguish errors, not-found and instrumental results.
- Estimate playback position from monotonic time, speed and authoritative updates.
- Add per-version timing corrections and manual-scroll recovery.
- Prevent late results for an old song from changing the current song's reader.

**M2 acceptance:** detect song → fetch → save → highlight current line works in the reader. Seeking and pausing update correctly. Uncertain matches and missing timing use manual reading. Cached lyrics open without network access. Explicit instrumental/countdown data is honored without inventing line end times.

**Checkpoint:** synthetic parser/matcher/timeline fixtures, mocked network failures/rate limits, offline reader test and one player seek/pause check. No live network dependency in routine unit tests.

## P3 — floating sing-along experience

**Maps to:** Task 6 and relevant Task 9 controls.

**Deliverable:** first daily-use prototype APK: notification → floating lyrics while music plays in another app.

- Connect the tested window host to the same live state as the reader.
- Default to the current lyric pair, with expansion for context and manual scrolling for untimed text.
- Implement drag/resize, text size, opacity, safe bounds and permission recovery.
- Implement the complete hidden/visible/manual-minimize/automatic-minimize state machine.
- Fetch immediately for song changes while visible and clear old content before showing loading state.
- Minimize after 15 seconds paused; restore only automatically minimized panels on playback resume.
- Keep playback controls out of the panel; retain explicit close and minimize semantics.

**M3 acceptance:** close stays hidden through subsequent songs; manual minimize stays minimized; automatic pause-minimize restores correctly; notification reopening targets the current song; process recreation starts hidden. Original lyrics remain readable with long lines, large fonts and busy underlying apps.

**Checkpoint:** reducer tests plus real over-app interaction on the S25 FE when available. Deliver an APK and a short test script: play, open, seek, pause 15 seconds, resume, minimize, next song, close, next song, reopen.

This is the first meaningful user-experience review. Record usability findings and fix failures against the existing scope before adding secondary features.

## P4 — personal library, editing and recovery

**Maps to:** Tasks 7, 9 and 10; backup follows stable version/editing semantics.

**Deliverable:** complete original-language workflow with durable personal content and safe backup.

- Complete now/library/history/settings, search, favorites and storage controls.
- Add manual matching, `.txt`/`.lrc` import and preview.
- Add timed text-only editing preserving line count/timestamps and unrestricted untimed-copy editing.
- Preserve provider originals and user-selected versions against future fetches.
- Export a versioned local backup; validate before transactional merging.
- Deduplicate identical data, retain conflicting personal edits and preserve current preferences/selections.

**M4 acceptance:** import → edit → select → restart retains the chosen personal version. Library browsing does not replace the live overlay song. Restoring the same backup twice is idempotent. Malformed/newer-schema backups cause no data changes. Deletion preserves favorites/personal work according to the explicit choice shown.

**Checkpoint:** database/editor tests; document-picker flow; empty-library restore; existing-library merge; injected rollback failure; saved-copy comparison. Personal edits are not considered safe until backup/restore tests pass.

## P5 — optional translation and visual refinement

**Maps to:** Task 8 and final design/accessibility work from Tasks 9 and 11.

**Deliverable:** coherent branded interface and either verified free translation or an honest unavailable state.

- Keep translation off initially and request it only for opened lyrics when enabled.
- Integrate a provider only if documented no-key/free access and permitted retention are verified.
- Store aligned translations against immutable lyric-version identity/content hash.
- Preserve original lyrics on network failure, unsupported language or invalid alignment.
- Invalidate old translation associations after edits; reuse valid saved results offline.
- Refine fonts, lyric pairing, spacing, motion, icons and all empty/error states.
- Validate RTL/script fallbacks, TalkBack, large text, contrast and 48 dp controls.

**M5 acceptance:** original reading never depends on translation success. Successful translated pairs stay aligned and survive offline reopening; edits cannot show stale translations. If no provider qualifies, settings clearly report unavailable and automated tests verify the fallback. There is no fake-success translation path.

**Checkpoint:** translation cache/alignment tests, bilingual layout screenshots and accessibility walkthrough. Reduced motion and font scaling are tested on real rendered UI, not inferred from token values.

## P6 — compatibility validation and personal release

**Maps to:** Task 11.

**Deliverable:** verified personal-release APK, reproducible build instructions, compatibility matrix and known limitations.

- Exercise Samsung Music, Spotify, SoundCloud, Musicolet and AMPlayer when installed/accessible. Record exact package, version and test date.
- Test metadata, timing, seeks, pauses, track changes, repeats, offline use, screen-off behavior and process termination.
- Run API 31 emulator checks and S25 FE Android 16 physical checks.
- Complete full unit/instrumented tests, lint, build and install smoke test.
- Record APK checksum and signing continuity instructions; keep keys and personal exports out of Git.
- Measure battery/performance only where a repeatable baseline is available; report evidence and limitations.

**M6 acceptance:** all required deterministic tests pass; no known crash/data-loss failure in the agreed flows; backup/restore is verified; the primary phone completes the end-to-end flow. Player limitations and untested apps are explicit. Translation unavailability is permitted. An unavailable physical phone prevents claiming a verified personal release, even if a candidate APK can be delivered.

**Checkpoint:** final report links evidence and installable artifact. No release publication or repository push is implied by this roadmap.

## Checkpoint tracker

| Milestone | Status | Evidence required |
| --- | --- | --- |
| M0 feasibility | Source implemented; verification deferred | Gate tests authored; initial red run recorded; provider assessment and Compose preview source delivered. Final build/device checks pending. |
| M1 collection | Source implemented; acceptance pending | Qualification/arbitration/database tests authored, not run; player smoke test pending |
| M2 reader | Source implemented; acceptance pending | Matching/parser/timing/cache/HTTP/stale-result tests authored, not run; live checks pending |
| M3 floating prototype | Source implemented; acceptance pending | State and reader tests authored, not run; APK and physical overlay walkthrough pending |
| M4 personal library | Source implemented; acceptance pending | Editing invariants, import, atomic/idempotent restore |
| M5 translation/design | Source implemented with unavailable online adapter; acceptance pending | Provider disposition, cache tests, accessibility screenshots |
| M6 personal release | Not started | Full checks, primary-device evidence, APK/checksum, matrix |

## Effort planning

Plan around 8–12 focused working sessions for the expanded scope, not one prompt per task. A rough allocation is P0: 1; P1: 1–2; P2: 1–2; P3: 1–2; P4: 2; P5: 1; P6: 1–2. Provider or device problems can extend this range; it is not a time guarantee. Re-estimate after M0 using actual toolchain, device and provider results.

The first floating prototype is targeted for the end of P3. Continue autonomously through authorized routine work; user input should primarily be physical-device permission/account access and feedback at M3, plus any genuine scope changes.
