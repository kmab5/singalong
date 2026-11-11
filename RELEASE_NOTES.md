# Singalong 1.3.0

## ✨ New

- Align floating lyrics and set separate sizes for the current and surrounding lines. Follow supplied word timing with optional floating word highlights, without glow.
- Build a ZIP backup from the sections you want: full lyrics or source references, song metadata, history and all saved settings. Include every saved lyric version or just your selected/preferred version per song.
- Adjust the backup size limit or choose Unlimited, with no separate per-file cap.
- Read categorized release notes in About and on the website. The Samuel / kmab credit opens my homepage.

## ↗ Improvements

- ZIP backups stream lyrics into separate files to reduce memory overhead. Older JSON backups can still be restored.
- Preview backup contents and choose whether to restore settings. Settings exports include every persisted preference; restoring keeps current tracking and translation-consent choices unchanged.

## ✓ Fixes

- Restoring partial backups keeps distinct recordings separate, even when their titles and artists match.
- Selected backup data is validated before restore writes begin. If settings cannot be applied after the library restores, the app reports that partial result.

Requires Android 12 or newer. Install the universal APK if you are unsure which architecture to choose. All APKs use the existing signing key and can update an installed release. Backups do not include audio files; links-only backups do not include lyric text.
