# Lyrics and translation providers

Singalong uses keyless services and preserves provider attribution. Network availability, song coverage and translation quality aren’t guaranteed.

| Provider | Role | Limits |
| --- | --- | --- |
| LRCLIB | Primary plain, line-timed and supplied word-timed lyrics | Match quality depends on recording metadata; unsupported or malformed timing falls back safely |
| lyrics.ovh | Plain-lyrics fallback | No independent duration/timing evidence; results remain possible matches |
| MyMemory | Optional English translation | Opt-in, remote text retention, bounded segments and a local daily budget |
| ML Kit | On-device language identification | SDK terms and usage/performance metrics apply |

Lookup uses saved versions first, preserves explicit selections and distinguishes uncertainty, missing lyrics and network failures. Provider requests identify the app, respect pacing/cooldowns and avoid replacing the current song with stale responses.

MyMemory translates opened lyrics, not the background library. The adapter limits each segment to 500 UTF-8 bytes and reserves against a 5,000-character daily budget. Completed lines remain available if later work fails. The separate Google Translate action copies original text and opens an external page; it doesn’t import results.

Downloaded lyrics and translations are personal offline copies. API access isn’t a blanket redistribution license, and the app’s GPL license doesn’t cover third-party lyric content. See [privacy](../PRIVACY.md) and [notices](../NOTICE.md).

Reference documentation: [LRCLIB API](https://lrclib.net/docs), [lyrics.ovh](https://github.com/NTag/lyrics.ovh#api), [MyMemory API](https://mymemory.translated.net/doc/spec.php), [MyMemory terms](https://mymemory.translated.net/terms-and-conditions), [ML Kit terms](https://developers.google.com/ml-kit/terms).

Audio recognition, unofficial scraping, paid/key-based APIs and alternative translation SDKs are outside the current scope. The ML Kit linking exception covers language identification; adding another Google SDK needs a separate licensing decision.
