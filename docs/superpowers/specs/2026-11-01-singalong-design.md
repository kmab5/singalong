# Singalong design

Singalong is my personal Android lyrics companion: follow selected music players, keep original lyrics available, and open a floating reader deliberately. This document consolidates the original design and subsequent implemented features.

## Platform and boundaries

Native Kotlin/Compose, Android 12+, compile/target SDK 36. Room stores library content; DataStore stores preferences. No accounts, backend, microphone recording, playback-audio capture, audio recognition or player controls.

Use selected media sessions without retaining unrelated notifications. Ask separately for music access, notification permission and overlay access. Manual search and library reading remain useful when access is denied. Imports, images, folder scans and backups use Android’s document picker.

## Tracking and lifecycle

Choose the most recently started eligible player; hold selection through ordinary pauses and allow a manual override. Unselected players must not collect data.

Ten seconds of observed playing time qualifies a history occurrence. Pauses, buffering, seeks and duplicate callbacks don’t add listening time or duplicate plays. Recording changes start a new occurrence. History and saved lyric versions are separate.

Opening lyrics requests lookup immediately, independently of history qualification. Closing floating lyrics doesn’t stop tracking. Disabling tracking stops collection, retries and the live overlay.

Saved tracking preference is separate from process authorization. A fresh process can resume only after opening the app or explicitly starting from the Quick Settings tile. Adding/viewing the tile never starts tracking. Locked taps require unlocking; missing setup opens the app. The tile reflects the same session state as the app and notification.

Overlays start hidden after restart, stay off the lock screen and respect Android’s overlay suppression. Foreground operation doesn’t guarantee survival against system or user termination.

## Lookup and timing

Prefer selected personal/stored versions, then local matches, then provider lookup. Preserve original metadata alongside conservative filename hints. Consider title, artist, album, duration and recording qualifiers; never fabricate lyrics or certainty.

Prefer supplied timing and closer known durations for automatic matches while retaining possible-match labels. Explicit selections and personal versions survive refreshes. Empty results produce a missing-lyrics state. Use bounded retries, provider cooldowns, cancellation and stale-result protection.

Playback estimates use monotonic elapsed time, speed and authoritative seek updates. Pause/buffering freezes progression; invalid position is unknown, not zero. Preserve LRC timestamps, offsets and stable line identities. Timing correction is per recording/version: positive delay moves lyrics later.

Word highlighting uses supplied timestamps when available. Line-only progression is estimated; untimed estimated sync is opt-in. Word tracking can be disabled independently of line sync. Manual scrolling suspends following until the user returns to the current line. Don’t invent instrumental gaps from line starts.

## Floating reader and visual design

Compact, expanded and minimal modes share the same live song state. Keep drag, resize, minimize, close and reopen actions distinct. Clamp geometry to safe bounds; retain accessible controls and readable wrapping.

Manual close/minimize persists through song changes. Automatic pause docking after 15 seconds can restore on resume. Missing-lyrics docking can recover when lyrics arrive, unless the user manually changed visibility. Closing the panel doesn’t disable background tracking.

Use semantic dark kmab tokens, Inter for reading/UI and PT Mono for utility text. Original lyrics stay primary; English subtext is secondary. Support 48 dp touch targets, scalable text, RTL, TalkBack and reduced motion. Now-reader styling and floating appearance are independent. Floating lyrics offer alignment, separate current/other-line sizes and optional supplied-timestamp word highlighting without glow.

## Library, edits and recovery

Keep favorites, versions and play history local. Support TXT/LRC import, simple editing, optional line timing, saved sorting and cancellable selected-folder lookup. Skip existing matches and never save empty misses.

Save edits as personal versions; preserve provider originals and explicit selection. Discard stale word timing for changed text and invalidate translations tied to old content.

Backups are selective ZIP archives with separate JSON sections and individual lyric files. Keep legacy JSON imports. Validate before transactional library merge; reject malformed or unsupported schemas without mutation. Deduplicate repeated restores and retain distinct recordings and personal versions. Keep existing explicit selections and favorites on conflicts; merge overlapping history counts by maximum rather than addition. Reading-settings restore is optional and applied separately. Exclude permissions, process authorization, credentials, audio files and transient artwork. Restore must not activate tracking or translation consent. See [backup contents](../../BACKUPS.md).

## Translation and cards

Translation is off initially and never blocks original lyrics. MyMemory receives text only after consent; save valid aligned results against the source version. Keep partial/error states honest. The Google Translate action is an explicit clipboard/browser handoff, with no automatic result import.

Lyric cards use selected original lines and a snapshot of the song. Provide eight layouts, seven fonts, curated/album-art palettes, optional artwork/details and PNG share/save. Render locally, preserve source images and report overflow instead of silently dropping text. Exclude audio filename/location; retain provider attribution in the saved lyric record. Minimal floating mode keeps its long-press movement gesture.

## Acceptance

Protect original content, stable state ownership, cancellation, backup atomicity and signing continuity with focused checks. A passing build is not physical-device acceptance. See the [phase roadmap](../plans/2026-11-01-development-phases.md), [testing](../../TESTING.md), [privacy](../../../PRIVACY.md) and [provider guide](../../PROVIDERS.md).
