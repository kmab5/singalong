# Singalong 1.3.0-rc.2

Testing candidate on the feature branch; not published or merged.

Floating lyrics now have alignment, separate sizes for the current and other lines, and optional word highlights using supplied timestamps. Floating word highlights have no glow or drift.

Backups are now ZIP files with separate sections. Choose full lyrics or source references, song metadata, play history and all saved settings. Older JSON backups can still be restored. Audio files are not copied, and links-only backups do not preserve lyric text.

The backup-size limit is now adjustable, with an Unlimited option and no separate per-file cap. Settings exports include every persisted preference, including selected players and tracking/translation choices. Restoring a file leaves current tracking and translation-consent choices unchanged.

Requires Android 12 or newer. The signed candidate uses the existing release key and can update the installed release.
