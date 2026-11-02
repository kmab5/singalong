"""Verify identity and version before packaging files for phone tests or GitHub Releases."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from zipfile import ZipFile
from release_version import ROOT, parse


def run(command):
    return subprocess.run([str(p) for p in command], check=True, capture_output=True, text=True).stdout


def verify_certificate(output, expected):
    match = re.search(r"Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)", output)
    if not match or match[1].lower() != expected.strip().lower() or not re.search(r"(?m)^Number of signers: 1\s*$", output):
        raise ValueError("Signing certificate mismatch. Refusing to package this APK.")


def verify_mapping(apk, mapping):
    match = re.search(r"(?m)^# pg_map_id: ([a-f0-9]+)$", mapping.read_text(encoding="utf-8"))
    if match:
        marker = ("r8-map-id-" + match[1]).encode()
        with ZipFile(apk) as archive:
            if any(marker in archive.read(name) for name in archive.namelist() if re.fullmatch(r"classes\d*\.dex", name)):
                return match[1]
    raise ValueError("Mapping does not match this APK. Wait for the build to finish before packaging.")


def main():
    sdk_path = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    local = ROOT / "local.properties"
    if not sdk_path and local.exists():
        sdk_path = next((line.split("=", 1)[1].strip().replace("\\:", ":").replace("\\\\", "\\")
                         for line in local.read_text().splitlines() if line.startswith("sdk.dir=")), None)
    if not sdk_path:
        raise SystemExit("Set ANDROID_HOME or configure sdk.dir in local.properties.")
    sdk = Path(sdk_path)
    build_tools = sdk / "build-tools" / "36.0.0"
    apk = ROOT / "app/build/outputs/apk/release/app-release.apk"
    mapping = ROOT / "app/build/outputs/mapping/release/mapping.txt"
    map_id = verify_mapping(apk, mapping)
    signer = build_tools / ("apksigner.bat" if os.name == "nt" else "apksigner")
    output = run([signer, "verify", "--verbose", "--print-certs", apk])
    expected = (ROOT / "release-certificate.sha256").read_text().strip().lower()
    verify_certificate(output, expected)
    name, code = parse((ROOT / "version.properties").read_text())
    suffix = ".exe" if os.name == "nt" else ""
    badging = run([build_tools / ("aapt2" + suffix), "dump", "badging", apk])
    if "package: name='com.kmab.singalong'" not in badging or f"versionCode='{code}'" not in badging or f"versionName='{name}'" not in badging:
        raise SystemExit("APK version does not match version.properties")
    run([build_tools / ("zipalign" + suffix), "-c", "-P", "16", "4", apk])
    target = ROOT / "artifacts/release"
    target.mkdir(parents=True, exist_ok=True)
    shutil.copy2(apk, target / "singalong.apk")
    shutil.copy2(mapping, target / "mapping.txt")
    commit = run(["git", "rev-parse", "HEAD"]).strip()
    (target / "build-info.json").write_text(json.dumps({"versionName": name, "versionCode": code, "commit": commit, "certificateSha256": expected, "r8MapId": map_id}, indent=2) + "\n")
    (target / "SHA256SUMS").write_text("".join(f"{hashlib.sha256((target / f).read_bytes()).hexdigest()}  {f}\n" for f in ("singalong.apk", "mapping.txt", "build-info.json")))
    print(f"Verified {name} ({code}); existing certificate; 16KB ZIP alignment; {apk.stat().st_size} bytes")


if __name__ == "__main__":
    main()
