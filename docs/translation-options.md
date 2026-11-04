# Translation options — 2026-11-04

Research only. No provider, SDK, permission, API key or billing change is implemented in this candidate.

The 1.1 source separately adds an explicit **Open in Google Translate** action. It copies the original lyrics, attempts to prefill a browser with English selected, and keeps manual paste available if the receiver ignores the input. This is not a new API provider and does not import translated results. The provider alternatives below remain research only.

## Why the current translation can stop early

The current MyMemory adapter translates two unique lines at a time and returns on the first failed batch. A line over 500 UTF-8 bytes is rejected. The local cache also reserves against 5,000 characters per day, and completed lines remain visible if a later request fails. Language detection can reject a song before requests start. These are code paths that can explain partial results; no device network trace was supplied to identify which caused each observed song's failure.

MyMemory documents a 500-byte segment limit and 5,000 characters/day for anonymous use. Supplying a contact email increases the allowance to 50,000/day, but that alone would not improve lyric interpretation or fix early termination. [Request specification](https://mymemory.translated.net/doc/spec.php), [usage limits](https://mymemory.translated.net/doc/usagelimits.php).

## Candidates

| Option | Practical fit | Trade-off |
| --- | --- | --- |
| ML Kit on-device translation | No hosted translation requests after downloading models; over 50 languages, models around 30 MB. A useful first prototype for reliable completion without operating a server. | Google describes it as suitable for casual/simple translation. Lyric quality needs testing, particularly idioms and mixed-language songs. Models add device storage. [Overview](https://developers.google.com/ml-kit/language/translation), [Android integration](https://developers.google.com/ml-kit/language/translation/android). |
| Google Cloud Translation | Official online API with a monthly allowance: NMT's first 500,000 characters receive a $10 credit. | Requires a billing-enabled project and authentication. Use a controlled backend or user-owned credentials, not a maintainer secret bundled in the APK. Further usage can incur charges. [Pricing](https://cloud.google.com/products/translate/pricing), [setup](https://docs.cloud.google.com/translate/docs/setup). |
| DeepL API | Official online API with account authentication; worth comparing against Google using the actual target languages. | Requires a key and quota handling. Current support documentation says new API Developer accounts receive 1,000,000 characters total, non-renewing; the older 500,000/month API Free plan is no longer available for new purchase. Do not design around an assumed permanent free tier. [Plans](https://support.deepl.com/hc/en-us/articles/360021200939-DeepL-API-plans), [authentication](https://developers.deepl.com/docs/getting-started/auth). |
| Self-hosted LibreTranslate | Open-source translation service powered by Argos Translate; can run without mandatory API keys on a server we control. | Needs hosting and maintenance. Its official managed service requires a purchased key. Language/model quality needs evaluation; self-hosting does not guarantee better lyric translations. [Documentation](https://docs.libretranslate.com/), [API usage](https://docs.libretranslate.com/guides/api_usage/). |
| deep-translator / unofficial Google wrappers | Python wrappers around other translation sources, not a separate translation engine. | The Google adapter parses translated text from HTML and handles rate limits. It is not the official Cloud API and would add a Python/backend dependency or require a separate Kotlin implementation. Web-page changes can break it; not the recommended reliability fix. [Adapter source](https://github.com/nidhaloff/deep-translator/blob/master/deep_translator/google.py). |

## Recommended next decision

For a personal, keyless app without a server, prototype **ML Kit** and assess representative lyrics before committing to its quality. The project's current linking permission covers ML Kit **language identification only**; extending it to the translation SDK would need an explicit scope/license decision. Downloads, model deletion and network preferences would also need UI.

For online translation, compare **Google Cloud and DeepL** with a small agreed sample before choosing an account/backend arrangement. No quality winner is established by this research. LibreTranslate is the option if running our own open-source service is preferable.

Regardless of provider, the next implementation should retain line IDs, translate every eligible line, distinguish partial from complete results, retry individual failed segments without losing completed ones, expose quota/error reasons, allow language correction, and preserve original lyrics. Context-aware requests must still return a validated line mapping; a translated paragraph must not be arbitrarily split to fabricate line alignment.
