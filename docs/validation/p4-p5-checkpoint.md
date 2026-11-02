# P4–P5 source checkpoint

> Historical checkpoint: results and APK versions below describe that revision. See the [current documentation index](../README.md) and [pre-merge checkpoint](premerge-checkpoint.md) for later changes and verification.

Source implemented; compilation, lint, test execution, migrations, screenshots and physical-device acceptance remain unverified at the user's instruction. No SDK package or emulator was installed. M4/M5 are not claimed passed.

## P4

- Library title/artist search, favorites, offline labels and manual metadata search with explicit version selection.
- TXT/LRC system-document import, strict UTF-8/BOM handling, bounded 2 MiB reads and preview before saving. Original imports are retained alongside personal edits.
- Simple text editor: timed copies enforce the original line count and retain line IDs/timestamps; untimed copies allow structural changes. Saving creates a new version and selects it. Existing originals remain intact. Back navigation asks before discarding; large drafts use an activity ViewModel instead of the saved-instance Bundle. Drafts survive rotation, but are not durable after process termination; save before leaving important work unfinished.
- Favorites and personal/imported versions survive default download cleanup. Clearing history leaves lyrics intact. Selected-version foreign keys cascade and remaining versions are selected explicitly. Occurrence deduplication records remain after clearing visible history to avoid recounting an ongoing play.
- Schema-1 JSON backup through document pickers, 50 MiB bounded reads, preview, reference validation before writes and transactional restore. Room database migration 1→2 adds favorites and translation cache.
- Restoring duplicates is idempotent; conflicting versions receive deterministic new IDs. Existing selections, favorites and adjustments win. History uses maximum count and latest timestamp, which can undercount disjoint histories. Display/network preferences are exported for reference; restore deliberately retains current settings and never activates tracking. Permissions, credentials and active sessions are excluded.

## P5

Original lyrics remain primary; aligned English subtext has a separate size control. The cache is bound to version ID/content hash with complete line-ID validation, attribution and source language. Disabled/unopened versions do not invoke translation. Valid saved translations can be read offline and travel with backups. Edited versions cannot inherit stale translations. The production adapter returns unavailable, not synthetic success.

Provider research revisited official [MyMemory limits](https://mymemory.translated.net/doc/usagelimits.php), [terms](https://mymemory.translated.net/terms-and-conditions), and [LibreTranslate hosting documentation](https://docs.libretranslate.com/). MyMemory permits limited anonymous API use but stores submitted segments long term and shares processing with partners; suitable lyric-result retention and automatic language handling were not established in this implementation. LibreTranslate managed hosting requires a paid key; self-hosting is outside scope. Therefore no online provider is connected. This is the unavailable outcome explicitly allowed by Task 8, not a claim that free translation is universally impossible. No lyric text was submitted for translation.

The existing KMAB palette, typography, lyric cue, native controls and original-language priority continue across library/editor/settings. Imported/saved English subtext renders in both reader and overlay. Long-text, mixed-script, large-font, contrast, TalkBack and rotation checks still need rendered/device evidence.

## Evidence and next checks

`git diff --check` and source review only; no build/test success is claimed. Added unrun editor/import, backup codec and translation gate/cache tests, plus Room tests for repeated restores, invalid-schema rejection, cleanup protection and rollback using an injected SQLite trigger failure.

When verification is authorized: compile with the existing SDK, generate/review Room schemas, run all tests, verify upgrade migration, import/edit/save/reopen, rotate during editing, restore the same backup twice, restore conflicting personal versions, clear downloads with favorites and live playback, then inspect bilingual fixtures with TalkBack and large fonts. Keep valuable personal content backed up separately until recovery tests pass. P6 compatibility/release validation remains next.
