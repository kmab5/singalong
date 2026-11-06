# Changelog

## 1.2.0 — prepared, not yet published

- Added a locally generated Album art palette using predominant cover colors and contrasting text. It updates with the cover, stays when the foreground cover is hidden, and is unavailable when artwork has no usable pixels.
- Integrates lyric cards: eight layouts, seven fonts, curated and automatic palettes, seven backgrounds, line emphasis, fine artwork/text sizing, corner branding with local wrapping, and named PNG share/save.
- Clearing downloaded lyrics now safely discards late translation and timing-adjustment writes instead of failing on a deleted version. HTTP adapters identify the current app version consistently.
- Updated current docs and the website with a labelled card illustration and prepared-versus-published status. Retains rc.2's empty-provider-lyrics folder-scan fix.

## 1.2.0-rc.4 — unreleased

- Added a fixed gap below card editor tabs. Hiding the foreground cover now preserves album artwork used as a Photo background.
- Continuous artwork sizing, a wider 20–180 text-size range, four additional palettes (Plum, Clay, Fog, Amber), and Halo, Wash, Dusk and Paper backgrounds.
- Logo overlays support all four corners, size, colored/monochrome styling and an optional app name. Text flows around the corner footprint without reserving an entire footer.
- Removed audio filename/location from card details and exports. Selected lyric lines can independently use bold/glow emphasis, retained through preview, export and draft restoration.
- Save PNG suggests `[song-title]-[line-numbers]-singalong.png`, using one-based source line numbers and ranges for selections. Filenames are sanitized and bounded.
- The latest supplied crash report was confirmed to be rc.1. The rc.2 empty-lyrics folder-import fix remains; this update does not claim a new crash diagnosis from that older trace.

## 1.2.0-rc.3 — unreleased

- Card editing now uses a stable full-screen dialog after the draggable lyric menu. Pinned preview/export actions, clipped scrolling, no stretch overscroll, equal-width pill tabs and consistent section/choice spacing address the recorded scroll-boundary movement and cut-off controls.
- Rebuilt the eight layouts around larger artwork, grouped song credits and spacious verses. Removed decorative rings/rules; the standalone Singalong mark aligns to the footer edge, with optional app name and left/right placement.
- Restored bounded text sizing, font weight, line spacing, verse position, artwork scale/corners, background treatment/photo shade and footer alignment. All seven fonts, palettes, formats and metadata switches remain.
- Retains the rc.2 empty-lyric folder-scan fix. The newer uploaded crash file identifies rc.1, before that fix.

## 1.2.0-rc.2 — unreleased

- Redesigned card editor with a pinned rounded-square preview, four compact control sections and eight distinct preset compositions. Coordinated palettes, three text sizes and named font choices replace position/width/shade sliders and hex inputs.
- Bundled Inter, PT Mono, Lora, Oswald, Caveat, Space Grotesk and Playfair Display for offline preview/export. Optional title, artist, album, filename, file location, cover and app name; only the icon stays by default. Source attribution is preserved in saved lyrics.
- Empty timed provider responses fall back to plain lyrics or count as not found. Folder scanning filters empty versions before saving, avoiding the reproduced save assertion crash while retaining instrumental gaps in real lyrics.

## 1.2.0-rc.1 — unreleased

- Long-press an original lyric in Now, saved lyrics or the standard floating reader to open a draggable context sheet and create a lyric card. Minimal-mode long-press dragging is preserved.
- Four card templates and square/portrait/story/landscape formats, with editable font, size, alignment, colors, backgrounds and lyric/cover positions. Select original lines without modifying saved lyrics.
- Cards retain song credits, original provider attribution and Singalong/kmab branding. Available local player artwork is snapshotted once; custom cover/background images use the system picker.
- Share PNGs through Android's Sharesheet or save through the document picker. Rendering is bounded and reports overflow rather than silently dropping lyric text. No new storage permissions.

## 1.1.0 — prepared, not yet published

