# Player compatibility

Target device: Samsung S25 FE, Android 16 (user-reported). Device testing is performed by the user; no installed versions or successful player interactions have been observed by the coding agent.

| Player | Package/version | Metadata / position / seek | Overlay / lock / resume | Offline / repeats | Status |
| --- | --- | --- | --- | --- | --- |
| Samsung Music | Record on phone | Not tested | Not tested | Not tested | Pending user test |
| Spotify | Record on phone | Not tested | Not tested | Not tested | Pending user test |
| SoundCloud | Record on phone | Not tested | Not tested | Not tested | Pending user test |
| Musicolet | Record on phone | Not tested | Not tested | Not tested | Pending user test |
| AMPlayer | Record on phone | Not tested | Not tested | Not tested | Pending user test |

For each result record date, exact package/version, device/OS, observed behavior and reproduction steps. Media-session information varies by player; this table does not imply universal support. API 31 compatibility and Room migration/recovery tests also remain unverified on hardware. No emulator is installed for this task.

## Phone walkthrough

1. Install the signed release APK. Allow notification access and overlay permission; select a music app and start tracking.
2. Play 6 seconds, pause 14, then play 4. Expect one qualified play. Seeking forward should not qualify early.
3. Open floating lyrics. Seek, pause, resume and change songs during lookup; old lyrics must not replace the new song.
4. Pause 15 seconds: automatic minimization should restore on play. Manual minimize and close should persist through song changes. Reopen using the notification.
5. Lock/unlock, rotate, resize and use large text/TalkBack. Check that lyrics stay off the lock screen and controls remain reachable.
6. Open cached lyrics offline; test Wi-Fi-only changes. Stop tracking and check notification/window cleanup. Restart the process: tracking resumes only after opening the app, and the overlay starts hidden.
7. Import TXT/LRC, edit a personal copy, select it, restart and reopen. Export a backup and restore it twice; verify no inflated counts or lost personal versions. Keep an independent backup before testing deletion or recovery.
