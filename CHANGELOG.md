# Changelog

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
