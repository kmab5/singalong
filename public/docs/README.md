# singalong assets

- `singalong-icon.svg` / `singalong-icon.png`: existing Android app icon, 1024px PNG with transparent rounded corners.
- `singalong-logo.svg` / `singalong-logo.png`: icon plus an outlined Inter wordmark, light lettering for dark backgrounds, transparent PNG at 2x.

Identical hosted copies live in `docs/assets/` and are linked from the landing page. No personal screenshots or provider lyrics are included. SVG wordmark lettering uses paths, so the download needs no installed font.

To regenerate vectors and web fonts, install `fonttools` and `brotli` into a local authoring environment and run `python tools/export_site_assets.py`. Then run `node tools/render_site_assets.cjs` with Playwright available to export transparent PNGs into both directories. See `docs/SITE.md` for tool environment options. Font licenses are in `licenses/` and `docs/assets/fonts/`.
