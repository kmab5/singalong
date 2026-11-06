# Roomy cards and stable editor checkpoint

Historical rc.3 results; the [rc.4 checkpoint](card-controls-checkpoint.md) supersedes current controls and APK.

Candidate **1.2.0-rc.3 (18)**, on `feat/lyric-share-cards`; no merge, tag or release publication. Supersedes the [rc.2 editor design](card-refresh-checkpoint.md).

## Source changes and visual evidence

The latest screenshots and two recordings were reviewed locally. The short recording shows the complete sheet moving/stretching as the control list reaches its end. The editor now opens in a full-screen dialog, independently of the initial draggable context sheet. Safe-area insets, a fixed preview/export footer, explicit clipping and disabled stretch overscroll keep scrolling confined to its pane. Four equal-width pill tabs replace the underline strip. Section groups separate headings from options with consistent spacing; wrapping choices have explicit vertical gaps and the bottom control has scroll clearance.

Eight rebuilt compositions use a large artwork/music block and an open lyric region. Art size scales from the output's short edge, so wide formats retain substantial artwork. Title, artist and album are measured together; optional filename/location remain available. Artwork can be above, below, beside, centered or a wide banner. Decorative rules/rings were removed. The standalone Singalong glyph uses the existing mark geometry without a launcher tile, aligned to the footer edge with left/right choice and optional app name.

Restored controls include bounded text size, weight, line spacing, verse position, artwork scale/corners, background treatment and photo shade. Preview/export use the same sanitized immutable style and native renderer. Original excerpts, frozen artwork ownership, metadata visibility, export grants and the rc.2 folder-scan fix remain intact.

A browser composition study with explicit sample text and stand-in artwork was generated and visually inspected under ignored `artifacts/current-feedback/roomy-study.html` and `.png`. It corroborates layout direction only; it is not an Android screenshot or evidence of native rendering/touch behavior.

## Verification

Debug and release each pass **152 unit tests**, with zero failures, errors or skips. Debug/release lint reports **zero errors and 32 warnings** per variant. Debug, instrumentation and optimized release APK assembly passed. Six release-tool tests and local links in 55 Markdown documents pass. Same-key packaging verifies version **1.2.0-rc.3 (18)**, the pinned certificate, matching R8 map and 16 KB ZIP alignment; the APK is approximately 10 MiB. Spec and standards reviews found no actionable defects.

The installable candidate is `artifacts/singalong-1.2.0-rc.3.apk`, with its mapping/build information retained alongside it. Ignored build logs are under `artifacts/current-feedback/roomy-card-*`. Device regressions cover scroll extremes with stable preview/export bounds, larger artwork with typography controls, and existing selection/rotation/export behavior; they were compiled, not executed. No emulator or SDK installation occurred. Native appearance and scroll stability remain for phone acceptance.

The newly supplied crash log identifies rc.1 (16), before the empty-lyrics fix, and does not establish a new rc.2 regression.

## Phone acceptance

1. Install the same-key rc.3 APK over the current app. Long-press a lyric: the initial context menu still drags; entering the editor opens the stable full-screen view. Close and Android Back dismiss correctly.
2. On each tab, scroll repeatedly past both ends and fling. The preview, header and export actions must stay fixed, and the last control must be fully reachable. Repeat landscape and enlarged system text.
3. Inspect all eight presets with actual album art, no artwork, long metadata and several lyric lines. Compare standard/large artwork, corners, font weight, spacing and placement. Export both square and non-square PNGs; check that words/credits are complete or a clear overflow message appears.
4. Switch footer alignment and app name; the mark/name must align to the same content inset. Toggle song details and cover, then rotate or change the playing song: the open draft retains its selected song.
5. Adjust sliders rapidly, change a preset, cancel image selection and share/save. Export must match the completed preview. Repeat the original folder scan to confirm rc.2's fix remains effective on the phone.
