# singalong — personal Android app design

Status: scope confirmed by the user on 2026-11-01; implementation has not started.

## Purpose and release boundary

Follow music playing in another Android app, retrieve and retain lyrics, and open floating lyrics with one deliberate action. Original-language lyrics are always primary. Optional online English translation is secondary and must never block original lyrics.

Work locally. The user intends to create a GitHub repository named `singalong`; publishing or pushing is not part of this design task. The starting directory contains no app or Git repository.

The first release is a personal, sideloaded APK. No accounts, backend, paid services, API keys, audio recording, fingerprint recognition, playback controls, timestamp authoring, generated word timing, or Play Store release are included. Existing line timestamps are supported. Translation may ship unavailable if no suitable free provider exists.

## Confirmed platform and compatibility

- Native Android; Kotlin and Jetpack Compose are the proposed implementation stack.
- Minimum Android 12, API 31. Select and pin a current stable compile/target SDK and compatible build dependencies during setup; minimum and target are separate decisions.
- Primary physical test device: Samsung S25 FE, Android 16.
- Named compatibility targets: Samsung Music, Spotify, SoundCloud, Musicolet and AMPlayer. Verify actual installed package identifiers rather than guessing them from display names.
- Test named players on a best-effort basis and report limitations individually. Unavailable apps remain explicitly untested. Universal compatibility is not a release promise.
- Use API 31 emulator coverage in addition to the physical device. A successful Android 16 test does not prove Android 12 compatibility.

## Tracking, selection and collection

Users explicitly choose eligible players during onboarding or in settings. An unselected browser, podcast player or music app cannot create history or fetch lyrics.

While tracking is enabled, follow the most recently observed transition into playing among eligible sessions. Hold the selected player through an ordinary pause. Provide a manual session override; proposed lifetime is until the user returns to automatic selection or that session disappears. If attaching to several already-playing sessions, use the platform priority order as the deterministic tie-breaker.

Qualify automatic collection after 10 seconds of observed playing time for a recording. Pauses and buffering do not count. Seeking forward does not add listened time. Ordinary pause/resume retains accumulated time for that play. A different recording starts a new qualification window.

Tapping `sing along`, or receiving a new song while the lyrics panel is open, requests lyrics immediately. This bypasses fetch delay, not the history qualification threshold. A minimized edge handle does not count as an open reading panel; background collection then uses the normal 10-second threshold.

Save a history entry even if lookup fails. History displays one row per recording with latest qualified-play time and total play count. Store lyrics versions separately from play history. Callback duplication, pause/resume and metadata enrichment must not increment play count again.

Proposed counting rule: count once per qualified playback occurrence; a recording change or a credible end-to-start repeat creates a new occurrence. A seek to the beginning alone does not. Ambiguous repeat behavior must be documented per player rather than inventing listening events. Use unique internal occurrence IDs to make updates idempotent.

Network access uses Wi-Fi or mobile data by default. Offer Wi-Fi-only mode. When restricted or offline, use saved lyrics and queue missing background lookups for the next eligible active tracking period. Explicit `fetch using mobile data` may override Wi-Fi-only for that request. Disabling tracking stops background collection and retries.

## Lifecycle and permissions

Persist the user's tracking preference separately from an in-memory authorization for the current app process. After process death or reboot, callbacks cannot restart collection until the user opens the app. Opening it resumes tracking if the saved preference is enabled. If Android rebinds the notification listener in a new process, it remains inactive until that launch. No boot-start or sticky service restart is required.

Closing or manually minimizing the overlay must not disable tracking. Disabling tracking ends the live overlay session; saved-library reading remains available. After restart, overlay state starts hidden regardless of its previous visibility.

Explain notification-listener access, eligible-player selection and notification permission separately. Request overlay permission when floating lyrics are first requested. Permission denial leaves manual search/library reading usable. If notification permission is denied on a version that requires it, explain that notification reopening is unavailable and retain the in-app button.

Use media sessions without microphone, audio capture, accessibility-service access or broad file access. Do not persist unrelated notification content. Import and backup use the system document picker.

