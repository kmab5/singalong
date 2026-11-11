# Backups

In **Settings → Library & backup → Export backup**, choose the contents and save the ZIP somewhere you control.

- **Full lyrics:** original text, personal versions, timing and saved translations.
- **Links & metadata only:** source links and provenance. This cannot restore offline lyric text or personal edits; providers may disappear.
- **No lyrics:** leave lyric content and references out.
- **Song metadata:** album, duration, filenames and favorites. Audio files and artwork are not included.
- **Play history:** play counts and last-played dates.
- **Reading settings:** floating/Now appearance, sorting and Wi-Fi preference. Player access, tracking and translation consent stay on the device.

Lyrics and history carry song IDs, titles and artists even when full song metadata is excluded. This keeps the saved data associated with its song.

## Restore

Choose **Restore backup** and select a ZIP or an older Singalong JSON backup. Review the counts before restoring. Applying reading settings is optional and off by default.

Restore merges data rather than replacing the library. Existing selections and edits remain; omitted sections do not clear existing data. Repeated restore does not add the same records or play counts again. Links-only records do not appear as downloaded lyrics: search again when you need the words.

Backups are not encrypted. Keep private backups out of public repositories and choose storage you trust.

## ZIP layout

| Path | Contents |
| --- | --- |
| `index.json` | Format version, selected sections, file sizes and integrity hashes |
| `details.json` | Creation time, app version and recording identities |
| `songs.json` | Selected song metadata |
| `history.json` | Play counts and last-played dates |
| `settings.json` | Portable reading preferences |
| `lyrics.json` | Version-file references, provenance, selection, sync adjustments and saved translations |
| `lyrics/*.json` | Individual complete lyric versions, including supplied timing |

Unselected sections are omitted. Import validates the archive before changing the library, with limits of 50 MiB expanded total and 8 MiB per file. Edited or incomplete archives may fail integrity checks.
