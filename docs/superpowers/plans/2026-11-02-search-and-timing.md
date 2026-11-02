# Smart lyric search and optional timing editor

> Historical design/plan snapshot. Later accepted scope and current release instructions are in the [documentation index](../../README.md). Preserve this record as dated evidence.

## Scope

Implement filename-aware lyric lookup and opt-in line timing on the existing feature branch. Audio recognition is deferred to a future version at the user's request. Do not add audio capture, permissions or service credentials.

The user must confirm phone testing before merging to main, preparing v1.0.0, adding GPL v3/community documents, auditing all documentation, or implementing tagged GitHub Releases and the website APK link. Those tasks remain pending.

## Design

Search keeps original recording metadata and identity. A pure query planner recognizes common file extensions, track prefixes, underscores, explicit artist/title separators, and the unambiguous `Artist - Album - 03 - Title` layout. Known artist metadata takes precedence over guesses. Album is inferred only from that explicit layout. Preserve live/remix/acoustic qualifiers. Ambiguous titles without an artist remain available for manual search rather than pretending to identify audio.

Bound search to at most three distinct queries (currently at most two). Prefer recognized filename hints, then the original metadata; without recognized hints, search the supplied metadata unchanged. Stop on a successful match or provider failure/cooldown. Respect cancellation and network policy between queries. Match provider results strictly against each query and mark inferred matches uncertain. Apply the same search to background collection and manual lookup. Show a reviewable filename suggestion in search details.

The default editor stays a text field. An explicit Edit timing switch reveals a compact timing panel, with a selectable lyric line, timestamp input, 100 ms adjustments, clear timing, and a stamp-and-next button using the matching song's playback position. No estimated timing is used as a real timestamp. Hiding controls retains the draft. Draft text and timings survive rotation in the existing ViewModel. Adding/removing lines requires explicitly creating an untimed version so existing timestamps cannot silently attach to different lyrics.

Allow partially timed versions; untimed lines retain no timestamp. Validate finite, nonnegative timestamps in lyric order (equal times allowed). Saving creates a new personal version, preserving the source. Keep the existing glass surfaces, type scale, lilac accents, accessible labels and 48 dp touch targets. Animate timing-panel expansion; avoid a complex waveform editor.

## Work and evidence

- [x] Add behavior tests for filename inference, wrong artist/version rejection, metadata preservation, request bounds, cooldown and cancellation.
- [x] Add pure query planner and search coordinator; integrate repository/manual lookup and reviewable suggestions.
- [x] Add timing parse/edit/session tests, including partial timing, line-count changes, playback mismatch and immutable originals.
- [x] Add timing domain model and opt-in Compose controls; retain simple text editing by default.
- [x] Run debug/release unit tests, lint and APK assembly with existing JDK/SDK. Compile instrumentation tests without installing an emulator.
- [x] Produce a signed pre-1.0 test APK using the existing key; record evidence and phone checks still pending.

Delivery: commit and push focused changes on the feature branch. No merge or release tag.

Files: new `lyrics/LyricSearch.kt`, `lyrics/SearchQueries.kt`, `editor/TimingDraft.kt`, `ui/TimingEditor.kt`; update `LyricsRepository.kt`, `AppContainer.kt`, `LyricsEditor.kt`, `LibraryTools.kt`, and behavior tests under the corresponding test packages.

Evidence: [0.3.3 checkpoint](../../validation/search-timing-checkpoint.md). Phone acceptance remains open.
