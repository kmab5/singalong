# Singalong documentation

This index describes the current source. Earlier phase plans and validation reports are historical snapshots; their old version numbers, commands and deferred-check statements are not current release status.

## Current guides

- [Build, features and project status](../README.md)
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
- [Translation alternatives, research only](translation-options.md)

## Accepted changes since the initial design

The current app includes simple text editing plus an opt-in per-line timing editor, filename-aware search, optional estimated sync for untimed lyrics, minimal floating lyrics, bottom icon controls, automatic failure docking/recovery, independent Now-tab typography/motion, and a next-launch crash-report prompt. Original metadata/content is preserved; inferred matches and estimated timing remain distinguishable from verified timed lyrics.

The 1.1.0-rc.1 branch adds responsive floating actions, tap-only edge expansion, library sorting, selected-folder lyric lookup and an independent current timed-line size. Word glow is an estimated visual animation using line timestamps, not word-level timing. See the [implementation plan](superpowers/plans/2026-11-04-library-folder-and-lyrics-controls.md).

Audio recognition, microphone/playback capture and exact word-level timing remain out of scope. Some Android screens may suppress overlays by policy; Singalong must respect that policy. Player compatibility and device acceptance must be based on testing, not inferred from a successful build.

The maintainer authorized merging to main and publishing v1.0.0 on 2026-11-02. The stable version is 1.0.0 with Android version code 9, promoted from candidate code 8. Future releases follow the [release guide](RELEASING.md) and retain the existing signing certificate.

## Historical evidence

- [Initial design](superpowers/specs/2026-11-01-singalong-design.md) and [phase roadmap](superpowers/plans/2026-11-01-development-phases.md)
- [P0](validation/platform-feasibility.md), [P1–P3](validation/p1-p3-checkpoint.md), [P4–P5](validation/p4-p5-checkpoint.md), [P6](validation/p6-checkpoint.md)
- [Visual overhaul](validation/feedback-checkpoint.md), [compact controls](validation/compact-controls-checkpoint.md)
- [Request-cleanup crash evidence](validation/stability-checkpoint.md), [filename search/timing](validation/search-timing-checkpoint.md)
- [Provider investigation](validation/providers.md), [player/device coverage](validation/compatibility.md), [site verification](validation/site-checkpoint.md)

Keep historical evidence intact. Add later corrections/verification explicitly rather than rewriting an earlier untested milestone as if it had passed at the time.
