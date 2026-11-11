# Floating controls and selective backups

The maintainer confirmed testing on 2026-11-11 and authorized merging to main, tagging and pushing 1.3.0 after adding release notes and the preferred-version export choice.

- Floating lyrics: start/center/end alignment, independent current/other-line sizes, and optional word highlights using supplied word timestamps. No glow or drift. Preserve line following when word tracking is off.
- Backups: choose full lyrics, source links/metadata, or no lyrics; choose song metadata, play history and settings independently. Audio files are not copied.
- Export a ZIP with an index, separate metadata/settings/history documents and individual lyric files under `lyrics/`. Keep legacy JSON imports working. Links-only restores retain source references without inventing lyric text.
- Restore merges included sections without clearing omitted data. Keep existing selections and personal edits. Export all persisted settings, including selected apps and tracking/translation choices. Optional settings restore preserves current tracking and translation consent; Android permissions are device-owned.
- Let the user change the backup-size limit or choose Unlimited for export and restore. Stream ZIP output and use temporary disk storage for archive reads, without a separate per-file byte cap.
- Validate archive structure, size and relationships before database writes. Test full/partial/legacy round trips, invalid archives, repeated restore and word-highlight eligibility. Build, lint and compile device tests locally; physical checks remain with the user.

- Export all saved lyric versions or only the selected/preferred version per song. Keep its timing, translations and provenance together without changing live selections.
- Show shared release notes in About and on the website, with labelled icons and colors for new features, improvements and fixes. Link the author credit to https://kmab5.github.io.