- Added library sorting and selected-folder/subfolder lyric lookup using audio metadata and filename hints, with cancellation and duplicate prevention.
- Made floating actions responsive to panel width; preserved long-press movement of the minimized display and tap-to-expand.
- Added independent timed-line sizing, full-width setting selections, smoother line transitions and Glow word motion.
- Added stored LRCLIB word timing, word/line/untimed version labels and backup support. A persistent Word tracking switch stops word processing while retaining line sync and source timestamps.
- Prefer timed lyrics and closest known duration for automatic matching; preserve explicit selections and personal edits.
- Added a Google Translate handoff with English selected. Copy full original lyrics before opening, prefer a browser, and keep a clipboard fallback when the receiver ignores input or the URL is too long. Report clipboard/launch failures without truncating text.
- Updated release documentation, privacy details and the website's 1.1 preview while retaining downloads for the latest published release.

The following candidate entries record development history, not separate published releases.

## 1.1.0-rc.5 — unreleased

- Added a persistent Word tracking switch under Settings → Now. Turning it off stops per-word processing and motion while retaining line sync and saved word timestamps.
- Added Open in Google Translate to Lyric options, passing the selected original lyrics with English output. Complete text is preserved; long-input limits are explained and oversized browser links are disabled.
- The user confirmed rc.4 word syncing works. These two additions await phone testing before merge and official 1.1.0 publication.

## 1.1.0-rc.4 — unreleased

- LRCLIB Lyricsfile word segments and timestamps are saved offline and drive the existing Glow animation from the playback clock. Line-only lyrics retain estimated progression.
- Word-synced, partially word-synced and line-synced versions are labelled separately. Equally matching versions prefer word timing without replacing saved selections.
- Word timing round-trips through backups. Changed personal-edit lines discard stale word timing; unchanged lines retain it. Multiline timing segments offer explicit untimed conversion for editing.
- Bounded safe YAML parsing runs off the UI thread; malformed word data falls back to line timing. Existing signing identity and approved Glow appearance are preserved.

## 1.1.0-rc.3 — unreleased

- Corrected black overlapping text in Glow by making hidden words' shadows explicitly transparent. Word drift, enlargement, glow and stable wrapping are preserved.
- Added a device pixel-rendering regression test covering wrapped text, RTL text and outgoing focus.

## 1.1.0-rc.2 — unreleased

- Brighter Glow with slow word lift/drift and gentle enlargement; stable wrapping and eased line handovers. Now animation labels are Gentle and Glow.
- Timed-first, closest-duration ranking for automatic lyric selection and version lists; real timestamps automatically follow while match uncertainty remains visible.
- Preserved existing long-press dragging after the user's clarification.
- Documented translation alternatives without changing the provider; removed superseded local artifacts.

## 1.1.0-rc.1 — unreleased

- Floating actions wrap to fit narrow panels; dragging the minimized edge handle moves it without opening it.
- Persistent library sorting and cancellable folder/subfolder lyric lookup with duplicate prevention, network-policy handling and filename hints retained alongside original tags.
- Independent current timed-line size and optional Word glow, an estimated visual progression between existing line timestamps.
- Full-width settings selection panels and a non-destructive database migration for filename hints.

## 1.0.0 — 2026-11-02

- First stable release, promoted from 1.0.0-rc.1 with Android version code 9.
- Floating lyrics, offline library, filename-aware search, optional translations and per-line timing editing.
- Same-key signed APK distribution through GitHub Releases, with checksums and matching crash mappings.

## 1.0.0-rc.1 — 2026-11-02

- Bottom floating controls, tap-to-open Now and adaptive missing-lyrics docking/recovery.
- Independent Now typography, alignment, animation and tap-to-focus.
- Next-launch crash-log prompt with explicit review/export and feedback handoff.
- Versioned, certificate-verified GitHub signing and release workflows; no public release is claimed yet.
- GPL v3 with the ML Kit linking permission, privacy/community/release guides and indexed historical evidence.

## 0.3.3 — 2026-11-02

- Filename-aware lyric search preserving original metadata and uncertainty.
- Optional line timing while keeping simple text editing as the default.
- Debug/release automated verification: 78 unit tests per variant; phone checks separate.

## 0.3.2 — 2026-11-02

- Moved HTTP response read/close entirely onto OkHttp callbacks to address song-change cancellation crashes.
- Timed/Untimed version labels, lowercase kmab branding and local crash export.
- Static landing page with reusable PNG/SVG artwork.

Earlier development and evidence are preserved in the [documentation index](docs/README.md).
