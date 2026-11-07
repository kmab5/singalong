# Copyright and third-party notices

Copyright © 2026 Samuel (kmab). Original Singalong source code, documentation, site code and original artwork are licensed under [GNU GPL version 3](LICENSE), with the narrowly scoped additional permission in [LICENSE-EXCEPTION](LICENSE-EXCEPTION). This is GPL-3.0-only plus that permission. It does not relicense third-party works or grant rights in downloaded lyrics/translations.

## Bundled dependencies

| Component | License / terms |
| --- | --- |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | Apache License 2.0 |
| AndroidX / Jetpack Compose, Room, DataStore, Core, Activity, Lifecycle | Apache License 2.0 |
| OkHttp and Okio | Apache License 2.0 |
| SnakeYAML | Apache License 2.0; [bundled license](app/src/main/assets/licenses/Apache-2.0.txt). Bounded safe loading of LRCLIB Lyricsfile documents |
| ML Kit Language Identification and required Google SDK components | [ML Kit terms](https://developers.google.com/ml-kit/terms) and the applicable Google SDK terms; covered by the project's linking permission, not relicensed as GPL |
| Transitive utility libraries, including Android annotations and transport/support code | Their original package notices and licenses remain applicable |
| Inter | SIL Open Font License 1.1; [bundled notice](docs/assets/fonts/Inter-OFL.txt) |
| PT Mono | SIL Open Font License 1.1; [bundled notice](docs/assets/fonts/PTMono-OFL.txt) |
| Lora | SIL Open Font License 1.1; [app notice](app/src/main/assets/licenses/lora-OFL.txt), [website notice](docs/assets/fonts/Lora-OFL.txt) |
| Oswald | SIL Open Font License 1.1; [bundled notice](app/src/main/assets/licenses/oswald-OFL.txt) |
| Caveat | SIL Open Font License 1.1; [bundled notice](app/src/main/assets/licenses/caveat-OFL.txt) |
| Space Grotesk | SIL Open Font License 1.1; [bundled notice](app/src/main/assets/licenses/spacegrotesk-OFL.txt) |
| Playfair Display | SIL Open Font License 1.1; [bundled notice](app/src/main/assets/licenses/playfairdisplay-OFL.txt) |

Dependency versions are pinned in `gradle/libs.versions.toml`; Gradle resolves their POM/license metadata. Library names and marks belong to their respective owners. The application is independent of music players, lyric providers and Google.

## Retrieved content

LRCLIB, lyrics.ovh and MyMemory provide content under their own terms and applicable rights. Software licensing does not grant a right to redistribute a downloaded lyric collection. Public release attachments contain APKs only; build diagnostics remain in CI artifacts. Neither includes a user's database, lyrics, backups or feedback files. Font license texts accompany the downloadable website font assets.
