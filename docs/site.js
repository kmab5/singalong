(() => {
  "use strict";
  const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)");
  const stage = document.querySelector(".demo-stage");
  const panel = document.querySelector(".lyric-window");
  const edge = document.querySelector("#edge-handle");
  const modes = [...document.querySelectorAll("[data-mode]")].filter(
    (el) => el.tagName === "BUTTON",
  );
  const english = document.querySelector("#translation");
  const samples = [
    {
      original: "I keep the\nchorus close.",
      translated: "Nakaratı\nyakınımda tutarım.",
      subtext: "I keep the chorus close.",
    },
    {
      original: "For a moment,\nI am home.",
      translated: "Bir an için,\nevimdeyim.",
      subtext: "For a moment, I am home.",
    },
    {
      original: "The city\nslips away.",
      translated: "Şehir\nuzaklaşır.",
      subtext: "The city slips away.",
    },
  ];
  let index = 0;
  const content = document.querySelector(".lyric-content");
  function showLine() {
    const sample = samples[index];
    const key = english.checked ? "translated" : "original";
    const [first, second] = sample[key].split("\n");
    const line = document.querySelector("#sample-line");
    line.replaceChildren(
      document.createTextNode(first),
      document.createElement("br"),
    );
    const emphasis = document.createElement("em");
    emphasis.textContent = second;
    line.append(emphasis);
    line.lang = english.checked ? "tr" : "en";
    document.querySelector("#sample-previous").lang = line.lang;
    document.querySelector("#sample-next").lang = line.lang;
    document.querySelector("#sample-previous").textContent = samples[
      (index + 2) % 3
    ][key].replace("\n", " ");
    document.querySelector("#sample-next").textContent = samples[
      (index + 1) % 3
    ][key].replace("\n", " ");
    const translation = document.querySelector("#sample-translation");
    translation.lang = "en";
    translation.textContent = sample.subtext;
    translation.hidden = !english.checked;
    content.getAnimations().forEach((animation) => animation.cancel());
    if (!reducedMotion.matches)
      content.animate(
        [
          { opacity: 0.3, transform: "translateY(5px)" },
          { opacity: 1, transform: "translateY(0)" },
        ],
        { duration: 280, easing: "ease-out" },
      );
  }
  function setMode(mode) {
    const focused = panel.contains(document.activeElement);
    stage.dataset.mode = mode;
    panel.inert = mode === "docked";
    panel.setAttribute("aria-hidden", String(mode === "docked"));
    edge.hidden = mode !== "docked";
    modes.forEach((button) =>
      button.setAttribute("aria-pressed", String(button.dataset.mode === mode)),
    );
    if (mode === "docked" && focused) edge.focus();
  }
  modes.forEach((button) =>
    button.addEventListener("click", () => setMode(button.dataset.mode)),
  );
  document
    .querySelector("#demo-dock")
    .addEventListener("click", () => setMode("docked"));
  edge.addEventListener("click", () => {
    setMode("floating");
    modes[0].focus();
  });
  document.querySelector("#next-line").addEventListener("click", () => {
    index = (index + 1) % samples.length;
    showLine();
  });
  english.addEventListener("change", showLine);
  reducedMotion.addEventListener("change", () => {
    if (reducedMotion.matches)
      content.getAnimations().forEach((animation) => animation.cancel());
  });
  const gallery = document.querySelector("#card-gallery");
  for (const kind of ["palette", "format"]) {
    const buttons = [...document.querySelectorAll(`button[data-${kind}]`)];
    buttons.forEach((button) =>
      button.addEventListener("click", () => {
        gallery.dataset[kind] = button.dataset[kind];
        buttons.forEach((item) =>
          item.setAttribute("aria-pressed", String(item === button)),
        );
        const title = (text) => text[0].toUpperCase() + text.slice(1);
        document.querySelector("#card-description").textContent =
          `${title(gallery.dataset.palette)} · ${title(gallery.dataset.format)}`;
      }),
    );
  }
  const status = document.querySelector("#release-status");
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 5000);
  fetch("https://api.github.com/repos/kmab5/singalong/releases?per_page=100", {
    headers: { Accept: "application/vnd.github+json" },
    signal: controller.signal,
  })
    .then((response) => {
      if (!response.ok) throw new Error("Release lookup unavailable");
      return response.json();
    })
    .then((releases) => {
      if (!Array.isArray(releases)) return;
      const release = releases.find(
        (item) =>
          item &&
          !item.draft &&
          !item.prerelease &&
          /^v\d+\.\d+\.\d+$/.test(item.tag_name) &&
          Array.isArray(item.assets) &&
          item.assets.some((asset) => asset?.name === "singalong.apk"),
      );
      const apk = release?.assets.find(
        (asset) => asset?.name === "singalong.apk",
      );
      if (!apk || typeof apk.browser_download_url !== "string") return;
      const prefix = `https://github.com/kmab5/singalong/releases/download/${release.tag_name}/`;
      if (apk.browser_download_url !== prefix + "singalong.apk") return;
      document.querySelectorAll("[data-apk-download]").forEach((link) => {
        link.href = apk.browser_download_url;
      });
      document.querySelectorAll("[data-release-link]").forEach((link) => {
        link.href = `https://github.com/kmab5/singalong/releases/tag/${release.tag_name}`;
      });
      document.querySelectorAll("[data-release-version]").forEach((label) => {
        label.textContent = release.tag_name;
      });
      const variants = [
        [
          "singalong-arm64-v8a.apk",
          "ARM64",
          "Most modern phones, including Galaxy S25 FE",
        ],
        ["singalong-armeabi-v7a.apk", "ARMv7", "Older 32-bit ARM devices"],
        ["singalong-x86_64.apk", "x86_64", "64-bit Intel or AMD devices"],
        ["singalong-x86.apk", "x86", "32-bit Intel or AMD devices"],
      ];
      const downloads = document.querySelector("#apk-variants");
      for (const [filename, label, description] of variants) {
        const asset = release.assets.find(
          (item) =>
            item?.name === filename &&
            item.browser_download_url === prefix + filename,
        );
        if (!asset) continue;
        const item = document.createElement("li");
        const link = document.createElement("a");
        link.href = asset.browser_download_url;
        const title = document.createElement("strong");
        title.textContent = `${label} APK`;
        const detail = document.createElement("span");
        detail.textContent = description;
        link.append(title, detail);
        item.append(link);
        downloads.append(item);
      }
      document.querySelector("#apk-options").hidden =
        !downloads.childElementCount;
      const size =
        Number.isFinite(apk.size) && apk.size > 0
          ? ` · ${(apk.size / 1_000_000).toFixed(1)} MB`
          : "";
      status.textContent = `${release.tag_name}${size}. Universal APK. Signed with the same key for updates.`;
    })
    .catch(() => {
      status.textContent =
        "Download the latest signed universal APK. Release notes are on GitHub.";
    })
    .finally(() => clearTimeout(timeout));
})();
