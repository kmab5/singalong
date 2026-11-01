# P6 automated validation and signed candidate

Date: 2026-11-01. The user authorized builds, lint, unit tests and release signing. Physical testing remains with the user. No emulator, system image or SDK package was installed.

## Results

The final Gradle invocation completed successfully in 3m 5s:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
```

- 35 unit tests across 12 suites: zero failures/errors.
- Debug lint: zero errors, 21 warnings, all dependency-update notices. Pinned versions were retained. No lint baseline was added.
- Debug APK, signed release APK and instrumentation-test APK built successfully. Release vital lint passed.
- Room schema 2 generated and checked into `app/schemas`. Migration and Room/Compose instrumentation tests compiled but were not executed.
- `apksigner verify --verbose --print-certs`: verified, APK Signature Scheme v2, one RSA-3072 signer with the requested identity.
- `zipalign -c -P 16 -v 4`: verification successful.

Reports remain local at `app/build/reports/tests/testDebugUnitTest/index.html` and `app/build/reports/lint-results-debug.html`.

## Fixes and environment findings

- Set both `JAVA_HOME` and the quoted Gradle JVM argument to JDK 21; the initial attempt selected the machine's Java 26 and failed.
- The KSP compiler dependency download repeatedly stalled/reset. Recovered it from the same Maven Central URL using resumed transfers, verified published SHA-1 `614ad99c6e8890e1438501c90092d2d9bc5f1b68`, and populated the local Gradle artifact cache. No dependency version or repository was changed.
- Resolved Android/Compose `WindowInsets` name collision with an explicit Android import.
- Replaced the editor's `LocalContext` activity cast with `LocalActivity`.
- Added Android 12+ extraction rules excluding automatic cloud/device-transfer data; explicit in-app backup remains the supported recovery path.
- Documented the intentional physical-left origin of overlay coordinates for RTL lint rather than incorrectly changing geometry to logical-start coordinates.
- Existing SDK tools emitted an XML-version warning, and two dependency native libraries could not have their debug symbols stripped. Neither prevented builds, signature checks or APK alignment verification.

## Phone artifact

Install `artifacts/singalong-release.apk` (copied from `app/build/outputs/apk/release/app-release.apk`). Size: 47,375,541 bytes. Package `com.kmab.singalong`, version name `0.2.0-p5`, version code 3; minimum API 31, target API 36.

SHA-256: `AB8E2B01A2F7B36F9E5B4A8CFD852FFD8CDD3EDB5A1FE67C19113EE09F958A38`.

Certificate SHA-256: `8BBE1DA96DD01E32F971D516F1619B67C5A15B5222B40864AD3179F62AEF6A04`.

The private key and credentials are ignored local files; see [release signing](release-signing.md) for their locations and backup/update procedure. Source changes and the public fingerprint are committed; APKs, private keys, passwords and local build logs are not.

## Remaining acceptance

M6 is a signed candidate, not a fully device-verified release. The [compatibility matrix and phone walkthrough](compatibility.md) remain pending. Installation, real-player metadata/timing, permissions, overlay behavior, battery usage, RTL/large-text/TalkBack, upgrade migration and transactional recovery still require actual device evidence. No successful live lyric-provider request or online translation availability is newly claimed by these build checks. Earlier phase documents record historical unverified states; this checkpoint supersedes their build/unit/lint status only.
