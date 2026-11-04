# Singalong 1.1.0-rc.2 — testing candidate

Version 1.1.0-rc.2 (11). Prepared for phone testing; this update has not been merged or published. The latest public release remains v1.0.0.

- Glow adds brighter highlights, slow word lift/drift and slight enlargement. Fixed wrapping and eased drawing/scrolling reduce sudden changes between current lines. Glow word progression is estimated from line timestamps.
- The Now animation settings are named Gentle and Glow. Untimed text is unaffected by the current timed-line size setting when estimated sync is off.
- New automatic lyric choices prefer timed matches, then the closest known song duration. The version list uses the same preference. Explicit selections and personal versions remain saved.
- Real timestamps start following automatically even for an uncertain match; the uncertainty label stays visible. Refreshing the same version preserves a manual following pause. Untimed estimated sync remains opt-in.
- Long-press dragging of minimal lyrics is unchanged, as requested. Folder scanning, sorting and responsive floating controls from rc.1 remain available.
- Translation alternatives are documented for discussion; the provider is unchanged.

Requires Android 12 or newer. Install over an existing release-signed installation to retain data. The signing key is unchanged. Debug APKs use a different key.

See [the checkpoint](docs/validation/glow-selection-checkpoint.md) for automated evidence and phone checks, and [translation research](docs/translation-options.md) for options. Same-key packaging includes APK/checksum/build metadata and the matching R8 mapping. No new release tag is created by this work.
