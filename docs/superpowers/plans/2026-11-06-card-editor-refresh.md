# Lyric card editor refresh

User feedback on the 1.2.0-rc.1 recording authorizes this redesign on the existing feature branch. No merge or publication.

## Design

Pin a rounded-square preview above a scrollable control area; fit non-square exports inside the square without cropping. Keep share/save accessible. Use four sections: Looks, Text, Details and Lines. On short landscape windows, keep preview beside the controls. Replace numerical positions, hex fields and sliders with coordinated palette, type size and alignment choices. Eight templates must change composition, not just color: Afterglow, Signal, Margin, Spotlight, Sleeve, Postcard, Duet and Gallery. Respect original excerpts; reject overflow explicitly.

Bundle Inter, PT Mono, Lora, Oswald, Caveat, Space Grotesk and Playfair Display, with font licenses. Preview font options in their own face. Use only the Singalong icon by default, optional app name; remove kmab and source/provider footers from exported cards while preserving source attribution in saved lyrics. Independently toggle title, artist, album, filename, file location and cover. Unavailable data stays unavailable; location means file folder, never device GPS. Filename and location are off by default. Template selection preserves visibility preferences, selected lyrics and format.

## Implementation and checks

- [x] Add behavior tests for metadata omission, filename/location derivation, preset preference preservation and non-overlapping compositions.
- [x] Separate curated appearance choices and normalized composition geometry from Android rendering. Replace generic bands with eight designed layouts; load bundled font resources for preview/export.
- [x] Keep frozen source/artwork ownership and guarded export. Split editor controls from sheet orchestration; pin preview and use compact section controls with accessible touch targets.
- [x] Update device regressions for sticky preview, toggles, saved-state restoration and all preset/font/format combinations. Compile device tests without installing an emulator.
- [x] Run unit tests, lint, debug/release builds and docs/tool checks; package with the existing key, commit and push the feature branch. Phone acceptance stays pending.
