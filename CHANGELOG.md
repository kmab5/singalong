# Changelog

## 1.2.2 — prepared

- Add arm64-v8a, armeabi-v7a, x86 and x86_64 APKs alongside the universal APK in one release.
- Keep public release attachments to APKs; retain crash mappings, build metadata and checksums in CI artifacts for 90 days.
- Show architecture downloads on the website when available.
- Condense project documentation into current guides.

## 1.2.1 — 2026-11-07

- Add a Quick Settings tile to start or stop tracking.
- Reflect the live session, handle incomplete setup and require unlocking for locked-device taps. Opening the shade doesn’t start tracking.
- Keep the existing tracking notification unchanged.

## 1.2.0 — 2026-11-07

- Add lyric cards with eight layouts, seven fonts, curated palettes and a local album-art palette, optional artwork and PNG sharing.
- Add library sorting and cancellable folder lookup with duplicate prevention.
- Support saved LRCLIB word timing, adjustable word tracking and improved lyric matching.
- Add a Google Translate handoff with full-text clipboard fallback.
- Improve floating controls, lyric styling and card editing.
- Fix empty-lyrics folder saves and late writes after deleting downloaded lyrics.
- Rebuild the website with interactive illustrations and stable APK downloads.
- Automate signed releases for new versions pushed to main.

The 1.1 work and intermediate release candidates were incorporated into 1.2.0.

## 1.0.0 — 2026-11-02

- First stable release: floating lyrics, offline library, filename-aware search, optional translations, personal editing and line timing.
- Signed APK updates through GitHub Releases.
