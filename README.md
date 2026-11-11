# singalong

I’m kmab, and Singalong is my Android lyrics companion. It follows music from the players you choose and keeps the lyrics in a floating window.

[Website](https://kmab5.github.io/singalong/) · [Download](https://github.com/kmab5/singalong/releases/latest) · [Feedback](https://kmab5.github.io/kmab-feedback/?product=singalong)

## Install

Requires **Android 12 or newer**. Download `singalong.apk` from the latest release if you’re unsure which APK to choose. Smaller downloads are available for arm64-v8a, armeabi-v7a, x86 and x86_64 when included in that release.

Open Singalong, allow music access and select your players. Grant overlay permission when you want floating lyrics. Install updates over the existing release to keep your library.

For a shortcut, expand Quick Settings, open **Edit**, and add **Singalong** beside your other tiles. Tap it to start or stop tracking. Adding the tile or opening the shade doesn’t start tracking.

## Features

- Floating lyrics in compact, expanded or minimal mode, with alignment and separate current/other-line sizes.
- Synced lyrics, word highlighting where available, timing adjustments and manual reading.
- Offline library, favorites, history, folder lookup and TXT/LRC import.
- Personal edits and selective ZIP backups with adjustable or unlimited size; older JSON backups can still be restored.
- Custom lyric cards with local artwork, palettes, fonts and PNG sharing.
- Optional English translation and a Google Translate shortcut.

Singalong reads song metadata and playback position; it doesn’t record audio. Online lookup sends song details to lyric providers, and optional translation sends lyric text to a translation service. See [privacy](PRIVACY.md).

Player support depends on the information each app exposes. Estimated timing and uncertain matches are labelled; translation and lyrics may be unavailable.

## Development

See [building](docs/BUILDING.md), [testing](docs/TESTING.md) and [contributing](CONTRIBUTING.md). The [documentation index](docs/README.md) covers design, providers, releases and the website.

Licensed under [GPL v3](LICENSE) with an [ML Kit linking exception](LICENSE-EXCEPTION). Third-party software, fonts and lyric content retain their own terms; see [notices](NOTICE.md).