Engineering gate: verify a user-started foreground tracking/overlay service with an appropriate service type and truthful subtype declaration on API 34+. `specialUse` is the candidate for this personal app; do not pretend the app itself plays media. Start from visible UI or an allowed notification interaction, not arbitrary background callbacks. Prefer the system-bound listener where sufficient. Document the verified lifecycle before building the remaining features. Foreground service status does not guarantee survival against OEM or user termination.

## Lyrics retrieval and matching

Use a replaceable provider interface. Evaluate LRCLIB first through its documented API. Only integrate free supported access with no user key; do not depend on private player tokens or fragile undocumented scraping. Genius and Musixmatch integrations are not promised.

Order: selected personal version, selected stored provider version, local candidate match, network exact lookup, network candidate search. Normalize comparison keys but preserve original display strings, featured artists and recording qualifiers. Title/artist/album/duration/version markers inform matching; a title-only database key is insufficient.

Save and display the best plausible result even when uncertain. Show `possible match` and `change match`; retain source attribution and provider IDs. An empty result or clearly unrelated candidate yields `lyrics not found` instead of fabricated content.

Confident matching is a prerequisite for automatic timing. A materially different duration or live/remix qualifier yields manual reading even if that candidate includes timestamps. `try sync` explicitly opts into those timestamps. Switching matches also switches the associated correction and translation.

Cancel or detach obsolete requests when playback changes. A late response may populate its own recording's library entry, but can never overwrite the current song's UI. Deduplicate in-flight lookups and enforce provider rate limits. Separate not-found, network failure, rate limit, instrumental, and malformed-response states. Respect retry-after; do not repeatedly fetch every playback tick.

Record source/attribution/retention metadata. Public API accessibility does not establish permanent storage rights. The desired product behavior is indefinite storage, subject to verified provider permissions. If a provider cannot support retention, omit that provider from automatic permanent collection or document a permitted alternative; do not silently promise unavailable offline storage.

## Synchronization

Estimate position only from usable player state: base position plus monotonic elapsed time multiplied by playback speed while playing. Freeze during pause/buffering; replace estimates on authoritative seeks and updates. Clamp to known duration. Unknown or invalid position is not zero.

Resolve the current line from parsed timestamps, not text length or evenly divided duration. Support repeated timestamps, repeated chorus lines, multiple timestamp tags per line, empty timed lines and embedded LRC offset. Preserve stable line identities independently of translated text.

Save a correction per recording and selected lyrics version. Positive delay moves a lyric later: `effectiveTime = playerPosition - delayMs`; negative delay moves it earlier. Expose `earlier`, `later`, displayed seconds and `reset`. Proposed step is 0.25 seconds with an editable numeric value for larger adjustments.

Missing timestamps or playback position gives manual scrolling with `timing unavailable` when relevant. Returning timing offers `resume sync`; it must not unexpectedly jump a user reading manually. Manual scrolling suspends following until `back to current line` is tapped.

During an instrumental interval explicitly established by timing data, show the upcoming lyric with a countdown. A large gap between line-start timestamps alone is not proof that the previous sung line has ended. Before the first timed line, a countdown is valid. Do not create word-level highlighting from line-level timestamps.

## Overlay state and interaction

Default: current original lyric plus its English translation if enabled and available. With translation off, show the original only. Allow wrapping; never silently truncate the sung text or shrink the font to fit. Expand to view surrounding lines or open the full reader. Untimed content uses a scrollable manual-reading presentation.

The window has a dedicated drag handle, adjustable size/opacity/text size, minimize, close and expand actions. Playback controls are absent. Keep height stable during ordinary line changes; long text can scroll inside or expand deliberately. Clamp saved geometry to current safe bounds after rotation and display-size changes. Provide accessible move/size alternatives to dragging.

States:

