# Landing-page verification

Static HTML/CSS/JavaScript lives directly in `docs/`. The existing app icon is exported with its original geometry; the wordmark uses outlined Inter glyphs. SVG and transparent PNG copies in `docs/assets/` and `public/docs/` are byte-identical. WOFF2 fonts and their license notices are hosted locally. No personal feedback screenshots or real lyric/provider data are used.

The frontend-design and ui-ux-pro-max guidance was applied to the accepted dark glass/purple direction. The interactive panel uses labelled original sample text and demonstrates floating/minimal/docked modes, edge restore, a next-line action and optional English subtext. Source/setup and feedback actions target real repository/form links; no fabricated APK/store URL is present.

## Executed checks

`tools/check_site.cjs` passed using the existing installed Chrome and Playwright, with the site served under a `/singalong/` project prefix:

- Viewports: **320, 390, 768, 1024 and 1440 pixels**; no horizontal overflow or translated-panel clipping.
- Floating/minimal/docked mode behavior, restore from the edge, next sample line and English subtext.
- Keyboard activation, visible skip-link focus, FAQ expansion, local anchor destinations and all four downloadable image URLs.
- Reduced-motion styling and readable no-JavaScript content.
- No browser JavaScript errors or failed local resource requests.
- Desktop/mobile screenshots inspected; a narrow-screen translated-panel clipping issue found in the first run was corrected before the final pass.

`tools/export_site_assets.py` and `tools/render_site_assets.cjs` also ran successfully. Check output and screenshots are local/ignored under `artifacts/site-final-check.log`, `artifacts/site-desktop.png` and `artifacts/site-mobile.png`.

GitHub's [Pages deployment for a20e4db](https://github.com/kmab5/singalong/actions/runs/41493081097) completed successfully on `feat/p0-foundation`. The live [landing page](https://kmab5.github.io/singalong/) returned HTTP 200 with the expected title. Installed Chrome verified the live minimal-mode interaction and all four PNG/SVG downloads, with no browser/resource errors. Local result: `artifacts/site-live-check.log`. The existing Pages configuration published the `/docs` site without an administration change. [Hosting instructions](../SITE.md) include the source settings for future changes.
