# provider feasibility — p0

Assessed 2026-11-01; updated during P1–P3 implementation. LRCLIB now has a source-level integration; runtime verification remains deferred. Translation is not implemented.

## LRCLIB

**Disposition:** implement LRCLIB for this personal offline app, consistent with its official downloader's stated purpose. The [official LRCGET README](https://github.com/tranxuanthang/lrcget#readme) describes downloading lyrics for an offline music library and identifies LRCGET as the official LRCLIB client. This supports the intended personal offline workflow; it is not a blanket copyright or redistribution license. Broader distribution/rights remain an unresolved release consideration.

Read the current [official API documentation](https://lrclib.net/docs) through its [Markdown endpoint](https://lrclib.net/docs.md). It documents anonymous access, identifying User-Agent headers, sequential requests, pacing and honoring Retry-After. Registration is optional. The client identifies itself as `Singalong/0.1.0 (https://github.com/kmab5/singalong)` without a key.

The response contract includes plain/timed content, recording metadata, duration, an instrumental indicator and provider record ID. Match/search and error handling belong to P2; word-sync fields are outside the first release.

A single read-only smoke request to `/api/search?track_name=Still%20Alive&artist_name=Jonathan%20Coulton` with an identifying User-Agent returned **HTTP 503** in this environment. No returned lyrics were stored or printed. This proves the need for an unavailable path; it does not establish sustained uptime or rule out a transient/network-edge failure.

The documentation inspected did not establish a blanket perpetual storage license for lyric texts. Each saved provider version retains its provider ID, source URL, attribution and the retention note `personal offline copy; no redistribution grant`. Automated fixtures use synthetic lyrics. No license to third-party lyric text is invented or bundled with this project. A successful live fetch and provider availability check still need to be recorded before claiming verified operation.

No Genius/Musixmatch adapter is included. The approved no-key/no-paid constraint remains unchanged.

## online translation

P5 update: official usage/terms/hosting pages were rechecked. No qualifying online adapter was established; the production provider explicitly returns unavailable. Version-bound cache validation and English subtext rendering are implemented for valid saved records. See [P4–P5 checkpoint](p4-p5-checkpoint.md). No source lyrics were sent to a translation service.

**Disposition:** unavailable in P0. MyMemory is a candidate for later evaluation, not a promised integration. Translation remains non-blocking for the personal release.

### MyMemory

The [official usage limits](https://mymemory.translated.net/doc/usagelimits.php) describe a small anonymous daily character allowance. That could cover occasional personal requests but not background translation of a whole library. The [official API specification](https://mymemory.translated.net/doc/spec.php) must be rechecked when implementing segmentation and language handling.

The [service terms](https://mymemory.translated.net/terms-and-conditions) permit API automation within restrictions, prohibit crawling/limit evasion and restrict multi-paragraph translation requests. They also describe retention of submitted segments and third-party processing. Lyrics-version storage permission and aligned line mapping still need an implementation-time disposition. No live song text was submitted to a translation endpoint during P0.

If integrated later: opt-in, disclose remote processing, respect quotas, validate response alignment and cache only under permitted terms. Do not rely on background whole-library translation, email registration or an undisclosed key to satisfy the free/no-account requirement.

### LibreTranslate

[Official documentation](https://docs.libretranslate.com/) distinguishes free self-hosting from managed paid/key-based hosting. Self-hosting would introduce infrastructure outside the approved scope. Public mirrors are not automatically supported production endpoints. No qualifying managed no-key service was established in P0.

## checkpoint impact

- Translation absence is explicitly acceptable and does not block M0.
- LRCLIB source integration follows the documented personal offline use case, but the earlier smoke request failed and no successful live integration run is claimed. The provider gate remains **assessed, not passed**.
- Revisit current provider availability and any broader storage/distribution rights before a release beyond personal use.
