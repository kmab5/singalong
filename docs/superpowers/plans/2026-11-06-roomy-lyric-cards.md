# Roomy lyric cards and stable editor

The rc.2 phone recording shows the modal sheet translating/stretching at the settings scroll boundary, cut-off controls, touching chip rows and sparse cards with disconnected small artwork, credits and logo. User requests authorize another design pass on the feature branch; no merge or publication.

## Design decisions

- Keep the initial context menu as a draggable sheet. Move the editing workspace into a stable full-screen dialog with safe-area insets, pinned square preview, clipped independent scrolling pane and fixed export footer. Disable stretch overscroll within the editor. Use four equal-width pill tabs and consistent 20dp outer, 24dp section, 8dp choice spacing.
- Retain eight presets but replace arbitrary coordinates and decorative rules/circles with coherent music/lyric groups: cover-left header, cover-right header, cover-left footer, cover-right footer, split, centered artwork, photo banner, centered sleeve. Album art occupies roughly a third to two-fifths of the short edge; metadata stays together beside or below it. Without artwork, reclaim the space for the words.
- Use a standalone Singalong glyph at an aligned footer, with optional app name and left/right footer choice. No launcher tile, tiny floating icon or additional byline. User metadata switches remain intact.
- Restore bounded font-size adjustment, weight, line spacing, vertical placement, artwork size/corners, background treatment/photo shade, and footer alignment. Keep palettes, fonts, original lines and metadata choices. No arbitrary overlapping X/Y controls.
- Use measured native text/credits with explicit overflow; render preview/export identically. Add behavior tests for non-overlap, artwork scale, missing metadata/cover reflow, bounded controls and stable scroll extremes. Produce a browser layout study using the same geometry for visual inspection; it is not a native Android screenshot.

## Execution

1. Add regression tests; rework pure composition and curated style models.
2. Implement enlarged art, grouped measured credits, typography controls and a consistent footer mark.
3. Rebuild workspace/controls and add device regressions for scrolling, visibility, state and settings.
4. Review visual study and code, run existing SDK/JDK unit/lint/build checks, update docs and candidate version, preserve signing key, commit and push. Phone acceptance remains pending.

The newly supplied crash file identifies rc.1 (16), before rc.2's empty-lyric scanner fix; preserve the fix and regression tests without treating that older trace as evidence of an rc.2 regression.
