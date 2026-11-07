# Singalong documentation

This index describes the current source. Earlier phase plans and validation reports are historical snapshots; their old version numbers, commands and deferred-check statements are not current release status.

## Current guides

- [Build, features and project status](../README.md)
- [1.2.1 Quick Settings tracking tile checkpoint](validation/tracking-tile-checkpoint.md)
- [1.2 integration and album-art palette checkpoint](validation/1.2-main-checkpoint.md)
- [Website overhaul and 1.2 publication checkpoint](validation/site-release-checkpoint.md)
- [Release/version/signing workflow](RELEASING.md)
- [Signing continuity and local builds](validation/release-signing.md)
- [Privacy and external services](../PRIVACY.md)
- [License](../LICENSE), [ML Kit linking permission](../LICENSE-EXCEPTION), [third-party notices](../NOTICE.md)
- [Contributing](../CONTRIBUTING.md), [conduct](../CODE_OF_CONDUCT.md), [security reporting](../SECURITY.md)
- [Website hosting and checks](SITE.md)
- [Release-candidate verification checkpoint](validation/premerge-checkpoint.md)
- [Folder scan and lyric-control candidate checkpoint](validation/library-folder-checkpoint.md)
- [Glow and timed-selection follow-up checkpoint](validation/glow-selection-checkpoint.md)
- [Glow hidden-shadow correction checkpoint](validation/glow-shadow-checkpoint.md)
- [Provider word-sync checkpoint](validation/word-sync-checkpoint.md)
- [Word tracking switch and Google Translate checkpoint](validation/word-tracking-controls-checkpoint.md)
- [1.1 main integration and Translate fallback checkpoint](validation/1.1-main-checkpoint.md)
- [1.1 website preparation checkpoint](validation/1.1-site-checkpoint.md)
- [Lyric card feature checkpoint](validation/lyric-card-checkpoint.md)
- [Translation alternatives, research only](translation-options.md)

## Accepted changes since the initial design

Version 1.2.1 adds a manually installed Singalong Quick Settings tile. Tapping it explicitly starts/stops tracking through the existing session controls; adding/viewing the tile does not resume tracking. The existing notification is unchanged. See the [tile plan](superpowers/plans/2026-11-07-tracking-tile.md).

The 1.2.0 source adds draggable lyric actions and a branded card editor with eight layouts, a pinned preview, seven bundled fonts, ten curated palettes plus a locally generated album-art palette, optional metadata, selected-line emphasis, local artwork and PNG share/save. Corner logo overlays wrap nearby text; photo backgrounds remain independent of cover visibility. The draft snapshots the selected song independently of playback. Minimal floating lyrics retain their long-press movement gesture; standard floating lyrics route card actions into the activity. See the [card plan](superpowers/plans/2026-11-06-lyric-share-cards.md) and [integration plan](superpowers/plans/2026-11-06-album-palette-integration.md).

The current app includes simple text editing plus an opt-in per-line timing editor, filename-aware search, optional estimated sync for untimed lyrics, minimal floating lyrics, bottom icon controls, automatic failure docking/recovery, independent Now-tab typography/motion, and a next-launch crash-report prompt. Original metadata/content is preserved; inferred matches and estimated timing remain distinguishable from verified timed lyrics.

The 1.1.0 source adds responsive floating actions, tap-only edge expansion, library sorting, selected-folder lyric lookup and an independent current timed-line size. Glow follows supplied LRCLIB word timing, falling back to estimated progression for line-only lyrics. The user confirmed rc.4 word syncing works. Word tracking can be disabled, keeping line sync. Google Translate opens through a browser-first handoff that copies full original lyrics for manual paste if needed. See the [initial implementation plan](superpowers/plans/2026-11-04-library-folder-and-lyrics-controls.md) and [integration plan](superpowers/plans/2026-11-04-translate-fallback-and-main.md).

Audio recognition and microphone/playback capture remain out of scope. Supplied word timestamps are supported; generating word timestamps from audio is not. Some Android screens may suppress overlays by policy; Singalong must respect that policy. Player compatibility and device acceptance must be based on testing, not inferred from a successful build.

The maintainer authorized the 1.1 merge on 2026-11-04, the lyric-card/1.2 merge on 2026-11-06, and automatic versioned main-branch releases on 2026-11-07. Main contains **1.2.1 (21)**. Publication follows the [release guide](RELEASING.md) and retains the existing signing certificate. Website-only changes leave published APKs intact.

## Historical evidence

- [Initial design](superpowers/specs/2026-11-01-singalong-design.md) and [phase roadmap](superpowers/plans/2026-11-01-development-phases.md)
- [P0](validation/platform-feasibility.md), [P1–P3](validation/p1-p3-checkpoint.md), [P4–P5](validation/p4-p5-checkpoint.md), [P6](validation/p6-checkpoint.md)
- [Visual overhaul](validation/feedback-checkpoint.md), [compact controls](validation/compact-controls-checkpoint.md)
- [Request-cleanup crash evidence](validation/stability-checkpoint.md), [filename search/timing](validation/search-timing-checkpoint.md)
- [Provider investigation](validation/providers.md), [player/device coverage](validation/compatibility.md), [site verification](validation/site-checkpoint.md)

Keep historical evidence intact. Add later corrections/verification explicitly rather than rewriting an earlier untested milestone as if it had passed at the time.

The [1.2 checkpoint](validation/1.2-main-checkpoint.md) records current integration; the [roomy card checkpoint](validation/roomy-card-checkpoint.md) retains earlier editor evidence; the [rc.2 checkpoint](validation/card-refresh-checkpoint.md) retains folder-scan regression evidence.
