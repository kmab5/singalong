# Translate fallback and main integration

The maintainer requested a fix for Google Translate opening without text, a clipboard fallback, current documentation/site cleanup, and a merge to main. This authorizes the merge and push; a public release tag is not part of this task.

## Implementation

1. Keep the correctly encoded full-text English-target URL. Add a small testable handoff that copies the complete original text before opening another app, because there is no acknowledgement of input acceptance from Google. Prefer Android's browser selector and fall back to the normal URL handler if unavailable. Handle clipboard/launcher failures explicitly, without truncation or pretending external text was received. Oversized URLs still allow copying and opening the English-target page; extremely large clipboard transactions are rejected with an export instruction.
2. Add regression tests for copy-before-open, a receiver that discards URL input, missing browser, clipboard failure, Unicode/blank lines, and overlong URLs. Compile Android intent/clipboard tests without installing devices or SDKs.
3. Preserve the website's current kmab visual style. Add a clearly labelled 1.1 feature section covering word timing and its switch, folder scanning/sorting, responsive controls and external translation with clipboard fallback. Keep downloads tied to the latest actually published release; do not advertise 1.1 as published before a tag exists. Check responsive layouts, keyboard access, reduced motion and download fallback using the existing browser harness.
4. Prepare version **1.1.0 (15)**, refresh README, release notes, privacy/release/site docs and current verification evidence. Preserve historical checkpoints as history. Run Android unit/lint/build checks, release packaging/certificate checks, documentation and release-tool checks. Remove only superseded generated APKs after the replacement is verified.
5. Review, commit, fetch current remote state, merge the feature branch into main and push. Verify the remote main commit and report local tests separately from phone verification and hosted CI results. Keep the existing signing key and keep all feedback/build output ignored.

The website task can run independently of the Android handoff task under the subagent-driven-development workflow. No audio recognition, new translation provider, new permission, key regeneration or unrelated dependency upgrade is included.
