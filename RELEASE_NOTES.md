# Singalong 1.1.0

Prepared source version **1.1.0 (15)**. The feature merge is authorized; this is not a published GitHub Release until a matching tag is explicitly approved and pushed. The current public download remains v1.0.0.

- **Word-synced Glow:** supplied LRCLIB word timestamps drive highlights, including unequal word durations and gaps. Line-only lyrics retain estimated progression. Version choices distinguish word-synced, partially word-synced, line-synced and untimed lyrics.
- **Word tracking switch:** turn off per-word processing and motion in Settings → Now while keeping line following. Saved timestamps remain intact; turn it back on to restore word behavior.
- **Google Translate fallback:** Lyric options copies the full original text before attempting to open Google Translate in a browser with English selected. Paste if Google opens empty. Longer lyrics may need sections; copying or launch failures are reported without truncating text. Results stay in Google Translate.
- **Folder lyric collection:** select a folder in Library to find and save lyrics for its audio files and subfolders using metadata and filename hints. Existing matches are skipped; scans can be cancelled. Audio stays on the device.
- **Library sorting:** organize saved lyrics by title, artist, album, recent playback or play count.
- **Reader and floating controls:** bottom actions wrap to fit, Now has an independent timed-line size and full-width setting selections, and line transitions and Glow motion are smoother. Long-press dragging of the minimized panel and tap-to-expand are preserved.
- **Better automatic selection:** prefer timed matches, then the closest known song duration. Explicit selections and personal versions remain saved. Real timestamps follow automatically; untimed estimated sync remains opt-in.

Word timing is retained offline and in backups. Editing a line's text or timestamp drops stale word timing from the personal copy while retaining the original. On a cached song, use **Find, import or edit lyrics → find lyrics** to look for additional word-synced versions. Coverage depends on provider contributions.

Requires Android 12 or newer. Install over an existing release-signed installation to retain data. The signing key is unchanged; debug APKs use a different key. In-app translation still uses MyMemory; the Google Translate button is an external handoff, not a new translation API.

See the [integration checkpoint](docs/validation/1.1-main-checkpoint.md), [website checkpoint](docs/validation/1.1-site-checkpoint.md), and [privacy details](PRIVACY.md). Automated evidence is separate from phone testing; the clipboard/browser change still needs validation on the user's device. Packaging includes the APK, checksums, build metadata and matching R8 crash mapping.