| Current state / event | Result |
| --- | --- |
| Hidden + explicit show | Open compact panel; fetch immediately |
| Visible + new song | Clear old lyrics immediately; show new metadata/loading; fetch immediately |
| Visible + close | Hidden; future songs do not reopen it |
| Visible + manual minimize | Manual edge handle; future songs and resume do not expand it |
| Visible + paused for 15 seconds | Automatic edge handle |
| Automatic edge handle + playback resumes | Restore previous visible presentation |
| Either edge handle + tap | Open panel |
| Any state + process restart | Hidden |
| Any state + tracking disabled | Hidden |

Pause before the 15-second deadline freezes lyrics/countdown. Resume cancels the deadline. A manually closed or minimized panel cannot be reopened by an old timer or callback. Proposed new-track rule: if the panel was automatically minimized and eligible playback starts, restore it; manual state always wins.

Use a tracking notification with `show lyrics` and an explicit stop-tracking action. Notification reopening targets the current eligible song, not stale metadata embedded when the notification was built. If overlay permission has been revoked, route through visible permission recovery.

Do not display lyrics on the lock screen in this release. Suspend rendering when the display is off; retain collection only while the authorized tracking process remains active. Respect system surfaces that suppress overlays.

## Screens and visual system

Navigation: `now`, `library`, `history`; settings in the toolbar. Now presents the selected player, current recording, lookup state and `sing along`. Library offers search, favorites, saved/offline status and local versions. History lists unique recent recordings, last play and play count, including missing lyrics.

Opening a non-playing saved song opens the full-screen reader without changing the active overlay. Reader supports manual reading, selecting versions, simple edits, import, favorite, timing adjustment when applicable, source information and translation visibility. Import/search are available even when no media session exists.

KMAB dark-only first release. Follow `brand-kit/02_COLOR_SYSTEM/design-tokens.json`, which the repository README identifies as canonical, rather than conflicting root-level legacy typography.

| Role | Value |
| --- | --- |
| Background | #0E0E11 |
| Surface | #17171B |
| Raised surface | #1E1E24 |
| Primary text | #EDEBE4 |
| Secondary text | #9A9AA6 |
| Primary accent | #B24BFF |
| Confirmed saved status | #3DE08A |
| Informational caution | #F5C451 |
| Display | Inter Display, restrained use |
| Lyrics and UI | Inter; appropriate script fallback fonts |
| Timing and utility | PT Mono |

Use a 4/8/12/16/24/32 dp spacing scale and 16 dp panel corners. Main signature: a purple cue next to the active original/translation pair. Translation is subordinate in size, never unreadably faint. Small-body contrast must meet 4.5:1; validate against the actual composed overlay background. Default opacity is fully readable; limit unsafe transparency behind text or provide an opaque text backing. Do not use dim brand tokens for essential text.

Native touch targets are at least 48 dp. Support font scaling, TalkBack descriptions, right-to-left scripts, multiline translation and reduced motion. Do not automatically announce every advancing line through TalkBack. Preserve lyrics and artist capitalization; lowercase applies to app headings and controls. No changing album-driven palette, decorative equalizer or animated background.

The layout details and control placements are proposed implementation defaults within the approved brand direction, to be visually reviewed before final polish.

## Manual lyrics and editing

Manual search allows correcting title/artist and selecting another match. Import UTF-8 `.txt` and `.lrc` through the document picker with preview and recording association. Show encoding/format errors without destroying existing versions.

`edit text` on timed lyrics preserves timestamps and line identity. Users may change words but cannot insert/remove timed lines. Save a new personal version, never mutate the provider original. `create untimed version` makes an unrestricted text copy and permits adding/removing/reordering lines. Editing untimed lyrics uses the simple text editor.

User versions remain selected until explicitly changed. Provider refreshes cannot override selection. Any changed original text makes an old translation inapplicable; retain history if useful but never display it as the translation of the new content. No time-sync authoring UI in this release.

## Translation

Off initially. When enabled, opening a non-English lyric version requests English translation if a valid stored translation is absent. Do not translate the whole background-collected library. Original content appears immediately; translation has independent loading/error state.

