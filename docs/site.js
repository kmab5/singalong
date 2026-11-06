(() => {
  "use strict";
  const cardCoverButtons = [...document.querySelectorAll("[data-card-cover]")];
  cardCoverButtons.forEach(button => button.addEventListener("click", () => {
    document.querySelector("#sample-card").dataset.cover = button.dataset.cardCover;
    cardCoverButtons.forEach(option => option.setAttribute("aria-pressed", String(option === button)));
  }));
  const stage = document.querySelector(".demo-stage");
  const windowPanel = document.querySelector(".lyric-window");
  const edge = document.querySelector("#edge-handle");
  const modeButtons = [...document.querySelectorAll(".mode-switch button")];
  const translation = document.querySelector("#translation");
  const line = document.querySelector("#sample-line");
  const subtext = document.querySelector("#sample-translation");
  const nextLine = document.querySelector("#sample-next");
  const previousLine = document.querySelector("#sample-previous");
  const lyricContent = document.querySelector(".lyric-content");
  const expandButton = document.querySelector("#demo-expand");
  const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)");
  const modeDescriptions = {
    floating: "Your player underneath. Your lyrics within reach.",
    expanded: "More of the song, with the current line in focus.",
    minimal: "Just the words. A little less window, a little more space.",
    docked: "Tucked out of the way. Tap the edge handle to bring it back.",
  };
  const samples = [
    {
      original: "Let the next line\nfind you here.",
      turkish: "Bir sonraki satır\nseni burada bulsun.",
      english: "Let the next line find you here.",
    },
    {
      original: "There’s a little room\nfor a louder voice.",
      turkish: "Daha yüksek bir ses için\nbiraz yer var.",
      english: "There’s a little room for a louder voice.",
    },
    {
      original: "Keep the window open.\nKeep the words close.",
      turkish: "Pencereyi açık tut.\nKelimeleri yakın tut.",
      english: "Keep the window open. Keep the words close.",
    },
  ];
  let index = 0;
  function showLine() {
    const sample = samples[index];
    line.textContent = translation.checked ? sample.turkish : sample.original;
    subtext.textContent = sample.english;
    subtext.hidden = !translation.checked;
    const language = translation.checked ? "turkish" : "original";
    nextLine.textContent = samples[(index + 1) % samples.length][language].replace("\n", " ");
    previousLine.textContent = samples[(index + samples.length - 1) % samples.length][language].replace("\n", " ");
    lyricContent.getAnimations().forEach((animation) => animation.cancel());
    if (!reducedMotion.matches) {
      lyricContent.animate(
        [{ opacity: 0.45, transform: "translateY(5px)" }, { opacity: 1, transform: "translateY(0)" }],
        { duration: 220, easing: "cubic-bezier(0.2, 0, 0, 1)" },
      );
    }
  }
  function setMode(mode) {
    const focusInPanel = windowPanel.contains(document.activeElement);
    stage.dataset.mode = mode;
    edge.hidden = mode !== "docked";
    windowPanel.inert = mode === "docked";
    windowPanel.setAttribute("aria-hidden", String(mode === "docked"));
    modeButtons.forEach((button) =>
      button.setAttribute("aria-pressed", String(button.dataset.mode === mode)),
    );
    document.querySelector("#mode-description").textContent = modeDescriptions[mode];
    const expandLabel = mode === "expanded" ? "Compact sample lyrics" : "Expand sample lyrics";
    expandButton.setAttribute("aria-label", expandLabel);
    expandButton.title = expandLabel;
    if (mode === "docked" && focusInPanel) edge.focus();
  }
  modeButtons.forEach((button) =>
    button.addEventListener("click", () => setMode(button.dataset.mode)),
  );
  edge.addEventListener("click", () => {
    setMode("floating");
    modeButtons[0].focus();
  });
  document.querySelector("#demo-dock").addEventListener("click", () => setMode("docked"));
  expandButton.addEventListener("click", () => setMode(stage.dataset.mode === "expanded" ? "floating" : "expanded"));
  document.querySelector("#next-line").addEventListener("click", () => {
    index = (index + 1) % samples.length;
    showLine();
  });
  translation.addEventListener("change", showLine);
  reducedMotion.addEventListener("change", () => {
    if (reducedMotion.matches) lyricContent.getAnimations().forEach((animation) => animation.cancel());
  });
  const downloads = [...document.querySelectorAll("[data-apk-download]")];
  const releaseStatus = document.querySelector("#release-status");
  fetch("https://api.github.com/repos/kmab5/singalong/releases?per_page=100", {
    headers: { Accept: "application/vnd.github+json" },
    signal: AbortSignal.timeout(5000),
  }).then((response) => {
    if (!response.ok) throw new Error("Release lookup unavailable");
    return response.json();
  }).then((releases) => {
    if (!Array.isArray(releases)) return;
    const release = releases.find((item) => !item.draft && !item.prerelease &&
      /^v\d+\.\d+\.\d+$/.test(item.tag_name) &&
      Array.isArray(item.assets) && item.assets.some((asset) => asset.name === "singalong.apk"));
    const asset = release?.assets.find((item) => item.name === "singalong.apk");
    if (!asset?.browser_download_url?.startsWith("https://github.com/kmab5/singalong/releases/download/")) return;
    downloads.forEach((download) => { download.href = asset.browser_download_url; });
    document.querySelectorAll("[data-release-version]").forEach((label) => { label.textContent = release.tag_name; });
    document.querySelectorAll("[data-release-link]").forEach((link) => {
      link.href = `https://github.com/kmab5/singalong/releases/tag/${release.tag_name}`;
    });
    if (Number.isFinite(asset.size) && asset.size > 0) {
      document.querySelectorAll("[data-release-size]").forEach((label) => {
        label.textContent = `${(asset.size / 1000000).toFixed(1)} MB`;
      });
    }
    releaseStatus.textContent = "Android 12 and newer. Signed with the same release key for updates. Checksums and source are on GitHub Releases.";
  }).catch(() => {
    releaseStatus.textContent = "Android 12 and newer. Download the latest signed APK; release details and checksums are on GitHub.";
  });
})();
