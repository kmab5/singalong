# Lyric share cards

## Confirmed request and design

Long-press a lyric line to open a draggable bottom context sheet with Share lyric card. The editor starts with that original line and lets users select nearby lines. Preserve song title, artist, album and source credit, plus the Singalong icon/wordmark and lowercase kmab. No lyrics or musical credits are invented or rewritten.

Use the existing dark/purple glass UI, with a live export preview above grouped controls. Templates: Glass, Midnight, Paper, Artwork. Exports: square, portrait, story, landscape PNG. Customize background/accent/text colors, font family, text size/alignment, lyric position, cover visibility/size/position, and background treatment. Allow local image selection for a cover or background. Keep automatic artwork best-effort from the currently matching media session; saved lyrics without artwork offer image selection. No new broad media/storage permission or remote artwork search.

Opening the editor snapshots the recording/version/line and optional artwork. Playback changes must not replace that snapshot. Long-press in a floating reader routes the selection into the activity; overlay dragging remains unchanged. The bottom sheet can be dragged/dismissed, and its editor can expand with scrollable controls. A close action releases the transient draft; it does not modify saved lyrics or translations.

## Architecture and tasks

- Pure `cards/CardModels.kt`: immutable content and style, templates, aspect dimensions, clamping, original-order excerpt selection and validation. Unit regressions cover selection, bounds, templates and no source mutation.
- Android `cards/LyricCardRenderer.kt`: one Canvas-based renderer for preview and exported PNG, word wrapping/RTL, exact original excerpt, metadata/footer branding, cover/background placement and bounded output. Report text that cannot fit rather than silently clipping selected lyrics. Instrumentation tests compiled for all aspect/template combinations and long text.
- Compose `ui/LyricCardSheet.kt`: context sheet, selectable excerpt, live preview and labelled editing controls. Reuse the renderer off the main thread, debounce changing controls, preserve draft controls across rotation, cancel old previews and block stale exports. Photo picker and CreateDocument use existing system contracts.
- `cards/CardImages.kt` / `CardExport.kt`: bound local image decode, narrow cache FileProvider, PNG export and Android Sharesheet read grants. Save uses the document picker. Clear old generated cards with bounded retention; never delete user-selected originals.
- Integration: `LyricsReader`, `FoundationScreen`, `AppContainer`, `AndroidPlaybackObserver`, floating panel/host. Preserve existing tap focus and long-press drag behavior. Exact song matching for player artwork prevents using a new song's cover with an old excerpt.
- Build/check/review with the existing JDK/SDK and signing key; no emulator or SDK install. Prepare a signed feature candidate **1.2.0-rc.1 (16)**. Commit and push the feature branch; do not merge or publish this new feature without a new instruction.

Work can be delegated into independent renderer and editor modules under the subagent-driven-development workflow. Shared interface contracts are in CardModels.kt; agents must not modify one another's files. Final reviews cover requirements and code standards. Device behavior is reported separately from compiled instrumentation coverage.
