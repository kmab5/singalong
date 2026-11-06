# Card controls checkpoint

Candidate **1.2.0-rc.4 (19)** on `feat/lyric-share-cards`. Supersedes the [rc.3 controls](roomy-card-checkpoint.md); no merge, tag or publication.

## Implemented source

The tabs now have a fixed gap above their scrolling content. Foreground cover visibility no longer changes the album art used by Photo; a chosen background image still takes priority. Artwork sizing is continuous from 20–46% of the short edge, and text sizing spans 20–180 before fitting. Plum, Clay, Fog and Amber join the six existing palettes. Halo, Wash, Dusk and fine Paper grain join Plain, Soft and Photo.

The logo sits on a separate overlay in any corner, with continuous size, colored/monochrome styling and optional app name. The content no longer reserves a footer. Native text layout measures physical row intersections with the logo footprint, including padding, and only indents intersecting rows. Reflow recomputes those intersections; cyclic measurements try simpler line breaking and then bounded font reduction rather than retain stale indents. Artwork can sit behind the mark. Lyrics and metadata remain separate from foreground artwork.

Selected source lines have independent right-side emphasis toggles for bold text with a soft glow. Emphasis is draft state, included in preview/export equality checks and saved-state restoration; it does not change original lyrics. Removed audio filename/folder controls and collection for cards. Older saved drafts tolerate the removed fields.

Save PNG suggests the sanitized song title and one-based source line numbers: `Song-title-3-singalong.png`, `Song-title-3-5-singalong.png`, or `Song-title-3-5,8-singalong.png`. Very large disjoint selections use the first line plus a selected-line count to stay within filename limits. Original lyric text is never put into the filename. The document picker allows renaming.

## Crash evidence

The report in `feedback/singalong-crash (1).txt` identifies **1.2.0-rc.1 (16)** and R8 ID `bed008294f0f0839033256c72d758d1850862d876a98c9eddfe640277771726c`. The user confirmed this is the latest report and corrected the rc.2 description. It predates the rc.2 empty-provider-lyrics fix. An exact matching rc.1 map is unavailable locally, so this report is not independently symbolicated or proof of a new regression. Existing folder-import tests retain the reproduced empty-candidate assertion fix and continuation to the next file. Phone retesting remains necessary; no additional crash fix is claimed in rc.4.

## Verification

Debug and release each pass **157 unit tests**, with zero failures, errors or skips. Both lint reports have **zero errors and 32 warnings**. Debug, instrumentation and optimized release APK assembly passed. Six release-tool tests and links in 57 Markdown documents pass. Spec and standards review identified stale indents and old-draft compatibility during implementation; both were corrected and focused rereviews found no outstanding findings.

Same-key packaging verifies version **1.2.0-rc.4 (19)**, the pinned certificate, matching R8 map and 16 KB ZIP alignment. The signed APK is 10,478,450 bytes, at `artifacts/singalong-1.2.0-rc.4.apk`; its map/build information remain alongside it. The previous rc.3 APK is superseded, with its map retained for diagnosis. Logs are ignored under `artifacts/current-feedback/card-controls-*`.

Native regressions for photo-background persistence, emphasis/restoration and wrapping-heavy corner layouts are compiled only; no emulator or SDK package was installed. These checks establish source/build behavior, not phone acceptance or proof that the historical crash cannot recur.

## Phone checks

1. Toggle the foreground cover off while Photo uses album art; the background must stay. Pick a custom background and confirm it takes priority.
2. Move the logo through every corner, adjust its size, toggle name and color style, and use a bottom-aligned multi-line verse. Only nearby text should wrap; there must be no reserved footer.
3. Try art/text size extremes, all backgrounds, light/dark palettes and non-square formats. Long excerpts must show a fitting error rather than lose words.
4. Select disjoint lines, emphasize one, rotate and export. Preview and PNG must agree, and saved lyrics must stay unchanged. Check the suggested PNG name against original source line numbers.
5. Scroll each control pane to both ends and confirm the fixed tab gap, preview and export actions remain stable. Repeat the original folder scan with a not-found file followed by a known song.
