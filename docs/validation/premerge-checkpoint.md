# Pre-merge release candidate

Current version source: `version.properties`. Merge, production tag and public release remain unperformed and require the user's later instruction.

## Source changes and diagnosis

All four supplied NetworkOnMainThreadException traces contain the 0.3.1 R8 map ID `c3d42ed4476e4fabc4cdb17f1cae83af8f5c84e7d57b512bcf594100d6110226`. Archived 0.3.2 and 0.3.3 mappings differ. The user subsequently confirmed About shows 0.3.3 (7) and said the original crash appears fixed. No further networking rewrite was justified by those older traces. Existing guarded cleanup/cancellation tests remain in the suite.

New uncaught Java/Kotlin reports prompt review/export on the next launch. Acknowledging a report prevents repeated prompts for the same report; a later crash is eligible again. The universal feedback form does not accept automatic log attachments through its URL contract, so the flow saves a reviewed log and opens the form for the user to attach and submit it. No automatic report upload is claimed.

Floating options are bottom icons; narrow widths scroll the row. Tapping full lyrics/title opens Now. Lookup failures dock after eight seconds without active retry, and automatic docking retains reader demand so successful lyrics can restore the panel. Manual docking/close remains respected. Unknown playback state is not treated as a pause. Transient window detach/token errors retain requested visibility and are reconciled by the owning service; real permission loss closes the window. Lock-screen hiding remains enforced. Android can intentionally suppress overlays on sensitive screens, including some Settings screens; this is not bypassed.

Now has independent size, alignment, spacing, inactive-line dimming, None/Fade/Gentle focus animation, and tap-to-focus controls. These do not alter saved lyrics or playback timing.

Pre-merge work includes version/tag validation, signed CI artifacts, tag-driven releases guarded by main ancestry, a website release link, GPL v3 with the user-approved ML Kit linking exception, conduct/contributing/security/privacy/notices, and a documentation index distinguishing historical evidence from current guidance.

## Evidence

Final automated verification and remote signing results are recorded here after execution. Device interaction and acceptance remain with the user; no emulator or SDK packages are installed.

## Phone checks

1. Confirm the candidate version in About after updating the existing signed installation.
2. Try floating icons at narrow and normal widths, tap expanded lyrics to open Now, and verify scrolling still works.
3. Use a missing song, wait for automatic docking, then play a known song and check automatic return. Manual docking/close must remain respected. Retry before timeout and check that the panel stays available during the request.
4. Enter and leave Android Settings/permission screens, lock/unlock, rotate, and switch music apps. Secure-screen suppression is expected; the panel should return on ordinary screens if still requested.
5. Change Now alignment/animation/size/spacing and try tap-to-focus/Follow lyrics. Confirm these settings persist independently of floating sizes.
6. If a new crash occurs, reopen and review the prompt, save the log, attach it to feedback, and verify the same report does not repeatedly prompt after dismissal. Native crashes/OS kills are not guaranteed by this handler.
