"""Single version source, strict release tags, and monotonically increasing Android codes."""
import argparse
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PATTERN = re.compile(r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-rc\.([1-9]\d*))?")


def version_key(name):
    match = PATTERN.fullmatch(name)
    if not match:
        raise ValueError("Use MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-rc.N")
    major, minor, patch, rc = match.groups()
    return (int(major), int(minor), int(patch), 1 if rc is None else 0, int(rc or 0))


def parse(text):
    values = dict(line.split("=", 1) for line in text.splitlines() if "=" in line and not line.startswith("#"))
    name, code = values["versionName"], int(values["versionCode"])
    version_key(name)
    if not 1 <= code <= 2_100_000_000:
        raise ValueError("Android versionCode is out of range")
    return name, code


def validate(name, code, tag=None, previous=(), allow_current=False):
    key = version_key(name)
    if tag is not None and tag != "v" + name:
        raise ValueError("Tag must match version.properties exactly")
    for old_name, old_code in previous:
        if allow_current and (name, code) == (old_name, old_code):
            continue
        if code <= old_code or key <= version_key(old_name):
            raise ValueError("Version name and code must increase beyond every earlier release")


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, text=True, capture_output=True, check=True).stdout.strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("check", "set"))
    parser.add_argument("name", nargs="?")
    parser.add_argument("--code", type=int)
    parser.add_argument("--tag")
    args = parser.parse_args()
    path = ROOT / "version.properties"
    name, code = parse(path.read_text())
    if args.command == "set":
        if args.name is None or args.code is None:
            parser.error("set requires a name and --code")
        new_name, new_code = parse(f"versionName={args.name}\nversionCode={args.code}")
        validate(new_name, new_code, previous=[(name, code)])
        name, code = new_name, new_code
    previous = []
    for tag in git("tag", "--list", "v*").splitlines():
        if tag == args.tag:
            continue
        result = subprocess.run(["git", "show", f"{tag}:version.properties"], cwd=ROOT, capture_output=True, text=True)
        if result.returncode == 0:
            previous.append(parse(result.stdout))
    validate(name, code, args.tag, previous, allow_current=args.command == "check" and args.tag is None)
    if args.command == "set":
        path.write_text(f"versionName={name}\nversionCode={code}\n")
    print(f"{name} ({code})")


if __name__ == "__main__":
    main()
