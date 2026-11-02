const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs = require("node:fs/promises");
const path = require("node:path");
const root = path.resolve(__dirname, "..");
const docs = path.join(root, "docs");
(async () => {
  const browser = await chromium.launch({
    executablePath: process.env.CHROME_PATH || undefined,
    headless: true,
  });
  try {
    const exportPage = await browser.newPage();
    for (const name of ["singalong-icon", "singalong-logo"]) {
      const svg = await fs.readFile(
        path.join(docs, "assets", name + ".svg"),
        "utf8",
      );
      const width = Number(svg.match(/width="(\d+)"/)[1]);
      const height = Number(svg.match(/height="(\d+)"/)[1]);
      await exportPage.setViewportSize({ width, height });
      await exportPage.setContent(
        "<style>html,body{margin:0;background:transparent}svg{display:block}</style>" +
          svg,
      );
      await exportPage.screenshot({
        path: path.join(docs, "assets", name + ".png"),
        omitBackground: true,
      });
      await fs.copyFile(
        path.join(docs, "assets", name + ".png"),
        path.join(root, "public/docs", name + ".png"),
      );
    }
    await exportPage.close();
    console.log(
      "Rendered transparent PNG assets to docs/assets and public/docs.",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
