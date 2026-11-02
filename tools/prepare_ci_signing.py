"""Restore only the existing keystore. Passwords remain in the CI environment."""
import base64
import os
from pathlib import Path

required = ("ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD")
if any(not os.environ.get(key) for key in required):
    raise SystemExit("Missing release signing secrets. Restore the existing key; never generate a replacement.")
target = Path(os.environ["RUNNER_TEMP"]) / "singalong-release.jks"
target.write_bytes(base64.b64decode(os.environ["ANDROID_KEYSTORE_BASE64"], validate=True))
target.chmod(0o600)
with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as output:
    output.write(f"SINGALONG_KEYSTORE_PATH={target.as_posix()}\n")
print("Existing release keystore restored for this job.")
