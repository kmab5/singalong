# Website overhaul and 1.2 publication

Date: 2026-11-07. Scope: major GitHub Pages redesign and publication of the current main-branch Android source. The maintainer explicitly authorized staying on main, tagging and publishing v1.2.0.

## Cause and implementation

Earlier Android and Pages CI runs succeeded, but only v1.0.0 had a tag. The former release workflow listened only for tag pushes, so merged 1.1/1.2 source never became a public APK.

The workflow now plans publication on main, tags and manual runs. It skips complete unchanged releases, rejects conflicting tags/incomplete assets, and requires a new version for changed app source. Queued runs are preserved. Builds retain the existing key and pinned certificate checks.

The site has a new hero, floating-reader demo, contrasting card gallery, library illustration, setup section and FAQ. Original illustrative text/artwork is identified. Self-hosted Inter/Lora/PT Mono, responsive spacing, finite motion and reduced-motion support replace the old presentation. Downloads bind only to canonical stable release assets, with a no-JavaScript/API-failure fallback.

## Local verification

- Ten release-helper unit tests pass, covering versions, publication decisions, conflicting tags, duplicate runs, incomplete releases and HTTP failures.
- Browser checks pass at 320, 390, 768, 1024 and 1440px: every floating mode with/without English subtext, palette/format controls, docking focus, keyboard operation, reduced motion, no-JavaScript content, API metadata/fallbacks, local assets and anchors. No page errors or horizontal overflow.
- Reviewed full desktop/mobile screenshots and gallery captures under ignored `artifacts/`. Fixed translated-panel overlap and language metadata found during review.
- Native Android source is unchanged from the [1.2 integration checkpoint](1.2-main-checkpoint.md). Hosted release CI performs fresh native tests, lint, instrumentation compilation, release assembly and signing validation. No physical-device or emulator execution is claimed here.

## Hosted verification

- Commit `b461bf6d020324761a6a98d0bd830f4f261bc93c` was committed directly on main and pushed atomically with annotated tag `v1.2.0`.
- [Release run 42296314187](https://github.com/kmab5/singalong/actions/runs/42296314187) passed debug/release tests and lint, instrumentation compilation, release assembly, certificate/version/mapping/alignment checks and publication.
- [Android CI 42296314462](https://github.com/kmab5/singalong/actions/runs/42296314462) passed both ordinary checks and the signed-candidate job.
- [Queued main-trigger run 42296314786](https://github.com/kmab5/singalong/actions/runs/42296314786) passed its plan and skipped publication because the complete release already existed. No tag or assets were replaced.
- [Pages deployment 42296313681](https://github.com/kmab5/singalong/actions/runs/42296313681) passed. A live Chrome check at 1440px and 390px verified the redesigned page, docking/restoring, palette/format controls, no horizontal overflow or JavaScript errors, and all three download links resolving to the exact v1.2.0 APK. A first network-idle navigation timed out; the retry waiting for DOM readiness passed.
- [v1.2.0](https://github.com/kmab5/singalong/releases/tag/v1.2.0) is the latest stable release. Assets include the 10,494,594-byte APK, mapping, build metadata and checksums. Its build metadata identifies the tagged commit and existing pinned certificate. R8 map ID: `f1145f226a31aa3c8747c4acc8a0a52ff10ff02a5eda1bedd5788d48b4b295e1`.
- Independently downloaded the complete published APK and build metadata, verified their SHA-256 values against `SHA256SUMS` and GitHub asset digests, then checked the APK with existing SDK tools: version 1.2.0 (20), one signer matching the pinned certificate, and 16 KB alignment all passed. The published mapping header matches the APK's embedded R8 ID; its full-file checksum matches GitHub's asset digest. The full 47 MB mapping was not downloaded locally because transfer stalled; release CI verified the complete mapping before upload. The APK download succeeded using bounded parallel range requests, with the assembled file verified before use.
- Published APK SHA-256: `0de8c6fe3da162812d7deb21bd4810b83be8dff5356224b7f4c900fbbb1767da`. Local evidence and live screenshots are in ignored `artifacts/current-feedback/`; the verified APK is in `artifacts/published-v1.2.0/`. No device testing was added by this release operation.
