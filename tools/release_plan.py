"""Plan publication without moving tags or replacing assets."""
import json
import os
from pathlib import Path
import subprocess
import urllib.error
import urllib.request
from release_version import ROOT, git, parse, validate
from release_assets import APK_NAMES

REQUIRED_ASSETS = set(APK_NAMES.values())


def decide(name, code, ref, release, tagged_commit, head, app_changed=False):
    tag = "v" + name
    if ref != "refs/heads/main" and ref != "refs/tags/" + tag:
        raise ValueError("Release must run on main or the tag matching version.properties")
    validate(name, code, tag)
    if release is not None:
        if release.get("draft") or not REQUIRED_ASSETS.issubset({asset["name"] for asset in release.get("assets", [])}):
            raise ValueError("An incomplete release exists; inspect it rather than replacing its assets")
        if app_changed:
            raise ValueError("App source changed after publication. Bump versionName and versionCode")
        return tag, False
    if tagged_commit is not None and tagged_commit != head:
        raise ValueError("Version tag points to another commit. Never move it; use a new version")
    return tag, True


def fetch_release(repository, tag, token):
    request = urllib.request.Request(f"https://api.github.com/repos/{repository}/releases/tags/{tag}",
        headers={"Accept": "application/vnd.github+json", "Authorization": f"Bearer {token}", "User-Agent": "singalong-release"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as failure:
        if failure.code == 404:
            return None
        raise


def main():
    name, code = parse((ROOT / "version.properties").read_text())
    tag = "v" + name
    ref = os.environ["GITHUB_REF"]
    repository = os.environ["GITHUB_REPOSITORY"]
    if repository != "kmab5/singalong":
        raise ValueError("Publication is restricted to the upstream repository")
    existing = subprocess.run(["git", "rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}"],
        cwd=ROOT, text=True, capture_output=True)
    tagged_commit = existing.stdout.strip() if existing.returncode == 0 else None
    release = fetch_release(repository, tag, os.environ["GH_TOKEN"])
    app_changed = False
    if release is not None:
        if tagged_commit is None:
            raise ValueError("Published tag missing locally; fetch all tags")
        comparison = subprocess.run(["git", "diff", "--quiet", tag, "HEAD", "--", "app", "gradle",
            "gradlew", "gradlew.bat", "build.gradle.kts", "settings.gradle.kts", "gradle.properties",
            "version.properties", "tools/package_release.py", "tools/release_assets.py", "tools/prepare_ci_signing.py"], cwd=ROOT)
        if comparison.returncode not in (0, 1):
            raise ValueError("Could not compare published source")
        app_changed = comparison.returncode == 1
    tag, publish = decide(name, code, ref, release, tagged_commit, git("rev-parse", "HEAD"), app_changed)
    with Path(os.environ["GITHUB_OUTPUT"]).open("a") as output:
        output.write(f"tag={tag}\npublish={str(publish).lower()}\n")
    print(f"{tag}: {'publish verified source' if publish else 'already published; app unchanged'}")


if __name__ == "__main__":
    main()
