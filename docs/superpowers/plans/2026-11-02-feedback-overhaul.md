# Phone feedback overhaul

> Historical design/plan snapshot. Later accepted scope and current release instructions are in the [documentation index](../../README.md). Preserve this record as dated evidence.

The user's November 2 feedback authorizes this cross-screen revision beyond the original phase roadmap. Feedback media stays in the ignored local `feedback/` directory. Execute inline; physical verification belongs to the user.

## Design

Keep KMAB's dark foundation, Inter typography and violet identity. Use translucent, rounded glass panels with thin highlights, stable light foregrounds and generous lyric line spacing. Native background blur belongs to the floating window; use a readable opaque fallback when Android disables cross-window blur. In-app glass uses layered translucent surfaces over a subdued ambient background.

Now is a dedicated lyric stage: compact song header, primary floating action, secondary controls in a sheet. Library/history use compact song rows, clear search/favorites and icon actions. Settings becomes a short category index with focused subpages for access/players, appearance, translation and storage. Preserve editing, imports, backups, saved versions and independent live playback.

The floating surface opens with its final nonzero geometry. A rectangular edge handle (48dp touch width) replaces the minimized square. Tap or swipe inward to reopen; drag vertically to reposition. Expanded lyrics support manual scrolling and resume-follow. Controls explicitly mean Dock, Expand/Compact and Close. Controls and text never inherit the background opacity.

Use shared short easing for navigation/content changes, animated lyric following/highlights and window geometry. Respect Android animation duration scale. Avoid unnecessary animation on 200ms clock ticks. No fabricated playback transport controls: control only actions the app implements.

## Checkpoints

1. Fix initial overlay layout and notification action. Extract/test geometry and edge placement; preserve lock-screen hiding, cleanup, permissions and safe bounds.
2. Fix cached failures and add bounded automatic retries (2/5/10 seconds, honoring provider Retry-After), cancellation and connectivity recovery. Publish originals before translating.
3. Add source-aware lyrics.ovh fallback alongside LRCLIB. Add opt-in MyMemory English translations with local language detection, line alignment, quota guard and saved translations. No API keys or new Android permissions.
4. Rebuild shell, reader, collections, settings and floating controls with shared design/motion. Restyle editor/storage flows without discarding functionality.
5. Run regression tests, lint and signed minified release build using existing JDK/SDK/key. Archive mapping, verify signing/alignment, document actual evidence and outstanding phone checks. Commit/push source and docs only.

## Provider references and limits

- [lyrics.ovh API](https://github.com/NTag/lyrics.ovh): title/artist endpoint returns plain lyrics; upstream identity/timing is not independently confirmed, so mark possible match/manual reading.
- [MyMemory API](https://mymemory.translated.net/doc/spec.php): individual segments up to 500 UTF-8 bytes; explicit source language.
- [MyMemory limits](https://mymemory.translated.net/doc/usagelimits.php): anonymous quota; protect requests locally, also honor server quota responses.
- [MyMemory terms](https://mymemory.translated.net/terms-and-conditions): disclose transmission and storage of submitted text before opting in. Never submit to the contribution endpoint.
- [ML Kit language identification](https://developers.google.com/ml-kit/language/identification/android): bundled language detector, no account or model download needed.

Provider attribution is retained. Public access is not a redistribution license; copies remain personal. More providers can be added only when a usable, free, keyless interface is verified.

Additional investigation: [Lingva's documented public API](https://github.com/thedaviddelta/lingva-translate#public-apis) was tested with a synthetic greeting; its official endpoint returned HTTP 403 from this environment. It is not included as a working provider. This revision adds one usable online translation provider (previously none), rather than claiming unverified provider coverage. MyMemory's live synthetic request returned status 200; this does not establish translation accuracy or uptime.

## Phone acceptance checks (pending)

Open floating lyrics from the app and notification over each selected player; dock/restore by tap and swipe; rotate; lock/unlock; revoke overlay access; check Samsung blur/reduced motion and large system fonts. Seek/pause/change tracks during lookup and translation; test offline/Wi-Fi recovery. Compare originals, saved versions and translated subtext; verify editor/backup flows. Build success cannot establish smooth frame pacing on the phone.
