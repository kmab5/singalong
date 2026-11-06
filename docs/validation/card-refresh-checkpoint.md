# Card refresh and folder-scan checkpoint

Historical rc.2 results; the [rc.3 checkpoint](roomy-card-checkpoint.md) supersedes the card/editor design and current APK.

Candidate **1.2.0-rc.2 (17)** on `feat/lyric-share-cards`. No merge, tag or release publication. This supersedes the [first card checkpoint](lyric-card-checkpoint.md).

## Implemented source

- Reviewed the user's 70-second screen recording locally. Replaced the scrolling preview and large settings form with a pinned rounded-square viewport, independent scrolling controls, fixed export actions and Looks/Text/Details/Lines sections. Short landscape windows place preview beside controls. Non-square output is fitted without cropping.
- Eight compositions reserve separate regions for lyrics, title, remaining metadata, artwork and logo: Afterglow, Signal, Margin, Spotlight, Sleeve, Postcard, Duet and Gallery. Removed black credit/lyric boxes, source/provider footer and kmab byline from exported images. The Singalong icon stays; app name is optional and off by default.
- Six coordinated palettes, seven bundled named fonts, three text sizes, alignment and four formats replace hex fields and geometry/shading sliders. Lora, Oswald, Caveat, Space Grotesk and Playfair Display come from the Google Fonts repository with OFL notices included in the APK; Inter/PT Mono were already bundled. Preview and PNG use the same native renderer and font resources.
- Title, artist, album, filename, file location, cover and app name are separate switches. Unavailable metadata is labelled and disabled. Filename/location are off by default; location is a known file folder, never GPS. A local media file URI can supply file metadata only when the session still matches the selected song. Opaque content identifiers and remote URLs are not exported as filenames/locations. Stored lyric attribution remains unchanged.
- Choosing a preset preserves excerpt, format and metadata/cover visibility. Local custom backgrounds reset to the chosen preset. Export still uses frozen song/artwork and complete selected original lines, with explicit overflow errors and narrow read grants.

## Folder crash diagnosis

The supplied redacted log identifies 1.1.0 (15), `IllegalArgumentException`, and R8 ID beginning `faa394ac`. Its original mapping was unavailable locally. Rebuilding main 8cf08c1 produced ID beginning `546af4b5`; retracing against that reconstruction points to `LibraryRepository.saveFolderMatch` line 57, but this is **not an exact-build mapping verification**.

Independent failing regressions reproduced the save assertion: timestamp-only LRC (or blank Lyricsfile rows) could produce a nonempty version with no actual words. Folder save explicitly requires actual lyrics. The fix uses real plain/LRC fallback when a higher-priority timed response is empty, rejects wordless versions at matching, and filters empty versions again before folder save. Empty candidates count as not found and the next file is processed. Blank instrumental intervals inside otherwise valid lyrics remain intact. Recording-ID/database assertions remain enforced; no broad exception swallowing was added.

## Verification

Debug and release each pass **149 unit tests**, with zero failures, errors or skips. Debug/release lint reports **zero errors and 32 warnings** per variant. Debug, instrumentation and optimized release APK assembly passed. Six release-tool tests and links in 53 Markdown documents pass. Existing-key packaging verified version 1.2.0-rc.2 (17), the pinned certificate, matching R8 mapping and 16 KB ZIP alignment. The candidate is approximately 10 MiB.

Regression tests failed before the empty-lyrics fix and passed afterward. Review also found eager line-row composition, off-center landscape Gallery artwork and opaque player identifiers being treated as filenames. These were corrected with a bounded lazy line picker and geometry/URI regressions; focused rereviews found no further actionable issues. Device tests for the pinned preview, long line picker, metadata controls and state restoration are compiled only; no emulator, SDK package installation or native visual acceptance is claimed.

Ignored logs are under `artifacts/current-feedback/card-refresh-*` and `folder-crash-red.log`. The installable candidate is `artifacts/singalong-1.2.0-rc.2.apk`; its mapping/build information are retained beside it and in `artifacts/release/`. This records local verification, not hosted CI or phone acceptance.

## Phone checks

1. Install the same-key 1.2.0-rc.2 APK. Long-press a lyric, enter the editor and scroll every section: the square preview and export buttons must remain visible. Rotate and repeat with enlarged system text.
2. Compare all eight presets with and without a cover, in all formats. Check seven font previews, non-Latin text, long excerpts and metadata. Compare saved PNG with preview and confirm complete text or an explicit overflow message.
3. Toggle each available detail and app name. Confirm only the icon is mandatory, no source/byline appears, and format/visibility preferences survive preset changes and rotation. Check missing metadata, cancelled image pickers and export during rapid edits.
4. Repeat the folder scan that crashed, including a not-found song followed by a known song. Confirm progress continues, no blank lyric records are saved, and repeated scans still skip duplicates.

The privacy note and changelog describe the metadata inclusion changes. No feedback recording/log, private file path or signing credential belongs in Git.
