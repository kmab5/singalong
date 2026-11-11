# Floating controls and selective backups

Work stays on `feat/floating-controls-selective-backup` until phone testing is confirmed. No release tag or merge is part of this change.

- Floating lyrics: start/center/end alignment, independent current/other-line sizes, and optional word highlights using supplied word timestamps. No glow or drift. Preserve line following when word tracking is off.
- Backups: choose full lyrics, source links/metadata, or no lyrics; choose song metadata, play history and settings independently. Audio files are not copied.
- Export a ZIP with an index, separate metadata/settings/history documents and individual lyric files under `lyrics/`. Keep legacy JSON imports working. Links-only restores retain source references without inventing lyric text.
- Restore merges included sections without clearing omitted data. Keep existing selections and personal edits. Restoring appearance settings is optional; permission, tracking and translation-consent state are not restored.
- Validate archive structure, size and relationships before database writes. Test full/partial/legacy round trips, invalid archives, repeated restore and word-highlight eligibility. Build, lint and compile device tests locally; physical checks remain with the user.
