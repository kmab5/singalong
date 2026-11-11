const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs = require("node:fs/promises");
const path = require("node:path");
const http = require("node:http");
const assert = require("node:assert/strict");
const root = path.resolve(__dirname, "..");
const docs = path.join(root, "docs");
const fallback =
  "https://github.com/kmab5/singalong/releases/latest/download/singalong.apk";
const api = "https://api.github.com/repos/kmab5/singalong/releases?*";

(async () => {
  await fs.mkdir(path.join(root, "artifacts"), { recursive: true });
  const browser = await chromium.launch({
    executablePath: process.env.CHROME_PATH || undefined,
    headless: true,
  });
  const mime = {
    ".html": "text/html",
    ".css": "text/css",
    ".js": "text/javascript",
    ".json": "application/json",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".woff2": "font/woff2",
    ".ttf": "font/ttf",
    ".txt": "text/plain",
  };
  const server = http.createServer(async (req, res) => {
    try {
      const urlPath = decodeURIComponent(
        new URL(req.url, "http://localhost").pathname,
      );
      if (!urlPath.startsWith("/singalong/")) {
        res.writeHead(404).end();
        return;
      }
      const file = path.resolve(
        docs,
        "." +
          urlPath.slice("/singalong".length) +
          (urlPath.endsWith("/") ? "index.html" : ""),
      );
      if (!file.startsWith(docs + path.sep)) {
        res.writeHead(403).end();
        return;
      }
      res
        .writeHead(200, {
          "Content-Type":
            mime[path.extname(file)] || "application/octet-stream",
        })
        .end(await fs.readFile(file));
    } catch {
      res.writeHead(404).end();
    }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const base = `http://127.0.0.1:${server.address().port}/singalong/`;
  try {
    const errors = [];
    const page = await browser.newPage({
      viewport: { width: 1440, height: 1100 },
    });
    page.on("pageerror", (error) => errors.push(String(error)));
    page.on("response", (response) => {
      if (response.url().startsWith(base) && response.status() >= 400)
        errors.push(`${response.status()} ${response.url()}`);
    });
    await page.route(api, (route) =>
      route.fulfill({ contentType: "application/json", body: "[]" }),
    );
    await page.goto(base, { waitUntil: "networkidle" });
    await page.evaluate(() => document.fonts.ready);
    assert.equal(
      await page.locator("#apk-download").getAttribute("href"),
      fallback,
    );
    assert.equal(await page.locator("#apk-options").isVisible(), false);
    const journal = JSON.parse(await fs.readFile(path.join(docs, "releases.json"), "utf8"));
    const notes = journal.releases.find((item) => item.version === "1.3.0");
    assert.ok(notes, "Versioned release notes are present");
    assert.deepEqual(
      await page.locator(".release-group h3").allTextContents(),
      ["+New", "↗Improved", "✓Fixed"],
    );
    assert.deepEqual(
      await page.locator(".release-group h4").allTextContents(),
      notes.sections.flatMap((group) => group.items.map((item) => item.title)),
    );
    assert.equal(await page.locator("#notes-summary").innerText(), notes.summary);
    assert.equal(await page.locator("#notes-fallback").isVisible(), false);
    assert.equal(
      await page.getByRole("link", { name: "Samuel - kmab", exact: true }).getAttribute("href"),
      "https://kmab5.github.io",
    );
    await page
      .getByRole("button", { name: "Tide palette", exact: true })
      .click();
    await page.getByRole("button", { name: "Portrait", exact: true }).click();
    assert.equal(
      await page.locator("#card-gallery").getAttribute("data-palette"),
      "tide",
    );
    assert.equal(
      await page.locator("#card-gallery").getAttribute("data-format"),
      "portrait",
    );
    assert.equal(
      await page.locator("#card-description").innerText(),
      "Tide · Portrait",
    );
    await page
      .getByRole("button", { name: "Plum palette", exact: true })
      .click();
    await page.getByRole("button", { name: "Square", exact: true }).click();
    await page
      .getByRole("button", { name: "Dock sample lyrics", exact: true })
      .click();
    assert.equal(
      await page.locator(".lyric-window").evaluate((el) => el.inert),
      true,
    );
    assert.equal(
      await page
        .locator("#edge-handle")
        .evaluate((el) => el === document.activeElement),
      true,
    );
    await page.getByRole("button", { name: "Restore sample lyrics" }).click();
    await page.getByRole("button", { name: "Next sample line" }).click();
    assert.match(
      await page.locator("#sample-line").innerText(),
      /For a moment/,
    );
    await page.getByRole("checkbox", { name: "English subtext" }).check();
    assert.match(await page.locator("#sample-line").innerText(), /Bir an/);
    assert.equal(await page.locator("#sample-line").getAttribute("lang"), "tr");
    for (const id of ["#sample-previous", "#sample-next"])
      assert.equal(await page.locator(id).getAttribute("lang"), "tr");
    assert.equal(
      await page.locator("#sample-translation").getAttribute("lang"),
      "en",
    );
    assert.equal(await page.locator("#sample-translation").isVisible(), true);
    await page
      .getByText("Where do the lyrics come from?", { exact: true })
      .click();
    assert.equal(
      await page.locator(".faq details").first().getAttribute("open"),
      "",
    );
    const assets = await page
      .locator("a[download]")
      .evaluateAll((nodes) => nodes.map((node) => node.href));
    for (const url of assets)
      assert.equal((await page.request.get(url)).status(), 200);
    const anchors = await page
      .locator('a[href^="#"]')
      .evaluateAll((nodes) =>
        nodes
          .map((node) => node.getAttribute("href"))
          .filter((href) => href !== "#"),
      );
    for (const anchor of anchors)
      assert.equal(await page.locator(anchor).count(), 1);

    for (const width of [320, 390, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 1050 });
      await page.reload({ waitUntil: "networkidle" });
      await page.evaluate(() => document.fonts.ready);
      assert.ok(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        `Overflow at ${width}`,
      );
      assert.equal(await page.locator(".release-group").count(), 3);
      assert.ok(await page.locator("#notes-content").evaluate((el) => {
        const groups = [...el.children].map((group) => group.getBoundingClientRect());
        return groups.every((box) => box.left >= 0 && box.right <= innerWidth) &&
          (innerWidth > 760 || groups.every((box, i) => i === 0 || box.top >= groups[i - 1].bottom));
      }), `Release notes overflow or overlap at ${width}`);
      assert.ok(
        await page.locator("#apk-download").evaluate((el) => {
          const r = el.getBoundingClientRect();
          return r.width >= 44 && r.height >= 44 && r.top + scrollY < 750;
        }),
        `Download too small or too low at ${width}`,
      );
      for (const translated of [false, true]) {
        await page
          .getByRole("checkbox", { name: "English subtext" })
          .setChecked(translated);
        for (const mode of ["Expanded", "Lyrics only", "Floating"]) {
          await page.getByRole("button", { name: mode, exact: true }).click();
          await page.waitForFunction(
            () =>
              !document
                .querySelector(".lyric-window")
                .getAnimations({ subtree: true })
                .some((animation) => animation.playState === "running"),
          );
          assert.ok(
            await page.locator(".lyric-window").evaluate((el) => {
              const r = el.getBoundingClientRect(),
                p = el.parentElement.getBoundingClientRect();
              return (
                r.bottom <= p.bottom + 1 &&
                r.left >= p.left - 1 &&
                r.right <= p.right + 1
              );
            }),
            `${mode}, translated=${translated}: panel clipped at ${width}`,
          );
        }
      }
      for (const format of ["Portrait", "Square"]) {
        await page.getByRole("button", { name: format, exact: true }).click();
        assert.ok(
          await page.locator("#sample-card").evaluate((el) => {
            const r = el.getBoundingClientRect();
            return (
              r.left >= 0 &&
              r.right <= innerWidth &&
              el.scrollHeight <= el.clientHeight + 1
            );
          }),
          `Featured card clipped at ${width}/${format}`,
        );
      }
      await page.getByRole("checkbox", { name: "English subtext" }).uncheck();
      await page.evaluate(() => scrollTo(0, 0));
      if (width === 390 || width === 1440) {
        await page.screenshot({
          path: path.join(
            root,
            `artifacts/site-${width === 390 ? "mobile" : "desktop"}.png`,
          ),
          fullPage: true,
        });
        await page.locator("#cards").screenshot({
          path: path.join(root, `artifacts/site-cards-${width}.png`),
        });
        await page.locator("#release-notes").screenshot({
          path: path.join(root, `artifacts/site-notes-${width}.png`),
          // Large element captures can include a fixed link above the viewport.
          style: ".skip-link:not(:focus) { opacity: 0; }",
        });
        await page.locator("#release-notes").evaluate((el) =>
          el.scrollIntoView({ behavior: "instant", block: "start" }),
        );
        await page.screenshot({
          path: path.join(root, `artifacts/site-notes-viewport-${width}.png`),
        });
      }
    }
    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.getByRole("button", { name: "Next sample line" }).click();
    assert.equal(
      await page
        .locator(".lyric-content")
        .evaluate((el) => el.getAnimations().length),
      0,
    );
    await page.reload({ waitUntil: "networkidle" });
    await page.keyboard.press("Tab");
    assert.equal(
      await page
        .locator(".skip-link")
        .evaluate((el) => el === document.activeElement),
      true,
    );
    await page
      .getByRole("button", { name: "Moss palette", exact: true })
      .focus();
    await page.keyboard.press("Space");
    assert.equal(
      await page.locator("#card-gallery").getAttribute("data-palette"),
      "moss",
    );
    const noJs = await browser.newPage({
      javaScriptEnabled: false,
      viewport: { width: 390, height: 844 },
    });
    await noJs.goto(base, { waitUntil: "networkidle" });
    assert.equal(await noJs.locator("h1").isVisible(), true);
    assert.equal(await noJs.locator("#sample-card").isVisible(), true);
    assert.equal(
      await noJs.locator("#apk-download").getAttribute("href"),
      fallback,
    );
    assert.equal(await noJs.locator("#apk-options").isVisible(), false);
    assert.equal(await noJs.locator("#notes-fallback").isVisible(), true);
    assert.equal(
      await noJs.locator("#notes-fallback a").getAttribute("href"),
      "https://github.com/kmab5/singalong/releases/tag/v1.3.0",
    );
    await noJs.close();
    const release = {
      tag_name: "v1.2.0",
      draft: false,
      prerelease: false,
      assets: [
        {
          name: "singalong.apk",
          size: 10494834,
          browser_download_url:
            "https://github.com/kmab5/singalong/releases/download/v1.2.0/singalong.apk",
        },
      ],
    };
    await page.unroute(api);
    await page.route(api, (route) =>
      route.fulfill({
        contentType: "application/json",
        body: JSON.stringify([
          null,
          { ...release, tag_name: "v1.3.0-rc.1", prerelease: true },
          release,
        ]),
      }),
    );
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(
      await page.locator("[data-release-version]").innerText(),
      "v1.2.0",
    );
    assert.ok(
      await page
        .locator("[data-apk-download]")
        .evaluateAll(
          (nodes) =>
            nodes.length === 3 &&
            nodes.every((node) => node.href.endsWith("/v1.2.0/singalong.apk")),
        ),
    );
    assert.equal(await page.locator("#apk-options").isVisible(), false);
    const universal = release.assets[0];
    const variantAsset = (name) => ({
      name,
      browser_download_url: universal.browser_download_url.replace(
        "singalong.apk",
        name,
      ),
    });
    const variants = [
      "singalong-arm64-v8a.apk",
      "singalong-armeabi-v7a.apk",
      "singalong-x86_64.apk",
      "singalong-x86.apk",
    ];
    async function mockReleases(body) {
      await page.unroute(api);
      await page.route(api, (route) =>
        route.fulfill({
          contentType: "application/json",
          body: JSON.stringify(body),
        }),
      );
      await page.reload({ waitUntil: "networkidle" });
    }
    await mockReleases([
      { ...release, assets: [universal, ...variants.map(variantAsset)] },
    ]);
    const options = page.locator("#apk-options");
    assert.equal(await options.isVisible(), true);
    await options.locator("summary").focus();
    await page.keyboard.press("Enter");
    assert.equal(await options.getAttribute("open"), "");
    assert.deepEqual(
      await options
        .locator("a")
        .evaluateAll((nodes) => nodes.map((node) => node.href)),
      variants.map((name) => variantAsset(name).browser_download_url),
    );
    assert.match(await options.innerText(), /Galaxy S25 FE/);
    for (const width of [320, 390, 1440]) {
      await page.setViewportSize({ width, height: 1050 });
      assert.ok(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      );
      for (const link of await options.locator("a").all()) {
        assert.ok(
          await link.evaluate((el) => el.getBoundingClientRect().height >= 44),
        );
      }
      if (width !== 320)
        await page.locator("#download").screenshot({
          path: path.join(root, `artifacts/site-downloads-${width}.png`),
        });
    }
    // Only this release's canonical, supported APKs may become download links.
    await mockReleases([
      {
        ...release,
        assets: [
          universal,
          variantAsset("singalong-arm64-v8a.apk"),
          variantAsset("unrelated.apk"),
          {
            name: "singalong-armeabi-v7a.apk",
            browser_download_url: "https://example.com/evil.apk",
          },
          {
            name: "singalong-x86.apk",
            browser_download_url: "javascript:alert(1)",
          },
          {
            ...variantAsset("singalong-x86_64.apk"),
            browser_download_url:
              "https://github.com/kmab5/singalong/releases/download/v1.1.0/singalong-x86_64.apk",
          },
          null,
        ],
      },
    ]);
    assert.deepEqual(
      await options
        .locator("a")
        .evaluateAll((nodes) => nodes.map((node) => node.href)),
      [variantAsset("singalong-arm64-v8a.apk").browser_download_url],
    );
    await mockReleases([
      { ...release, assets: [universal, variantAsset("unrelated.apk")] },
    ]);
    assert.equal(await options.isVisible(), false);
    const newerRelease = {
      ...release,
      tag_name: "v2.0.0",
      assets: [universal, ...variants.map(variantAsset)].map((asset) => ({
        ...asset,
        browser_download_url: asset.browser_download_url.replace(
          "v1.2.0",
          "v2.0.0",
        ),
      })),
    };
    await mockReleases([
      { ...newerRelease, tag_name: "v2.1.0", prerelease: true },
      { ...newerRelease, tag_name: "v2.2.0", draft: true },
      newerRelease,
      release,
    ]);
    assert.equal(
      await page.locator("[data-release-version]").innerText(),
      "v2.0.0",
    );
    assert.equal(await page.locator(".release-version").innerText(), "v1.3.0");
    assert.equal(await page.locator("#notes-summary").innerText(), notes.summary);
    assert.equal(await page.locator("#notes-github").getAttribute("href"),
      "https://github.com/kmab5/singalong/releases/tag/v1.3.0");
    assert.equal(
      await page.locator("#apk-download").getAttribute("href"),
      newerRelease.assets[0].browser_download_url,
    );
    assert.deepEqual(
      await options
        .locator("a")
        .evaluateAll((nodes) => nodes.map((node) => node.href)),
      newerRelease.assets.slice(1).map((asset) => asset.browser_download_url),
    );
    assert.ok(
      await page
        .locator("[data-release-link]")
        .evaluateAll((nodes) =>
          nodes.every(
            (node) =>
              node.href ===
              "https://github.com/kmab5/singalong/releases/tag/v2.0.0",
          ),
        ),
    );
    for (const body of [
      "{}",
      "null",
      JSON.stringify([{ tag_name: "v1.2.0" }]),
      JSON.stringify([
        { ...release, assets: [variantAsset("singalong-arm64-v8a.apk")] },
      ]),
      JSON.stringify([
        {
          ...release,
          assets: [
            { name: "singalong.apk" },
            variantAsset("singalong-arm64-v8a.apk"),
          ],
        },
      ]),
      JSON.stringify([
        {
          ...release,
          assets: [
            {
              name: "singalong.apk",
              browser_download_url: "https://example.com/evil.apk",
            },
          ],
        },
      ]),
    ]) {
      await page.unroute(api);
      await page.route(api, (route) =>
        route.fulfill({ contentType: "application/json", body }),
      );
      await page.reload({ waitUntil: "networkidle" });
      assert.equal(
        await page.locator("#apk-download").getAttribute("href"),
        fallback,
      );
      assert.equal(await options.isVisible(), false);
      assert.equal(await options.locator("a").count(), 0);
    }
    await page.unroute(api);
    await page.route(api, (route) => route.abort());
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(
      await page.locator("#apk-download").getAttribute("href"),
      fallback,
    );
    assert.equal(await options.isVisible(), false);
    const notesUrl = `${base}releases.json`;
    for (const body of ["null", "{}", '{"releases":{}}', JSON.stringify({ releases: [] }),
      JSON.stringify({ releases: [{ ...notes, sections: [{ kind: "unknown", items: [] }] }] })]) {
      await page.route(notesUrl, (route) => route.fulfill({ contentType: "application/json", body }));
      await page.reload({ waitUntil: "networkidle" });
      assert.equal(await page.locator("#notes-fallback").isVisible(), true);
      assert.equal(await page.locator("#notes-content").isVisible(), false);
      await page.unroute(notesUrl);
    }
    await page.route(notesUrl, (route) => route.abort());
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(await page.locator("#notes-fallback").isVisible(), true);
    await page.unroute(notesUrl);
    const literalTitle = '<img src=x onerror="window.injected = true">';
    const escapedJournal = structuredClone(journal);
    escapedJournal.releases[0].sections[0].items[0].title = literalTitle;
    await page.route(notesUrl, (route) => route.fulfill({
      contentType: "application/json", body: JSON.stringify(escapedJournal),
    }));
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(await page.locator(".release-group h4").first().innerText(), literalTitle);
    assert.equal(await page.locator("#notes-content img").count(), 0);
    assert.equal(await page.evaluate(() => window.injected), undefined);
    assert.deepEqual(errors, []);
    console.log(
      JSON.stringify(
        {
          status: "passed",
          viewports: [320, 390, 768, 1024, 1440],
          checks: [
            "floating modes and focus",
            "translated layouts",
            "palette/format gallery",
            "keyboard and reduced motion",
            "no-JS content",
            "versioned release journal, mobile layout and safe text rendering",
            "release journal malformed/network/no-JS fallback",
            "release metadata and malformed/network fallback",
            "canonical architecture downloads and legacy/partial release fallback",
            "downloads/assets/anchors",
            "no overflow or page errors",
          ],
          assets: assets.length,
        },
        null,
        2,
      ),
    );
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
