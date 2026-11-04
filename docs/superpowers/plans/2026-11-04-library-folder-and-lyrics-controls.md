# Library folder scan and lyric controls

Requested scope: responsive floating controls, tap-only edge expansion, library sorting, folder lyric collection, independent current timed-line size, a more expressive animation and full-width settings selections.

## Design and implementation

1. Use available overlay width to distribute five 48 dp actions over rows; grow the minimum controlled-panel height to preserve reading space. Dragging the edge handle moves it and settles at the nearest edge; only tapping opens it. Keep accessible movement actions.
2. Add persistent library sorting (title, artist, album, recently played and most played), using stable tie-breakers and existing search/favorite filters. History retains its chronological default.
3. Add a folder picker in Library. A scan includes subfolders, reads audio tags on IO, and passes original metadata/filename through existing search inference. Process one lookup at a time with pacing, bounded retries/cooldowns, progress and Cancel. Respect Wi-Fi-only/network state. Save successful matches transactionally, rechecking library identity and content before insertion; preserve selections/personal versions and never create play history. Unavailable or unreadable tracks are counted, not saved as empty library entries. Runs belong to the application session; keep the app open and re-scan after process termination (existing successes are skipped).
4. Keep normal Now text size and introduce current timed-line size, persisted separately. Add an opt-in Word glow mode: estimated word emphasis between line timestamps, not real word timing. Retain original text/timestamps, respect playback pause/seek/speed/offset and reduced motion; do not animate untimed or unfollowed lyrics as if they have word timing.
5. Settings selection panels and chip groups use available width; preserve wrapping and accessible target sizes.

## Verification

Add pure logic tests for sorting, deduplication/import cancellation/retries, word progression and overlay geometry. Compile device tests for drag versus tap and narrow controls. Run debug/release unit tests, lint and signed APK assembly with the existing JDK/SDK/key. User performs physical testing; no emulator or SDK package installation. Commit and push this feature branch; no merge/tag/public release is authorized for this update.
