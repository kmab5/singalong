# Backups

In **Settings → Library & backup → Export backup**, choose the contents and save the ZIP somewhere you control.

- **Full lyrics:** original text, personal versions, timing and saved translations.
- **Links & metadata only:** source links and provenance. This cannot restore offline lyric text or personal edits; providers may disappear.
- **No lyrics:** leave lyric content and references out.
- **Song metadata:** album, duration, filenames and favorites. Audio files and artwork are not included.
- **Play history:** play counts and last-played dates.
- **All settings:** every persisted preference, including selected players, tracking and translation choices, notification prompt state, appearance, sorting, Wi-Fi and the backup-size limit. Android-granted permissions, caches and temporary drafts are not preferences.

Lyrics and history carry song IDs, titles and artists even when full song metadata is excluded. This keeps the saved data associated with its song.

## Restore

Choose **Restore backup** and select a ZIP or an older Singalong JSON backup. Review the counts before restoring. Applying settings is optional and off by default. Selected music apps are restored; the current tracking and translation-consent choices remain unchanged. Permissions still need to be granted on the device.

Restore merges data rather than replacing the library. Existing selections and edits remain; omitted sections do not clear existing data. Repeated restore does not add the same records or play counts again. Links-only records do not appear as downloaded lyrics: search again when you need the words.

Backups are not encrypted. Keep private backups out of public repositories and choose storage you trust.

## ZIP layout

| Path | Contents |
| --- | --- |
| `index.json` | Format version, selected sections, file sizes and integrity hashes |
| `details.json` | Creation time, app version and recording identities |
| `songs.json` | Selected song metadata |
| `history.json` | Play counts and last-played dates |
| `settings.json` | Complete typed preference snapshot plus legacy reading preferences |
| `lyrics.json` | Version-file references, provenance, selection, sync adjustments and saved translations |
| `lyrics/*.json` | Individual complete lyric versions, including supplied timing |

Unselected sections are omitted. Import validates the archive before changing the library. Edited or incomplete archives may fail integrity checks.

## Size limit

Under **Library & backup → Limit backup size**, enter a maximum in MiB and tap **Apply**, or turn the switch off for **Unlimited**. The default is 50 MiB of uncompressed content, including metadata. The choice applies to export and restore; there is no separate per-file cap. Increase it or turn it off before restoring a larger backup.

Unlimited removes the configured byte cap. Available storage and memory still apply, and individual lyric records must remain valid. ZIP output is streamed; restore uses a temporary cache file that is removed when the operation finishes. Large restores need temporary free space and take longer.
