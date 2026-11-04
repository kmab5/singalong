# Privacy and external services

Singalong reads metadata and playback position from music apps you select. It does not capture playback audio or record the microphone. Audio recognition is not implemented. Lyrics, translations, history, favorites and edits are stored in the app's local database. Android backup is disabled; exporting a backup is an explicit action.

Online lyric lookup sends song search details, including supported filename-derived hints, to LRCLIB and lyrics.ovh. Their servers see normal network information such as IP address. Downloaded lyrics retain source attribution and are not bundled with the APK or published through this repository.

The optional folder scan reads files only within a folder and subfolders selected through Android's document picker. Audio is read locally for metadata and is not uploaded. Filename search hints may be stored with saved recordings and included in exported backups. No broad storage permission or permanent folder access is requested. Scanning respects the Wi-Fi-only setting and can be cancelled; successful saves remain in the library.

Optional English translation sends lyric text to MyMemory after consent. MyMemory stores submitted text and applies its own limits and [terms](https://mymemory.translated.net/terms-and-conditions). Originals remain available when translation is disabled or unavailable.

Choosing **Open in Google Translate** in Lyric options sends the selected original lyrics to Google in a browser URL, with automatic source detection and English output. This happens only when you tap the button; Google's privacy terms and input limits apply. The text may appear in browser history. This external action is separate from MyMemory and Singalong's Wi-Fi-only lookup setting; results are not imported or saved automatically.

Language identification uses Google's ML Kit on this device. Google states that input text and identification results are processed locally, but the SDK may contact Google for updates and send usage/performance metrics. See [ML Kit terms and privacy](https://developers.google.com/ml-kit/terms). Singalong has no separate application analytics service; this does not mean third-party SDKs or providers make no network requests.

One bounded uncaught Java/Kotlin crash log is kept in private no-backup storage. It includes app/Android versions, timestamp, exception types and code locations; exception messages, song details and lyrics are excluded. On the next launch, a prompt offers review and export. Logs are not uploaded automatically. Choosing feedback opens the kmab form; the user attaches the exported log and submits it there. Native crashes, operating-system kills and all possible ANRs are not guaranteed to be captured by this handler.

The static GitHub Pages site has no app analytics or third-party fonts. Its download section checks public GitHub release metadata; GitHub's hosting/API terms apply. The feedback form is a separate service with its own submission and attachment handling.

Use Settings → Library & backup to export or clear supported data. Clearing Android app storage removes local settings and content. Keep backups you want to retain before doing so.
