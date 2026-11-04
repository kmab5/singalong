# Singalong 1.1.0-rc.4 — testing candidate

Version 1.1.0-rc.4 (13). Prepared for phone testing; this update has not been merged or published. The latest public release remains v1.0.0. Merge and official 1.1.0 publication await the user's approval after testing.

- Glow now follows supplied LRCLIB word timestamps, including gaps and unequal word durations; line-only lyrics keep estimated progression. Version choices distinguish Word-synced, Word-synced · some lines, Line-synced and Untimed.
- Word timing is retained in saved versions and backups. Editing a line's text or timestamp clears its word timing in the personal copy; the original remains saved.
- The user confirmed rc.3 Glow animation is working. Its appearance and the black-shadow correction are retained; verification of the new word-timing behavior is pending.
- The Now animation settings are named Gentle and Glow. Untimed text is unaffected by the current timed-line size setting when estimated sync is off.
- New automatic lyric choices prefer timed matches, then the closest known song duration. The version list uses the same preference. Explicit selections and personal versions remain saved.
- Real timestamps start following automatically even for an uncertain match; the uncertainty label stays visible. Refreshing the same version preserves a manual following pause. Untimed estimated sync remains opt-in.
- Long-press dragging of minimal lyrics is unchanged, as requested. Folder scanning, sorting and responsive floating controls from rc.1 remain available.
- Translation alternatives are documented for discussion; the provider is unchanged.

Requires Android 12 or newer. Install over an existing release-signed installation to retain data. The signing key is unchanged. Debug APKs use a different key.

To look for word timing on an already cached song, open **Find, import or edit lyrics → find lyrics**, then select a **Word-synced** version if one is returned. Existing cached versions are not silently replaced. Word timing coverage depends on provider contributions.

See [the latest checkpoint](docs/validation/word-sync-checkpoint.md) for word-sync evidence and phone checks, [the previous checkpoint](docs/validation/glow-selection-checkpoint.md) for other candidate changes, and [translation research](docs/translation-options.md) for options. Same-key packaging includes APK/checksum/build metadata and the matching R8 mapping. No new release tag is created by this work.
