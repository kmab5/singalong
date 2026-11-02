# Retry stability and the singalong landing page

User-authorized follow-up: Musicolet song changes and missing-song retries can terminate the app, with or without the overlay. A log supplied during investigation confirms `NetworkOnMainThreadException` while canceling a pending request. Its map ID matches the saved 0.3.1 mapping and resolves to `LrclibClient.search` closing the response on main. Keep verification of the fix on the phone separate from the regression tests.

## Android

1. Reproduce response cleanup on the caller thread with guarded response bodies. Both HTTP clients currently return live responses to a main-dispatcher coroutine; success, early error and cancellation cleanup can therefore perform socket I/O on that thread.
2. Consume and close responses entirely on OkHttp's callback worker, return parsed values only, retain cancellation and provider-specific failure classification. Keep mutable cooldown state under the owning coroutine's mutex. Test unread 404/429/503 bodies, ordinary JSON and cancellation while a body is blocked, followed by the next song.
3. Keep a bounded private report of future uncaught Java/Kotlin crashes, containing exception types/code locations and app/platform versions, never exception messages or music metadata. Delegate to Android's existing crash handler. Export only through an explicit About action; do not upload automatically.
4. Use lowercase kmab in all app strings/accessibility labels and remove the kmab-brand action. Bump to 0.3.2 (6), retain signing continuity and run the authorized Android checks.
5. Label each version Timed or Untimed in both selectors and the editor's selected-version button, based on actual stored timestamps. Experimental estimated following does not change that label.

## Landing page

Audience: an Android listener who wants lyrics without leaving their player. Job: explain the app, demonstrate floating/minimal behavior and lead to the source/setup and feedback. No public APK release is currently promised; don't invent a download or store listing.

Static HTML/CSS/JavaScript directly in `docs/`, with relative asset paths and `.nojekyll`, suitable for branch-based GitHub Pages. No site framework, external font requests, analytics or private feedback media. Existing technical documents remain intact.

Use the established palette: ink #0E0E11, glass #17171B, raised #1E1E24, paper #EDEBE4, muted #AAA6B5, purple #B24BFF and readable lilac #D49AFF. Inter for body/display, PT Mono for small utility labels. The signature is a working lyric-window demo over an abstract record-like background. Switch between floating/minimal/docked views, step through clearly labelled original sample lines, and reveal English subtext. No fake live playback or artist attribution. Keep the surrounding page quiet: oversized two-line headline, useful feature rows, three actual setup steps, privacy/limits and downloadable assets.

The ui-ux-pro-max query's generic gaming typography/green CTA is deliberately rejected in favor of the already accepted app typography and purple cue. Apply its keyboard, contrast, target-size, responsive and reduced-motion guidance. Verify desktop/mobile screenshots, demo interactions, asset URLs, no-JS readability and reduced motion in the installed Chrome using Playwright.

Export the existing Android icon geometry to SVG and PNG. Include a matching outlined wordmark. Hosted copies live in `docs/assets/`; reusable copies live in the explicitly requested `public/docs/`. Bundle font license notices. Document GitHub Pages source configuration without claiming deployment unless independently confirmed.

Commit Android stability and site work in focused commits; push both. Keep screenshots, logs, keys and APK/mapping artifacts ignored.
