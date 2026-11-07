# Singalong 1.2.1

Turn song tracking on or off from a new **Singalong** Quick Settings tile, beside Wi-Fi and Bluetooth. Open the notification shade's Edit controls and add the tile once. The existing tracking notification is unchanged.

- The tile reflects the live session, including stops made from the app or notification.
- Opening the shade does not start tracking. Tap explicitly to start, even after the app process has stopped.
- Missing music access or player selection opens Singalong for setup. Locked-device taps require unlocking, and startup/shutdown states prevent overlapping commands.

Requires Android 12 or newer. Signed with the existing release key for updates over 1.2.0. Assets include the APK, matching R8 crash mapping, build metadata and SHA-256 checksums.

See the [tracking tile checkpoint](https://github.com/kmab5/singalong/blob/v1.2.1/docs/validation/tracking-tile-checkpoint.md) for verification and the physical-device checklist. Device checks remain with the maintainer; no emulator was installed or run.
