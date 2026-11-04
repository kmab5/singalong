# Word tracking control and external translation

The user confirmed the rc.4 word-sync feature on their phone. Before merging 1.1.0, implement two additions and prepare another signed candidate for their approval.

1. Add an enabled-by-default, persistent **Word tracking** switch under Settings → Now. Turning it off bypasses word timelines, segmentation, masks, frame-clock sampling, drift and estimated word sweeps. Continue line following and line transitions; preserve timestamps and the selected animation so enabling the switch restores word behavior.
2. Add **Open in Google Translate** to Lyric options for the selected original version. Send all original text, including blank lines and Unicode, with automatic source-language detection and English output. This is an explicit external handoff, not an automatic translation provider or saved translation update. Disable oversized browser links without truncating text, and explain Google's 5,000-character input limit for longer songs. See [Google's text translation help](https://support.google.com/translate/answer/6142478?hl=en).
3. Add regression coverage for disabling/re-enabling progress while line following continues, full-text query encoding and oversized input. Compile the renderer regression for stopping word-clock reads and movement; device execution remains with the user.
4. Run unit tests, lint, debug/release assembly and release packaging against the existing certificate. Bump the candidate to **1.1.0-rc.5 (14)**, update docs, commit and push the feature branch. Do not merge, tag or publish until the user confirms this candidate.

No microphone, new permission, paid API, signing-key change or audio recognition is involved.
