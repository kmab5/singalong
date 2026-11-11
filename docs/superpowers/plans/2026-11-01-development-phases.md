# Development phases

This roadmap retains the project’s dependency order. The [design](../specs/2026-11-01-singalong-design.md) describes current behavior; [testing](../../TESTING.md) separates automated evidence from device acceptance.

| Phase | Scope | Acceptance focus |
| --- | --- | --- |
| P0 | Toolchain, permissions, theme, provider and overlay foundation | Explicit start/stop, process gate, readable overlay and honest provider failures |
| P1 | Selected-player tracking and local collection | Ten-second qualification, stable recording identity, idempotent history |
| P2 | Lookup and synchronized reader | Correct matching/timing, offline cache, stale-response protection |
| P3 | Floating lyrics | Seek/pause, docking/recovery, persistent manual close and permission cleanup |
| P4 | Library, import, editing and backup | Original preservation, selected personal versions, atomic/idempotent restore |
| P5 | Optional translation and visual refinement | Original-first reading, valid translation association, accessible rendered UI |
| P6 | Compatibility and release | Automated checks, signing continuity and documented device results |

P0–P5 source is implemented. P6 automated verification has run, but complete physical acceptance remains open. No milestone is considered physically verified solely because source or tests exist.

Subsequent work adds folder lookup, supplied word timing, lyric cards, the website and Quick Settings tracking. These are part of the current design; generated audio timing, audio capture, paid services and new permission scopes remain outside it.

The current delivery work is [floating controls and selective backups](2026-11-11-floating-backups.md), approved for the 1.3.0 stable release after maintainer testing. Device testing remains with the maintainer; no emulator or SDK-package installation is part of this work.
