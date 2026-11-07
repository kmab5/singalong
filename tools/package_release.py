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
from release_assets import APK_NAMES, ABIS


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


def release_outputs(metadata, name, code):
    if metadata.get("applicationId") != "com.kmab.singalong":
        raise ValueError("Unexpected application ID in APK output metadata")
    outputs = {}
    for element in metadata.get("elements", []):
        filters = element.get("filters", [])
        if not filters:
            abi = "universal"
        elif len(filters) == 1 and filters[0].get("filterType") == "ABI":
            abi = filters[0].get("value")
        else:
            raise ValueError("Unexpected APK split filter")
        filename = element.get("outputFile", "")
        if not re.fullmatch(r"[A-Za-z0-9_.-]+\.apk", filename):
            raise ValueError("APK output must be a filename inside the build directory")
        if abi not in APK_NAMES or abi in outputs or filename in outputs.values():
            raise ValueError("Unknown or duplicate APK architecture/output")
        if (element.get("versionName"), element.get("versionCode")) != (name, code):
            raise ValueError("APK output metadata does not match version.properties")
        outputs[abi] = filename
    if set(outputs) != set(APK_NAMES):
        raise ValueError("Build all four ABI APKs and the universal APK before packaging")
    return outputs


def verify_abi(apk, abi):
    with ZipFile(apk) as archive:
        actual = {name.split("/")[1] for name in archive.namelist()
                  if re.fullmatch(r"lib/[^/]+/[^/]+\.so", name)}
    if actual != (ABIS if abi == "universal" else {abi}):
        raise ValueError(f"Native libraries do not match the {abi} APK label: {sorted(actual)}")


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
    source = ROOT / "app/build/outputs/apk/release"
    mapping = ROOT / "app/build/outputs/mapping/release/mapping.txt"
    signer = build_tools / ("apksigner.bat" if os.name == "nt" else "apksigner")
    expected = (ROOT / "release-certificate.sha256").read_text().strip().lower()
    name, code = parse((ROOT / "version.properties").read_text())
    outputs = release_outputs(json.loads((source / "output-metadata.json").read_text()), name, code)
    suffix = ".exe" if os.name == "nt" else ""
    packages = []
    for abi, public_name in APK_NAMES.items():
        apk = source / outputs[abi]
        map_id = verify_mapping(apk, mapping)
        verify_abi(apk, abi)
        verify_certificate(run([signer, "verify", "--verbose", "--print-certs", apk]), expected)
        badging = run([build_tools / ("aapt2" + suffix), "dump", "badging", apk])
        if "package: name='com.kmab.singalong'" not in badging or f"versionCode='{code}'" not in badging or f"versionName='{name}'" not in badging:
            raise SystemExit("APK version does not match version.properties")
        run([build_tools / ("zipalign" + suffix), "-c", "-P", "16", "4", apk])
        packages.append({"abi": abi, "file": public_name, "bytes": apk.stat().st_size,
                         "sha256": hashlib.sha256(apk.read_bytes()).hexdigest()})
    target = ROOT / "artifacts/release"
    public = target / "apks"
    diagnostics = target / "diagnostics"
    public.mkdir(parents=True, exist_ok=True)
    diagnostics.mkdir(parents=True, exist_ok=True)
    for abi, public_name in APK_NAMES.items():
        shutil.copy2(source / outputs[abi], public / public_name)
    shutil.copy2(mapping, diagnostics / "mapping.txt")
    commit = run(["git", "rev-parse", "HEAD"]).strip()
    (diagnostics / "build-info.json").write_text(json.dumps({"versionName": name, "versionCode": code, "commit": commit,
        "certificateSha256": expected, "r8MapId": map_id, "apks": packages}, indent=2) + "\n")
    files = [f"apks/{filename}" for filename in APK_NAMES.values()] + ["diagnostics/mapping.txt", "diagnostics/build-info.json"]
    (diagnostics / "SHA256SUMS").write_text("".join(f"{hashlib.sha256((target / f).read_bytes()).hexdigest()}  {f}\n" for f in files))
    print(f"Verified {name} ({code}); five APKs; existing certificate; ABI contents; R8 mapping; 16KB ZIP alignment")
    for package in packages:
        print(f"  {package['file']}: {package['bytes']} bytes")


if __name__ == "__main__":
    main()
