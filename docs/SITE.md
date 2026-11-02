# Landing page

The public site is `docs/index.html`, with `site.css`, `site.js` and `assets/`. It is a static site; there is no build command, analytics or third-party font request. `.nojekyll` preserves the files as written. Existing project documents remain in this directory.

## GitHub Pages

In the repository's **Settings → Pages**, select **Deploy from a branch**, **main**, and **/docs**. Save. The project URL is `https://kmab5.github.io/singalong/`. Configuration/deployment status must be checked in GitHub; creating these files does not establish that the site is live.

The existing configuration deployed this page successfully on November 2. Live URL: [kmab5.github.io/singalong](https://kmab5.github.io/singalong/). See the [site checkpoint](validation/site-checkpoint.md) for the deployment and live browser verification.

See [GitHub's publishing-source instructions](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site).

Local preview: `python -m http.server 8765 --directory docs --bind 127.0.0.1`, then open `http://127.0.0.1:8765/`. All local asset paths are relative so project-path hosting works too.

## Content and assets

The release-focused layout puts a direct Android download in the header, hero and setup section. The hero includes the released version, approximate APK size, release notes/checksums and source. Setup explains APK installation and notification/overlay access; feature copy covers the library, translations, Now customization and opt-in timing editing.

The interactive panel uses clearly labelled original sample text; it does not access music, fetch lyrics or play audio. Floating/expanded/minimal/docked buttons, bottom dock/expand controls, edge restore, next line and English subtext work locally. Docking from the panel moves keyboard focus to the edge handle. Line changes use a short cancellable transition; motion honors `prefers-reduced-motion`. Page content and direct downloads remain usable without JavaScript.

All three download links default to the latest published `singalong.apk` through GitHub's stable release URL. A bounded public GitHub API request optionally updates every download, release link, version label and size together. Only stable releases with an APK hosted in this repository are accepted. An empty, malformed, rate-limited or failed API response, or disabled JavaScript, leaves the direct download usable. The first stable release must remain published for that fallback URL to resolve. Feedback opens the existing kmab form with product `singalong` and source `web`; reports are submitted by the visitor. See [release instructions](RELEASING.md).

Hosted SVG/PNG icons and outlined wordmarks are in `assets/`, with reusable copies in `public/docs/` at the repository root. The SVG icon preserves `app/src/main/res/drawable/ic_singalong.xml` geometry. Font license notices accompany the self-hosted WOFF2 files. See `tools/export_site_assets.py` for regeneration.

After exporting the SVGs, `node tools/render_site_assets.cjs` rasterizes transparent PNGs using Playwright. `node tools/check_site.cjs` serves the site under a `/singalong/` project path, checks responsive layouts/interactions, and saves review screenshots in ignored `artifacts/`. These authoring/check tools need Playwright installed in your tool environment; they are not site runtime dependencies. Set `PLAYWRIGHT_MODULE` to its module directory if it is outside this repo and optionally `CHROME_PATH` to an existing Chrome executable. No browser or Android SDK download is performed by the scripts.
