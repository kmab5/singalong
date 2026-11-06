# Singalong 1.2.0-rc.1 — lyric card candidate

Version **1.2.0-rc.1 (16)** is prepared on the lyric-card feature branch for phone testing. It has not been merged, tagged or published. The latest public APK remains v1.0.0; main contains prepared 1.1.0 source.

Long-press an original line in Now, saved lyrics or the standard floating reader to open a draggable bottom sheet. Choose **Share lyric card**, select the lines you want and customize the live preview.

- Four starting templates: Glass, Midnight, Paper and Artwork.
- Square, portrait, story and landscape PNG formats.
- Font family, text size, alignment, custom hex colors, background treatment and image shade.
- Album-cover visibility, size and placement; independent lyric positioning and width.
- Local image selection for covers and backgrounds, plus available artwork from the matching music player.
- Original song title, artist, album, source attribution and Singalong/kmab branding retained on the card.
- **Share PNG** opens Android's Sharesheet; **Save PNG** uses the document picker. No new storage permissions.

The draft keeps its selected song and copied player artwork when playback changes. Layout overflow gives an explanation instead of silently dropping lyrics. The original lyrics, translations and library are unchanged. Minimal floating mode retains its existing long-press drag gesture; expand it for lyric actions.

Install the same-key APK over an existing release-signed installation. Android 12 or newer is required. Physical gesture, visual, picker and recipient-app checks remain with the user; compiled device tests are not claimed as executed. See the [feature checkpoint](docs/validation/lyric-card-checkpoint.md) and [privacy details](PRIVACY.md).
