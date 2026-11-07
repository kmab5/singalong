# Testing

Automated checks verify logic and build output. They don’t establish phone compatibility, battery use or overlay behavior on every player.

## Recorded baseline

For **1.2.2 (22)**, local debug and release runs each passed **167 unit tests**. Both lint variants reported **0 errors and 33 warnings**. Debug/release builds and instrumentation-test compilation passed; instrumentation tests were not run.

Local packaging verified all five APKs: existing release certificate, version, matching R8 ID, intended native architecture and 16 KB ZIP alignment. These results apply to 1.2.2, not automatically to later source.

Thirteen release-tool tests passed. Website browser checks passed at five widths, including architecture links, future stable releases, partial/malformed responses and universal fallback. GitHub runs and publication status were not monitored for this update.

## Automated checks

Use the commands in [building](BUILDING.md). Run `:app:assembleDebugAndroidTest` to compile device tests, and [website checks](SITE.md) for web changes. Report compilation separately from execution.

Use synthetic lyrics in fixtures. Test ownership/cancellation, stale responses, qualification clocks, matching, timing, personal-version preservation, atomic restore and deletion races.

## Phone checks

On a release-signed build:

1. Select a player, grant access and start tracking. Play 6 seconds, pause 14, then play 4; expect one qualified play. Seeking must not qualify early.
2. Open floating lyrics, seek, pause and change songs during lookup. Old responses must not replace the current song. Pause for 15 seconds and check automatic docking/recovery; manual close/minimize must persist.
3. Add the Quick Settings tile. Toggle from another app, stop through the app/notification, try rapid taps, missing setup, process restart and lock/unlock. Adding or viewing the tile must not start tracking.
4. Check rotation, large text, TalkBack and overlay-permission revocation. Lyrics must stay off the lock screen. Android can suppress overlays on sensitive screens.
5. Test cached lyrics offline, Wi-Fi-only lookup and tracking shutdown. Fresh-process callbacks must not resume collection without opening the app or explicitly starting through the tile.
6. Import/edit/select lyrics, restart, and restore a backup twice. Check personal versions and play counts. Keep an independent backup before testing deletion.
7. Share/save a lyric card, change artwork and inspect the exported text. Test translation failures without losing original lyrics.

## Compatibility

The primary target is a Samsung S25 FE on Android 16. Samsung Music, Spotify, SoundCloud, Musicolet and AMPlayer are compatibility targets, not a verified support list.

The full player matrix, API 31 hardware coverage, device migrations/recovery and current release acceptance remain unverified here.

For phone results, record the app version, device/OS, player package/version, date and observed behavior. No emulator or device execution is claimed by the automated baseline.
