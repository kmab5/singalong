# Release-focused website refresh implementation plan

**Goal:** Make the published Android app easy to understand and download while retaining the approved kmab visual direction.

**Approval:** The user selected the release-focused refresh: preserve the current style, improve layout/demo, and prioritize downloads. Implement inline with the executing-plans workflow; no further design approval is needed for this scope.

**Architecture:** Keep static HTML, CSS and JavaScript in `docs/`, relative assets, self-hosted fonts and GitHub Pages on `main/docs`. HTML supplies functional release downloads without JavaScript; the optional GitHub request enhances all release links and details together. Demo state remains local, with original sample text and no audio.

**Design:** Retain ink #0E0E11, panel #17171B, paper #EDEBE4, muted #AAA6B5, purple #B24BFF and lilac #D49AFF. Keep Inter display/body and PT Mono utility text. The signature remains the floating lyric window, with a clearer current-line cue, usable bottom controls and compact/expanded/minimal/docked views. Prioritize a visible Android download and release details in the hero, then player context, features, timing, setup and concise privacy/help. Tighten mobile spacing and improve small labels without inventing compatibility claims.

## Constraints

- Android 12+, published v1.0.0, approximately 9.6 MB; no app code, release tag, signing or APK changes.
- Preserve the direct latest-APK fallback during API failure/rate limiting and without JavaScript.
- No analytics, third-party fonts, new dependencies or private feedback media.
- Label demo content as original sample text; no microphone/audio-recognition claims.
- Keyboard controls, 44px touch targets, no horizontal overflow and reduced motion.

## Tasks

- [x] Update `docs/index.html`: download in header/hero/setup, release details, current features, installation guidance, support links; keep semantic headings and existing asset exports.
- [x] Update `docs/site.css` and `docs/site.js`: responsive glass styling, four demo modes and bottom controls, gentle line transitions, shared release metadata across download links. Scope animations and cancel superseded effects; reduced motion bypasses animation.
- [x] Extend `tools/check_site.cjs`: verify all downloads under successful/failed/malformed release responses, new demo controls and no-JS downloads; retain viewport, clipping, keyboard and asset checks. Run with the installed Chrome and Playwright. Inspect generated desktop/mobile images and correct visible defects.
- [x] Update `docs/SITE.md`, check Markdown links and diff. Do not rebuild Android for website-only changes.

**Publication procedure:** Commit/push the website changes, then verify Pages deployment and live downloads. Report deployment separately from local checks.

## Verification commands

```powershell
$env:PLAYWRIGHT_MODULE='D:/Code/cs-engine-web/node_modules/playwright'
$env:CHROME_PATH='C:/Program Files/Google/Chrome/Application/chrome.exe'
node tools/check_site.cjs
python tools/check_docs.py
git diff --check
```

The existing screenshot artifacts are ignored. Publication is complete only after the Pages run succeeds and the live page exposes the refreshed controls/downloads.

## Local verification

The browser check passed at 320, 390, 768, 1024 and 1440 pixels, including all four modes, bottom controls, focus after docking, translated panel bounds, shared release metadata, malformed/network fallback, reduced motion, keyboard access, no-JavaScript downloads and four asset exports. Desktop/mobile screenshots were visually reviewed. The documentation checker passed for 36 Markdown files. These checks do not claim deployment; the subsequent Pages run supplies that evidence.
