# Singalong 1.2.0

Lyric cards, album-art palettes and word syncing, with the same signing identity for updates over earlier release builds. Requires Android 12 or newer.

- Long-press a lyric to create a card from original lines. Choose eight layouts, seven fonts, ten curated palettes or a palette generated from the album cover.
- Customize artwork size/corners, text size/weight/spacing/placement, background and a sized corner logo with optional name. Nearby text wraps around the logo. Emphasize selected lines with bold/glow styling.
- Share or save a PNG named after the song and selected line numbers. Hidden foreground covers still supply Photo backgrounds and palette colors. All card rendering and color extraction happen locally.
- Empty provider lyrics no longer reach folder-save assertions. Late translation/timing writes are discarded when their lyric version has been deleted.
- Includes the previously prepared 1.1 improvements: word syncing, word-tracking switch, folder scans, library sorting, responsive floating controls and Google Translate’s clipboard fallback.

The website now includes an interactive floating-lyrics demo, card palette/format previews and a direct APK download. The APK, matching crash mapping, build metadata and SHA-256 checksums are attached to this release.

See the [integration checkpoint](https://github.com/kmab5/singalong/blob/v1.2.0/docs/validation/1.2-main-checkpoint.md) for native verification and remaining phone checks. Device compatibility is limited to the recorded testing; compiled instrumentation tests have not been run on an emulator.
