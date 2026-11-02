# Landing page

The public site is `docs/index.html`, with `site.css`, `site.js` and `assets/`. It is a static site; there is no build command, analytics or third-party font request. `.nojekyll` preserves the files as written. Existing project documents remain in this directory.

## GitHub Pages

In the repository's **Settings → Pages**, select **Deploy from a branch**, the branch containing the site (currently `feat/p0-foundation`), and **/docs**. Save. If you later merge to another branch, switch the Pages source accordingly. The expected project URL is `https://kmab5.github.io/singalong/`. Configuration/deployment status must be checked in GitHub; creating these files does not establish that the site is live.

See [GitHub's publishing-source instructions](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site).

Local preview: `python -m http.server 8765 --directory docs --bind 127.0.0.1`, then open `http://127.0.0.1:8765/`. All local asset paths are relative so project-path hosting works too.

## Content and assets

The interactive panel uses clearly labelled original sample text; it does not access music, fetch lyrics or play audio. Floating/minimal/docked buttons, edge restore, next line and English subtext work locally. Page content remains readable without JavaScript. Motion honors `prefers-reduced-motion`.

The source/setup link targets the repository build guide; there is no invented APK/store link. Feedback opens the existing kmab form with product `singalong` and source `web`; reports are submitted by the visitor.

Hosted SVG/PNG icons and outlined wordmarks are in `assets/`, with reusable copies in `public/docs/` at the repository root. The SVG icon preserves `app/src/main/res/drawable/ic_singalong.xml` geometry. Font license notices accompany the self-hosted WOFF2 files. See `tools/export_site_assets.py` for regeneration.

After exporting the SVGs, `node tools/render_site_assets.cjs` rasterizes transparent PNGs using Playwright. `node tools/check_site.cjs` serves the site under a `/singalong/` project path, checks responsive layouts/interactions, and saves review screenshots in ignored `artifacts/`. These authoring/check tools need Playwright installed in your tool environment; they are not site runtime dependencies. Set `PLAYWRIGHT_MODULE` to its module directory if it is outside this repo and optionally `CHROME_PATH` to an existing Chrome executable. No browser or Android SDK download is performed by the scripts.
