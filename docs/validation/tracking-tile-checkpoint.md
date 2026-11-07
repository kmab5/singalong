# Quick Settings tracking tile

Date: 2026-11-07. Requested patch: 1.2.1 (21), directly on main. The user clarified that the tile belongs beside Wi-Fi/Bluetooth and the current notification must remain unchanged.

## Implemented source

- Added a protected system TileService with the Singalong monochrome icon and tracking on/off labels. Add it through the shade's Edit controls. No new runtime permission or library is required.
- Observe actual container/settings state only while the tile is listening. Adding/viewing the tile never authorizes tracking. An explicit start click can authorize a fresh process through the same app session controls.
- Missing setup opens Singalong; locked-device clicks use Android's unlock flow and re-read state afterward. Starting/stopping temporarily disables commands. Tracking stop retains the existing cleanup and notification behavior.
- A generation-owned start-request helper prevents a superseded start from leaving the tile pending after setup fails. Notification source is unchanged.

## Verification

Source review identified the superseded-start cleanup edge case before publication. The replacement-request cleanup was fixed and re-reviewed with no remaining actionable spec findings.

- Both new test groups were first run before their implementations and failed on missing symbols. Final debug and release unit runs each passed **167 tests**, zero failures/errors, including seven new tile-policy/request-ownership tests.
- Debug/release lint passed with **0 errors and 33 warnings** each. The first lint run flagged the old Intent overload for setup navigation; the final implementation isolates the Android 12/13 fallback with an API guard and narrow documented suppression. Android 14+ uses the PendingIntent overload.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, `testReleaseUnitTest`, `lintRelease` and `assembleRelease` passed using the existing JDK 21/SDK 36. Instrumentation tests were compiled, not run.
- Local release packaging verified version 1.2.1 (21), the existing certificate, matching R8 ID and 16 KB ZIP alignment. Local APK size: 10,495,386 bytes. R8 ID: `02d88d022780af4bcc67ee00575a3fbd0d1a1286c676075847ab5e2c03457374`.
- Ten release-tool tests and all 63 Markdown local-link checks passed. Website browser checks passed at 320/390/768/1024/1440px after adding tile setup instructions.
- The tracking notification source has no diff from v1.2.0. No SDK packages, emulators, signing keys or dependencies were added.

Hosted release/deployment outcomes are recorded after the main/tag push; local success alone does not establish publication.

## Physical checks still required

On the S25 FE, add Singalong through Quick Settings Edit, toggle from another app, and confirm the existing notification appears/disappears. Stop through the app and notification and check the tile refreshes. Try rapid taps, process restart, missing music access/player selection, lock/unlock and unlock cancellation. Check adding/removing/opening the tile never starts tracking without a tap. No emulator or device execution is claimed.
