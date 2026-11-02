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

GitHub repository metadata already reports Pages enabled, and the existing Pages workflow runs on `feat/p0-foundation`. The page is ready for that branch's `/docs` publishing source. Live availability is verified separately after pushing; local browser checks alone are not a deployment claim. [Hosting instructions](../SITE.md) include the source settings.
