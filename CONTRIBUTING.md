# Contributing to singalong

Discuss a substantial change in an issue before implementing it. Bug reports should include the app version, Android version, music player, reproduction steps and a reviewed diagnostic log where available. Avoid uploading lyrics, listening history, personal screenshots or signing material.

## Development

Use JDK 21, Android SDK platform 36 and build-tools 36.0.0. Minimum runtime is Android 12. Read [AGENTS.md](AGENTS.md), the [current docs](docs/README.md), and [build instructions](README.md). No emulator is required for compilation or unit tests. Instrumentation tests need a separately authorized device.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/check_docs.py
```

Keep platform integration, immutable state, persistence and UI separate. Preserve original metadata and user-authored lyrics. Add behavior-focused tests for state transitions, cancellation, timing and persistence; document device checks honestly. Honor the existing theme, accessible touch targets and Android's animation preferences. Do not introduce microphone capture, new permissions, paid APIs or API keys without discussion.

Open focused pull requests describing the problem, resulting behavior and actual verification. Changes involving signing/release automation must preserve the certificate in `release-certificate.sha256`. Ordinary pull-request checks do not receive signing secrets. Do not regenerate or submit a keystore. Do not commit `feedback/`, build output, private exports, machine paths/configuration or credentials.

## Licensing and conduct

Contributions must be your own or appropriately licensed. By submitting code, you agree to distribute it under [GNU GPL v3](LICENSE), including this project's [ML Kit linking permission](LICENSE-EXCEPTION), unless a file explicitly specifies a different compatible license. Preserve third-party notices. See [NOTICE.md](NOTICE.md) and the [code of conduct](CODE_OF_CONDUCT.md).

Release maintainers should follow [the release guide](docs/RELEASING.md). A passing CI run does not authorize a merge or release; those remain maintainer decisions.
