const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs = require("node:fs/promises");
const path = require("node:path");
const http = require("node:http");
const assert = require("node:assert/strict");
const root = path.resolve(__dirname, "..");
const docs = path.join(root, "docs");
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
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".woff2": "font/woff2",
    ".txt": "text/plain",
  };
  const server = http.createServer(async (req, res) => {
    try {
      let urlPath = decodeURIComponent(
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
    await page.route("https://api.github.com/repos/kmab5/singalong/releases?*", (route) => route.fulfill({ contentType: "application/json", body: "[]" }));
    page.on("pageerror", (e) => errors.push(String(e)));
    page.on("response", (r) => {
      if (r.status() >= 400) errors.push(`${r.status()} ${r.url()}`);
    });
    await page.goto(base, { waitUntil: "networkidle" });
    assert.equal(await page.locator("#apk-download").getAttribute("href"), "https://github.com/kmab5/singalong/releases/latest/download/singalong.apk");
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({
      path: path.join(root, "artifacts/site-desktop.png"),
      fullPage: true,
    });
    assert.equal(await page.locator("h1").innerText(), "Stay with\nthe song.");
    await page.getByRole("button", { name: "Expand sample lyrics", exact: true }).click();
    assert.equal(await page.locator(".demo-stage").getAttribute("data-mode"), "expanded");
    assert.equal(await page.locator("#sample-previous").isVisible(), true);
    await page.getByRole("button", { name: "Compact sample lyrics", exact: true }).click();
    assert.equal(await page.locator(".demo-stage").getAttribute("data-mode"), "floating");
    await page.getByRole("button", { name: "Dock sample lyrics", exact: true }).click();
    assert.equal(await page.locator("#edge-handle").evaluate(el => el === document.activeElement), true);
    await page.getByRole("button", { name: "Restore sample floating lyrics" }).click();
    await page.getByRole("button", { name: "Minimal", exact: true }).click();
    assert.equal(
      await page.locator(".demo-stage").getAttribute("data-mode"),
      "minimal",
    );
    assert.equal(await page.locator(".lyric-header").isVisible(), false);
    await page.getByRole("checkbox", { name: "English subtext" }).check();
    assert.equal(await page.locator("#sample-translation").isVisible(), true);
    assert.match(await page.locator("#sample-line").innerText(), /Bir sonraki/);
    await page.getByRole("button", { name: "Docked", exact: true }).click();
    assert.equal(
      await page.locator(".lyric-window").evaluate((el) => el.inert),
      true,
    );
    await page
      .getByRole("button", { name: "Restore sample floating lyrics" })
      .click();
    assert.equal(
      await page.locator(".demo-stage").getAttribute("data-mode"),
      "floating",
    );
    await page
      .getByRole("button", { name: "Show next sample lyric line" })
      .click();
    assert.match(await page.locator("#sample-line").innerText(), /Daha/);
    await page.getByRole("checkbox", { name: "English subtext" }).uncheck();
    await page
      .getByText("Where do the words come from?", { exact: true })
      .click();
    assert.equal(
      await page.locator("details").first().getAttribute("open"),
      "",
    );
    const assets = await page
      .locator("a[download]")
      .evaluateAll((nodes) => nodes.map((n) => n.href));
    for (const url of assets)
      assert.equal((await page.request.get(url)).status(), 200);
    const localLinks = await page
      .locator('a[href^="#"]')
      .evaluateAll((nodes) => nodes.map((n) => n.getAttribute("href")));
    for (const link of localLinks)
      assert.equal(await page.locator(link).count(), 1);
    for (const width of [320, 390, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      await page.reload({ waitUntil: "networkidle" });
      assert.ok(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        `Overflow at ${width}`,
      );
      await page.getByRole("checkbox", { name: "English subtext" }).check();
      const clipping = await page.locator(".lyric-window").evaluate((el) => {
        const b = el.getBoundingClientRect(),
          p = el.parentElement.getBoundingClientRect();
        return b.bottom > p.bottom + 1;
      });
      assert.equal(clipping, false, `Translated panel clipped at ${width}`);
      for (const mode of ["Expanded", "Minimal", "Floating"]) {
        await page.getByRole("button", { name: mode, exact: true }).click();
        await page.waitForFunction(() => {
          const panel = document.querySelector(".lyric-window");
          panel.getBoundingClientRect();
          return !panel.getAnimations({subtree:true}).some(animation => animation.playState === "running");
        });
        assert.ok(await page.locator(".lyric-window").evaluate(el => {
          const b = el.getBoundingClientRect(), p = el.parentElement.getBoundingClientRect();
          return b.bottom <= p.bottom + 1 && b.right <= p.right + 1 && b.left >= p.left;
        }), `${mode} panel clipped at ${width}`);
      }
      assert.ok(await page.locator("#apk-download").evaluate(el => {
        const b = el.getBoundingClientRect();
        return b.width >= 44 && b.height >= 44 && b.top + window.scrollY < 750;
      }), `Hero download should be prominent at ${width}`);
      if (width === 390) {
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.screenshot({
          path: path.join(root, "artifacts/site-mobile.png"),
          fullPage: true,
        });
      }
    }
    await page.emulateMedia({ reducedMotion: "reduce" });
    assert.equal(
      await page
        .locator(".lyric-window")
        .evaluate((el) => getComputedStyle(el).transitionDuration),
      "0s",
    );
    await page.getByRole("button", { name: "Show next sample lyric line" }).click();
    assert.equal(await page.locator(".lyric-content").evaluate(el => el.getAnimations().length), 0);
    await page.reload({ waitUntil: "networkidle" });
    await page.keyboard.press("Tab");
    assert.equal(
      await page
        .locator(".skip-link")
        .evaluate((el) => el === document.activeElement),
      true,
    );
    assert.equal(
      await page
        .locator(".skip-link")
        .evaluate((el) => getComputedStyle(el).opacity),
      "1",
    );
    await page.getByRole("button", { name: "Minimal", exact: true }).focus();
    await page.keyboard.press("Space");
    assert.equal(
      await page.locator(".demo-stage").getAttribute("data-mode"),
      "minimal",
    );
    const noJs = await browser.newPage({
      javaScriptEnabled: false,
      viewport: { width: 390, height: 844 },
    });
    await noJs.goto(base, { waitUntil: "networkidle" });
    assert.equal(await noJs.locator("h1").isVisible(), true);
    assert.ok(await noJs.locator("[data-apk-download]").evaluateAll(nodes =>
      nodes.length === 3 && nodes.every(node => node.href === "https://github.com/kmab5/singalong/releases/latest/download/singalong.apk")));
    assert.match(
      await noJs.locator("noscript").innerText(),
      /Enable JavaScript/,
    );
    await noJs.close();
    await page.unroute("https://api.github.com/repos/kmab5/singalong/releases?*");
    await page.route("https://api.github.com/repos/kmab5/singalong/releases?*", (route) => route.fulfill({ contentType: "application/json", body: JSON.stringify([
      { tag_name: "v2.0.0-rc.1", prerelease: true, assets: [{ name: "singalong.apk", browser_download_url: "https://github.com/kmab5/singalong/releases/download/v2.0.0-rc.1/singalong.apk" }] },
      { tag_name: "v1.1.0", prerelease: false, draft: false, assets: [{ name: "singalong.apk", size: 10100000, browser_download_url: "https://github.com/kmab5/singalong/releases/download/v1.1.0/singalong.apk" }] },
    ]) }));
    await page.reload({ waitUntil: "networkidle" });
    assert.ok(await page.locator("[data-apk-download]").evaluateAll(nodes => nodes.length === 3 && nodes.every(node => node.href === "https://github.com/kmab5/singalong/releases/download/v1.1.0/singalong.apk")));
    assert.equal(await page.locator("[data-release-version]").textContent(), "v1.1.0");
    assert.equal(await page.locator("[data-release-size]").textContent(), "10.1 MB");
    assert.ok(await page.locator("[data-release-link]").evaluateAll(nodes => nodes.every(node => node.href.endsWith("/releases/tag/v1.1.0"))));
    await page.unroute("https://api.github.com/repos/kmab5/singalong/releases?*");
    await page.route("https://api.github.com/repos/kmab5/singalong/releases?*", route => route.fulfill({contentType:"application/json",body:'{"message":"API unavailable"}'}));
    await page.reload({waitUntil:"networkidle"});
    assert.equal(await page.locator("#apk-download").getAttribute("href"), "https://github.com/kmab5/singalong/releases/latest/download/singalong.apk");
    await page.unroute("https://api.github.com/repos/kmab5/singalong/releases?*");
    await page.route("https://api.github.com/repos/kmab5/singalong/releases?*", (route) => route.abort());
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(await page.locator("#apk-download").getAttribute("href"), "https://github.com/kmab5/singalong/releases/latest/download/singalong.apk");
    assert.deepEqual(errors, []);
    console.log(
      JSON.stringify(
        {
          status: "passed",
          viewports: [320, 390, 768, 1024, 1440],
          checks: [
            "panel modes",
            "bottom controls and focus on dock",
            "consistent release downloads and metadata",
            "malformed response and network fallback",
            "edge restore",
            "subtext",
            "next line",
            "FAQ",
            "asset downloads",
            "anchors",
            "no overflow",
            "no translated clipping",
            "reduced motion",
            "keyboard controls and skip link",
            "no-JS content",
            "no browser/resource errors",
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
