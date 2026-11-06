# Card controls follow-up

Implement the requested card refinements on the feature branch; do not merge or publish.

- Keep a fixed gap below the Looks/Text/Details/Lines tabs.
- Separate background artwork from foreground cover visibility.
- Use continuous artwork sizing, wider text sizing, more palettes and soft background treatments.
- Overlay a sized, colored or monochrome logo in any corner, with an optional name. Wrap only affected text rows around its footprint rather than reserve a footer.
- Remove filename and location from card details and exports.
- Let selected source lines independently opt into bold/glow emphasis, preserved through preview, export and draft restoration.
- Suggest `[song-title]-[lyrics-line-no]-singalong.png` when saving. Use one-based source line numbers; compact contiguous selections into ranges and list separate ranges.
- Inspect the supplied crash. The user confirmed the existing rc.1 report is the latest report; do not invent a new rc.2 regression.

Validate domain geometry/naming and existing folder-import regressions, compile native renderer/editor tests, run debug/release checks and package with the existing signing key. Physical rendering and interactions remain for the user to verify.