Only free online services requiring no account/key are eligible. Verify availability, supported access, attribution, retention, language coverage and rate limits before integrating. No suitable provider is an accepted state, visibly explained in settings. This does not block release. No fake translation or automatic fallback to paid infrastructure.

Bind each translation to immutable lyric-version ID/content hash, source language, target English, provider/version and stable line IDs. Translate in context where the provider supports it, but validate segment alignment. If line mapping cannot be established, do not attach arbitrary translated sentences to timestamps. An unsupported or uncertain mixed-language result remains untranslated with an actionable message. Never guess language from artist nationality.

Cache successful translations with their lyrics subject to source permissions. Reopening reuses valid cached content, including offline. Changes to source text or version invalidate association. Translation is meaning-oriented, not a newly singable adaptation. Explain that online translation sends lyric text to the chosen service.

## Persistence and backups

Proposed storage: Room for recordings, lyric versions/lines, selections, translations, adjustments and history; DataStore for preferences. Use UUIDs for records and versions to support stable backup merges. Do not key versions only by their text; timestamps/source/recording identity matter.

Store local lyrics indefinitely until user deletion, within provider permissions. Provide storage usage, individual deletion and clearing downloads with a preserve-favorites option. Personal edits/imports require explicit inclusion in bulk deletion. Deleting history does not delete lyrics. Deleting a selected version requires explicit fallback selection, never a dangling reference.

Backup is a versioned UTF-8 JSON document through the system picker containing recordings, lyrics, translations, selections, adjustments, favorites, history summaries and preferences. Exclude credentials, permission grants, session tokens, active tracking authorization, notification content and cached artwork. Proposed maximum import size: 50 MiB, with a clear rejection before mutation. Parse/validate before a single transactional merge. Reject unsupported newer schema versions without changing data.

Restore merges: UUID and content fingerprints deduplicate identical versions; retain distinct personal versions; keep current explicit selections, favorites and preferences on conflicts, while filling absent values. Merge history by latest last-play timestamp and maximum overlapping count rather than summing. This intentionally avoids double-counting but can undercount disjoint histories; cross-device exact event reconciliation is outside scope. Repeated restore is idempotent. Export preferences for portability, but restoring into an existing installation does not overwrite current preferences automatically. Never activate tracking as a side effect of restore.

## Acceptance and delivery

Deliver an installable APK, build instructions, a compatibility matrix with actual results, and known limitations. No physical-device or battery claim without measured evidence. Device tests may require the user's phone connection, permission grants and player accounts.

Required scenarios: 10-second qualification excluding pauses; skipped songs; duplicate callbacks; immediate overlay requests; stale lookup completion; repeated songs; offline cache; wrong-match manual mode; seek/pause/speed changes; manual scroll recovery; 15-second auto-minimize; manual-close persistence; process restart gated until app launch; permission revocation; LRC/text import; timed edit invariants; personal-version protection; translation invalidation; idempotent backup restore; malformed-backup atomicity; font scaling/RTL/rotation.

Working release gates: reproducible build and unit tests; API 31 instrumented/emulator smoke tests; API 36 physical-device checks where available; honest compatibility matrix. Free provider/retention feasibility and Android service lifecycle are investigated early, before investing in polish.

## References

- [Android media sessions](https://developer.android.com/reference/android/media/session/MediaSessionManager)
- [Android playback state](https://developer.android.com/reference/android/media/session/PlaybackState)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Background service-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [LRCLIB API documentation](https://lrclib.net/docs)
- [KMAB source of truth](https://github.com/kmab5/kmab-brand/blob/main/README.md)
- [KMAB canonical tokens](https://github.com/kmab5/kmab-brand/blob/main/brand-kit/02_COLOR_SYSTEM/design-tokens.json)
- [KMAB writing guidance](https://github.com/kmab5/kmab-brand/blob/main/brand-kit/08_WRITING/WRITING_STYLE.md)

Design inputs: frontend-design and ui-ux-pro-max. Broad entertainment-site recommendations were rejected as unsuitable; native Compose accessibility and the user's brand take precedence.
