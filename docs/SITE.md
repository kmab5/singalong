# Landing page

The public site is [kmab5.github.io/singalong](https://kmab5.github.io/singalong/). Its source is `index.html`, `site.css`, `site.js` and `assets/` in this directory. It is static: no bundler, analytics or third-party font requests. GitHub Pages deploys main `/docs`; `.nojekyll` preserves the files as written.

## Design and content

The November 7 overhaul replaces the previous page structure. Oversized Inter headings and Lora accents lead into an illustrated floating reader, a contrasting light lyric-card gallery, a saved-library illustration, installation steps and accessible FAQs. PT Mono labels support the music-poster direction. All fonts are self-hosted and have adjacent license notices.

The illustrations use original sample words and artwork. They are labelled as interactive illustrations, not Android screenshots. The floating demo supports Floating, Lyrics only and Expanded modes, docking/restoring with keyboard focus transfer, advancing a line and English subtext. Gallery controls switch between three palette directions and square/portrait composition. Album-art color extraction happens in the Android app, not this demo.

Motion is finite and respects reduced motion. The page, sample cards and downloads remain usable without JavaScript. Turkish sample lines use Turkish language metadata, with English subtext marked separately.

## Downloads

All three download links default to GitHub's latest stable `singalong.apk` URL. A five-second bounded API request optionally adds the exact stable version, asset size and versioned download/release links. Only canonical APK URLs from this repository are accepted. Empty, malformed, rate-limited or unavailable API responses leave the default download usable. The fallback label is “Latest release”, never a hardcoded older version.

The release workflow publishes new main-branch app versions; website-only changes keep existing assets. See [release instructions](RELEASING.md). The feedback link opens the existing kmab form with product `singalong` and source `web`; the visitor submits any report.

## Assets and maintenance

SVG/PNG icons and outlined wordmarks are available from the footer and `assets/`, with reusable copies in `../public/docs/`. `tools/export_site_assets.py` preserves the Android icon geometry; `tools/render_site_assets.cjs` rasterizes exports. Keep app marks and font notices alongside their assets.

Preview with `python -m http.server 8765 --directory docs --bind 127.0.0.1`. All asset paths are relative and also work under `/singalong/`.

Run `node tools/check_site.cjs` with an existing Playwright installation (`PLAYWRIGHT_MODULE`) and Chrome executable (`CHROME_PATH`). The check serves the project path, verifies layouts at 320/390/768/1024/1440px, all demo modes with/without subtext, gallery formats, keyboard/reduced-motion/no-JavaScript behavior, downloads and malformed/network API fallbacks. Screenshots go to ignored `artifacts/`. No browser or Android SDK installation is performed.

See the [overhaul/publication checkpoint](validation/site-release-checkpoint.md) for current evidence. Earlier [site](validation/site-checkpoint.md), [1.1](validation/1.1-site-checkpoint.md) and [1.2 integration](validation/1.2-main-checkpoint.md) checkpoints remain historical records.
