# Privacy

Singalong reads metadata and playback position from music apps you select. It doesn’t record the microphone or capture playback audio. Lyrics, translations, history, favorites and edits stay in the app’s local database. Android backup is disabled; exporting a backup is your choice.

## Tracking and files

Tracking starts through the app or an explicit Quick Settings tile tap. Adding the tile or opening the shade doesn’t start it. The tile adds no runtime permission.

Folder lookup reads only the folder and subfolders you select through Android’s document picker. Audio is read locally for metadata, never uploaded. Filename-derived search hints may be saved and included in backups. Scans respect Wi-Fi-only lookup, support cancellation and don’t request broad storage permission or permanent folder access.

Lyric cards render locally from selected original lines, optional song details and artwork. Album-art palettes are also generated locally. Drafts and artwork are temporary; selected source images aren’t changed. Temporary PNGs are cleaned up as new exports are created.

**Share PNG** grants the receiving app temporary access; that app controls subsequent handling. **Save PNG** writes to your chosen destination. Cards exclude audio filenames and folder locations, but suggested PNG names contain the song title and selected line numbers. Provider attribution remains in the saved lyric record. Nothing is automatically uploaded.

## External services

Lyric lookup sends song search details, including filename-derived hints, to LRCLIB and lyrics.ovh. Providers also see ordinary network information such as your IP address. Retrieved lyrics retain source attribution.

Optional English translation sends lyric text to MyMemory after consent. MyMemory retains submitted text and applies its own limits and [terms](https://mymemory.translated.net/terms-and-conditions). Disabling translation doesn’t remove the originals.

**Open in Google Translate** copies the full original lyrics to the clipboard, replacing its contents, and attempts to prefill a browser with English output. Long text or receivers that ignore the link may require manual paste. Text can appear in browser or clipboard history and follows your device’s clipboard-sharing settings. This action uses Google’s terms and is separate from Singalong’s Wi-Fi-only lookup setting. Results aren’t saved back automatically.

ML Kit identifies language on the device. Google states that input and results are processed locally, but the SDK may contact Google for updates and send usage/performance metrics. See [ML Kit terms and privacy](https://developers.google.com/ml-kit/terms). Singalong has no separate app analytics service.

## Diagnostics and deletion

The app keeps one bounded Java/Kotlin crash log in private no-backup storage. It records versions, a timestamp, exception types and code locations; exception messages, lyrics and song details are excluded. A next-launch prompt lets you review and export it. Logs are never uploaded automatically, and not all crashes or app freezes can be captured.

The website uses GitHub Pages and requests public GitHub release metadata. It has no app analytics or third-party fonts. The feedback form is a separate service; you choose attachments and submit the report yourself.

Use **Settings → Library & backup** to export or clear supported data. Clearing Android app storage removes local settings and content. Save any backup you want to keep first.

ZIP backups contain only the sections you choose: full lyrics (including edits, timing and saved translations), source links/metadata, song metadata, play history and all saved settings (including selected music apps, tracking and translation choices). Song titles and artists accompany lyrics and history for matching; full song metadata also includes saved filenames. Audio files and album-art images are not copied. Backups are not encrypted; the destination you choose controls who can access them. Restoring settings is optional; current tracking and translation-consent choices are retained and Android permissions are never granted by restoring a file.
