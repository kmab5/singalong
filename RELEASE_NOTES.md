# Singalong 1.0.0

The first stable release of Singalong, a floating lyrics companion for Android. Version 1.0.0 (9); player and device support depends on the metadata and playback controls exposed by each music app.

- Floating lyrics with compact, expanded and minimal modes, bottom icon controls and an edge handle.
- Adaptive lookup/retry states; automatic docking when lyrics remain unavailable, returning after lyrics load.
- Filename-aware search, LRCLIB and lyrics.ovh sources, local lyric versions and offline reading.
- Optional English translations, simple text editing and opt-in per-line timing.
- Now-tab alignment, text size, line spacing, timed-line animation and tap-to-focus controls.
- A crash-log review/export prompt on the next launch and a link to the feedback form.

Requires Android 12 or newer. Install `singalong.apk` over an existing release-signed installation to retain data. The signing certificate is unchanged. Debug-signed installations use a different key.

Some apps, including sensitive Android Settings screens, intentionally suppress overlays while visible. Lyrics depend on provider coverage and metadata accuracy. Experimental sync for untimed lyrics is approximate. Audio recognition is deferred.

See `build-info.json` for the exact version/code/commit, `SHA256SUMS` for checksums and `mapping.txt` for decoding obfuscated crashes. Source is available from the release tag under GPL v3 with the documented ML Kit linking permission.
