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

Publication and deployment outcomes are recorded after the tagged push. Local checks alone do not establish that the release or website is live.
