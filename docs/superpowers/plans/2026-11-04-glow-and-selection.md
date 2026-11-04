# Glow motion and timed selection follow-up

The user's new recording was reviewed locally, including sampled frames around line transitions and the lyrics-only panel. It remains ignored personal feedback. The later clarification cancels all drag alterations: keep the existing long-press gesture for minimal lyrics and existing edge-handle behavior.

## Implementation

- Rename the Now settings options to Gentle and Glow without changing persisted enum identifiers.
- Reserve line wrapping and height at the larger configured text size. Animate drawing transforms instead of changing font metrics every frame. Keep one renderer through line handovers, settle outgoing highlights over time and scroll visible lines with a consistent eased duration. Preserve outgoing version content during crossfades.
- Glow uses brighter highlights, a slow small word drift/lift and slight enlargement of current/read words. Preserve text shaping, whitespace and accessible full-line text. Respect pause, seek, Android animation scale and approximate word-timing labels.
- Rank eligible matches by actual parsed timing first, then closest known provider duration, then confidence and deterministic identity. Preserve original matching exclusions and explicit/personal selections. Persist optional provider duration with the lyric JSON so the version list uses the same ranking.
- Real timestamps automatically enable following when a new version loads, including uncertain matches, while retaining the uncertainty label. Refreshing the same version must not undo a user's manual following pause. Untimed experimental sync remains opt-in.
- Explore translation alternatives in a sourced document; do not introduce providers or credentials in this update.
- Remove superseded ignored APKs, logs, screen captures and old mapping archives, plus the stale screenshot list. Keep current feedback, current build evidence, current APK/mapping and the existing signing identity. Historical tracked documentation and source stay intact.

## Verification

The new selection tests were run before the implementation and all three failed on ranking or timed-sync capability. Run them with the existing suite after the fix. Add motion continuity and stable-layout tests; device UI tests can be compiled only in this workspace. Build, lint, test and sign the next candidate with the existing JDK/SDK/key, commit and push the current feature branch without merging or publishing.
