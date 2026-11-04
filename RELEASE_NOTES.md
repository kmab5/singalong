# Singalong 1.1.0-rc.5 — testing candidate

Version 1.1.0-rc.5 (14). Prepared for phone testing; this update has not been merged or published. The latest public release remains v1.0.0. Merge and official 1.1.0 publication await the user's approval after testing.

- Settings → Now → **Word tracking** is on by default. Turn it off to stop word highlighting, word motion and word-clock processing while keeping line following. The choice persists and saved word timestamps remain intact.
- Lyric options → **Open in Google Translate** opens the complete selected original lyrics with English output. Google has a 5,000-character input limit, explained for longer songs; oversized links are disabled without truncation. External results are not saved into Singalong.

- Glow now follows supplied LRCLIB word timestamps, including gaps and unequal word durations; line-only lyrics keep estimated progression. Version choices distinguish Word-synced, Word-synced · some lines, Line-synced and Untimed.
- Word timing is retained in saved versions and backups. Editing a line's text or timestamp clears its word timing in the personal copy; the original remains saved.
- The user confirmed rc.4 word syncing and the Glow animation work. Their appearance and the black-shadow correction are retained. The new switch and external Translate action await phone testing.
- The Now animation settings are named Gentle and Glow. Untimed text is unaffected by the current timed-line size setting when estimated sync is off.
- New automatic lyric choices prefer timed matches, then the closest known song duration. The version list uses the same preference. Explicit selections and personal versions remain saved.
- Real timestamps start following automatically even for an uncertain match; the uncertainty label stays visible. Refreshing the same version preserves a manual following pause. Untimed estimated sync remains opt-in.
- Long-press dragging of minimal lyrics is unchanged, as requested. Folder scanning, sorting and responsive floating controls from rc.1 remain available.
- Translation alternatives are documented for discussion; the provider is unchanged.

Requires Android 12 or newer. Install over an existing release-signed installation to retain data. The signing key is unchanged. Debug APKs use a different key.

To look for word timing on an already cached song, open **Find, import or edit lyrics → find lyrics**, then select a **Word-synced** version if one is returned. Existing cached versions are not silently replaced. Word timing coverage depends on provider contributions.

See [the latest checkpoint](docs/validation/word-tracking-controls-checkpoint.md) for this candidate's evidence and phone checks, [word-sync evidence](docs/validation/word-sync-checkpoint.md) and [translation research](docs/translation-options.md). Same-key packaging includes APK/checksum/build metadata and the matching R8 mapping. No new release tag is created by this work.
