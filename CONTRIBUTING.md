# Contributing

I welcome bug reports and focused improvements. Please discuss substantial changes in an issue first.

Include the app version, Android version, music player and steps to reproduce a bug. Review diagnostic logs before sharing them; leave out lyrics, listening history, private screenshots and credentials.

## Development

Follow the [build guide](docs/BUILDING.md), [testing guide](docs/TESTING.md) and [design](docs/superpowers/specs/2026-11-01-singalong-design.md).

Keep platform integration, state, persistence and UI separate. Preserve original metadata and personal edits. Add behavior-focused regression tests for meaningful logic changes, and distinguish automated results from phone testing. Use accessible controls and the existing theme.

Changes to permissions, audio capture, paid services or API keys need discussion. Release changes must preserve the certificate in [release-certificate.sha256](release-certificate.sha256). Never commit signing material, private exports, feedback files, build output or machine configuration.

Open a focused pull request explaining the problem, the resulting behavior and what you tested.

## License and conduct

Contributions must be your own or appropriately licensed. Contributions use [GPL v3](LICENSE) with this project’s [ML Kit linking exception](LICENSE-EXCEPTION), unless a file specifies another compatible license. Preserve [third-party notices](NOTICE.md) and follow the [code of conduct](CODE_OF_CONDUCT.md).
