(() => {
  "use strict";
  const stage = document.querySelector(".demo-stage");
  const windowPanel = document.querySelector(".lyric-window");
  const edge = document.querySelector("#edge-handle");
  const modeButtons = [...document.querySelectorAll(".mode-switch button")];
  const translation = document.querySelector("#translation");
  const line = document.querySelector("#sample-line");
  const subtext = document.querySelector("#sample-translation");
  const nextLine = document.querySelector("#sample-next");
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
    nextLine.textContent = samples[
      (index + 1) % samples.length
    ].original.replace("\n", " ");
  }
  function setMode(mode) {
    stage.dataset.mode = mode;
    edge.hidden = mode !== "docked";
    windowPanel.inert = mode === "docked";
    windowPanel.setAttribute("aria-hidden", String(mode === "docked"));
    modeButtons.forEach((button) =>
      button.setAttribute("aria-pressed", String(button.dataset.mode === mode)),
    );
  }
  modeButtons.forEach((button) =>
    button.addEventListener("click", () => setMode(button.dataset.mode)),
  );
  edge.addEventListener("click", () => {
    setMode("floating");
    modeButtons[0].focus();
  });
  document.querySelector("#next-line").addEventListener("click", () => {
    index = (index + 1) % samples.length;
    showLine();
  });
  translation.addEventListener("change", showLine);
  const download = document.querySelector("#apk-download");
  const releaseStatus = document.querySelector("#release-status");
  fetch("https://api.github.com/repos/kmab5/singalong/releases?per_page=100", {
    headers: { Accept: "application/vnd.github+json" },
    signal: AbortSignal.timeout(5000),
  }).then((response) => {
    if (!response.ok) throw new Error("Release lookup unavailable");
    return response.json();
  }).then((releases) => {
    const release = releases.find((item) => !item.draft && !item.prerelease &&
      item.assets?.some((asset) => asset.name === "singalong.apk"));
    const asset = release?.assets.find((item) => item.name === "singalong.apk");
    if (!asset?.browser_download_url?.startsWith("https://github.com/kmab5/singalong/releases/download/")) return;
    download.href = asset.browser_download_url;
    download.textContent = `Download APK · ${release.tag_name}`;
    releaseStatus.textContent = "Android 12 and newer. Signed with the same release key for updates. Checksums and source are on GitHub Releases.";
  }).catch(() => {
    releaseStatus.textContent = "Android 12 and newer. Download the latest signed APK; release details and checksums are on GitHub.";
  });
})();
