# Testing

Automated checks verify logic and build output. They don’t establish phone compatibility, battery use or overlay behavior on every player.

## Recorded baseline

For **1.3.0 (25)**, local debug and release runs each passed **204 unit tests**. Coverage includes preferred-version export and attachments, unlimited/large backups, ZIP64 reading, complete preference snapshots, pre-restore validation, floating word-tracking eligibility and shared release-note parsing. Debug lint reported **0 errors / 31 warnings**; release lint reported **0 errors / 33 warnings**. Debug and signed release builds passed.

Device tests for migration, restore identity, preferred export and floating typography compile but have not been run. The maintainer confirmed feature testing and authorized this stable release; the final preferred-export and About changes have automated checks, not additional phone verification.

Local packaging verified all five APKs: existing release certificate, version, matching R8 ID, intended native architecture, 16 KB ZIP alignment and release-note assets matching the website source.

Fourteen release-tool tests and documentation link checks passed. Website browser checks passed at five widths, including release-note content, responsive layout, download fallbacks and the author link. GitHub runs and publication status were not monitored.

## Automated checks

Use the commands in [building](BUILDING.md). Run `:app:assembleDebugAndroidTest` to compile device tests, and [website checks](SITE.md) for web changes. Report compilation separately from execution.

Use synthetic lyrics in fixtures. Test ownership/cancellation, stale responses, qualification clocks, matching, timing, personal-version preservation, atomic restore and deletion races.

## Phone checks

For **1.3.0**, the targeted phone checklist is:

- Set floating alignment and different current/other-line sizes; inspect compact, expanded and minimal modes, translations and large system text.
- Play supplied word-timed lyrics, pause and seek. Turn floating word tracking off and confirm line sync continues. Check that the Now-tab word switch remains independent.
- Export full, links-only, history-only and settings-only ZIPs. Compare All saved versions with Selected / preferred only, including a selected personal edit. Inspect the separate files, restore twice and check the results. Try an older JSON backup as well.
- Open About, expand release notes and follow the Samuel / kmab credit. Notes should work offline; the credit should open the homepage.
- Set a small backup limit to exercise the size error, increase it and retry, then turn it off for Unlimited. The choice must survive restart and apply to both export and restore.
- Inspect the complete settings snapshot for selected players and tracking/translation choices. Restore settings and verify selected apps and appearance return while current tracking and translation consent stay unchanged.
- Restore two recordings with the same title/artist but different album/duration from a partial backup; they must remain separate. Test settings restore both unchecked and checked.
- Install over 1.2.2 to exercise the Room 3→4 migration. Existing saved versions and history must remain available.

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

The maintainer confirmed feature acceptance before release. The full player matrix, API 31 hardware coverage and device migrations/recovery have not been independently verified here. The last additions—preferred-version export and About release notes—have local automated coverage; no device execution is claimed for them.

For phone results, record the app version, device/OS, player package/version, date and observed behavior. No emulator or device execution is claimed by the automated baseline.
