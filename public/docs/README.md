# Singalong assets

- `singalong-icon.svg` / `singalong-icon.png`: app icon; transparent 1024px PNG.
- `singalong-logo.svg` / `singalong-logo.png`: icon and outlined wordmark for dark backgrounds; transparent PNG at 2×.

Hosted copies live in `docs/assets/`. SVG lettering uses paths and needs no installed font. No personal screenshots or provider lyrics are included.

Use `tools/export_site_assets.py` with fonttools/brotli to regenerate vectors and fonts, then `tools/render_site_assets.cjs` with Playwright for PNGs. See [website maintenance](../../docs/SITE.md). Preserve the notices in `licenses/` and `docs/assets/fonts/`.
