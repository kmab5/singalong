# Working on Singalong

## Scope and workflow

- Follow the confirmed design and phase roadmap in `docs/superpowers/`.
- Implement only the requested phase. Keep future behavior out of the current UI unless clearly labelled as unavailable or a preview.
- P6 build, lint and unit-test execution are authorized. Use JDK `D:/Code/java/jdk-21.0.12.1` and SDK `D:/Code/android-sdk`. Physical testing is being handled by the user. Do not install emulators, system images or SDK packages.
- Release signing uses the existing ignored `.signing/singalong-release.jks` and `signing.properties`. Never replace the key or commit signing credentials.
- Report implemented source separately from verified behavior. Never mark a milestone passed without its evidence.
- Keep commits focused and descriptive. Exclude machine configuration, build output, signing keys and personal data.

## Code principles

- Keep platform integration, state transitions, persistence and UI responsibilities separate. Prefer small cohesive classes and explicit interfaces over speculative abstractions.
- Keep domain decisions testable without Android. Inject clocks and external services where behavior depends on them.
- Expose immutable state; centralize state transitions and avoid competing sources of truth.
- Scope callbacks, receivers, coroutines and windows to their owner. Pair registration with cleanup, including permission revocation and service failure.
- Preserve original metadata and user-authored content. Never fabricate lyrics, translation, timing, saved songs or verification results.
- Handle missing permissions, metadata and network availability explicitly. Avoid broad catches that hide programming errors.
- Use semantic KMAB theme tokens, scalable text, native touch targets and labelled accessible controls. Prefer clear user-facing language over implementation terms.
- Add behavior-focused regression tests for meaningful logic changes. Do not add tests that merely repeat constants, source text or framework behavior. When execution is deferred, say so.
- Do not expand permissions or introduce paid/key-based services without revisiting the approved scope.
