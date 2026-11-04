# Singalong 1.1.0-rc.1 — testing candidate

Version 1.1.0-rc.1 (10). Prepared for phone testing; this update has not been merged or published. The latest public release remains v1.0.0.

- Floating controls wrap into rows to fit the panel. Drag the minimized handle to move it; tap to reopen.
- Library sorting by title, artist, album, recent playback and play count.
- A Library folder action finds and saves lyrics for audio in a chosen folder and its subfolders, skipping saved matches. Progress and Cancel are available; successful saves survive interruption. Keep the app open while scanning.
- A separate current timed-line size setting in Settings → Now.
- Optional Word glow: smooth, estimated word emphasis between existing line timestamps, with pause/seek handling. It is not exact karaoke timing.
- Full-width selection panels in Settings.

Requires Android 12 or newer. Install the candidate over an existing release-signed installation to retain data. The signing key is unchanged; the database migration retains existing lyrics and selections. Debug APKs use a different key.

Folder lookups send song search details to the existing lyric sources, never audio. Metadata accuracy and source coverage determine matches. Inferred matches remain labelled as uncertain. Scans respect Wi-Fi-only settings and source cooldowns. Reselecting a folder after app termination skips already saved matches; unfinished work is not scheduled in the background.

See [the checkpoint](docs/validation/library-folder-checkpoint.md) for automated evidence and pending phone checks. No new universal player/device compatibility claim is made. Same-key packaging includes APK/checksum/build metadata and the matching R8 mapping; no new release tag is created by this work.
