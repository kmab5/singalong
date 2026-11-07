"""Stable public APK names shared by packaging and publication checks."""
APK_NAMES = {
    "universal": "singalong.apk",
    "arm64-v8a": "singalong-arm64-v8a.apk",
    "armeabi-v7a": "singalong-armeabi-v7a.apk",
    "x86": "singalong-x86.apk",
    "x86_64": "singalong-x86_64.apk",
}
ABIS = set(APK_NAMES) - {"universal"}
