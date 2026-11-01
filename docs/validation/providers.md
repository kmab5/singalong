# provider feasibility — p0

Assessed 2026-11-01. This is a feasibility record, not a claim that a provider is integrated. The P0 app has no network permission or lyrics/translation client.

## LRCLIB

**Disposition:** documented no-key access is a technical candidate for P2. Permanent-retention rights remain unverified; do not interpret the service's software license or open API as a license to all underlying lyrics. Provider retention is an explicit unresolved release gate, not silently approved.

Read the current [official API documentation](https://lrclib.net/docs) through its [Markdown endpoint](https://lrclib.net/docs.md). It documents anonymous access, identifying User-Agent headers, sequential requests, pacing and honoring Retry-After. Registration is optional. We can identify this client as `Singalong/0.0.1 (https://github.com/kmab5/singalong)` without a key.

The response contract includes plain/timed content, recording metadata, duration, an instrumental indicator and provider record ID. Match/search and error handling belong to P2; word-sync fields are outside the first release.

A single read-only smoke request to `/api/search?track_name=Still%20Alive&artist_name=Jonathan%20Coulton` with an identifying User-Agent returned **HTTP 503** in this environment. No returned lyrics were stored or printed. This proves the need for an unavailable path; it does not establish sustained uptime or rule out a transient/network-edge failure.

The documentation inspected did not establish a blanket perpetual storage license for lyric texts. LRCLIB offers database downloads and its companion downloader, but these are not proof of underlying copyright permissions. Before permanent automatic collection is enabled, establish a permitted retention basis or keep that integration gated. Imported/user-authored content provides an independent path for development/testing. Use synthetic lyrics in automated fixtures.

No Genius/Musixmatch adapter is included. The approved no-key/no-paid constraint remains unchanged.

## online translation

**Disposition:** unavailable in P0. MyMemory is a candidate for later evaluation, not a promised integration. Translation remains non-blocking for the personal release.

### MyMemory

The [official usage limits](https://mymemory.translated.net/doc/usagelimits.php) describe a small anonymous daily character allowance. That could cover occasional personal requests but not background translation of a whole library. The [official API specification](https://mymemory.translated.net/doc/spec.php) must be rechecked when implementing segmentation and language handling.

The [service terms](https://mymemory.translated.net/terms-and-conditions) permit API automation within restrictions, prohibit crawling/limit evasion and restrict multi-paragraph translation requests. They also describe retention of submitted segments and third-party processing. Lyrics-version storage permission and aligned line mapping still need an implementation-time disposition. No live song text was submitted to a translation endpoint during P0.

If integrated later: opt-in, disclose remote processing, respect quotas, validate response alignment and cache only under permitted terms. Do not rely on background whole-library translation, email registration or an undisclosed key to satisfy the free/no-account requirement.

### LibreTranslate

[Official documentation](https://docs.libretranslate.com/) distinguishes free self-hosting from managed paid/key-based hosting. Self-hosting would introduce infrastructure outside the approved scope. Public mirrors are not automatically supported production endpoints. No qualifying managed no-key service was established in P0.

## checkpoint impact

- Translation absence is explicitly acceptable and does not block M0.
- LRCLIB is technically documented, but the smoke test failed transiently and retention is unresolved. Therefore the provider gate is **assessed, not passed**.
- Android foundation code can be completed independently. Before a later phase claims automatic permanent lyrics collection, revisit this gate with evidence.
