# Website

[The website](https://kmab5.github.io/singalong/) is a static GitHub Pages site served from main `/docs`. Its source is `index.html`, `site.css`, `site.js` and `assets/`. Fonts are self-hosted; there is no bundler or analytics.

The floating reader and lyric-card gallery use original sample text and artwork. They are interactive illustrations, not Android screenshots. Motion respects reduced-motion settings, and core content and the universal download work without JavaScript.

## Downloads

Download links default to the latest stable `singalong.apk`. A bounded GitHub API request can supply the exact version, asset size and release URL, plus architecture APK links when present. Only canonical release URLs from this repository are accepted.

Missing, malformed or unavailable metadata leaves the universal fallback usable. Don’t hardcode a release version into the fallback.

## Local work

```sh
python -m http.server 8765 --directory docs --bind 127.0.0.1
node tools/check_site.cjs
```

The browser check uses an existing Playwright installation and Chrome; set `PLAYWRIGHT_MODULE` and `CHROME_PATH` if needed. It covers responsive layouts, interactions, keyboard access, reduced motion, no-JavaScript behavior and download fallbacks. Screenshots go to ignored `artifacts/`.

Reusable icon and wordmark exports live in `assets/` and [public/docs](../public/docs/README.md). Regenerate vectors and fonts with `tools/export_site_assets.py` and PNGs with `tools/render_site_assets.cjs`. Keep font notices with the assets.
