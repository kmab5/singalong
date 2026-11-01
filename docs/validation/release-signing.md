# Release signing

The personal release key is local at `.signing/singalong-release.jks`, alias `singalong-release`. Its identity is `CN=samuel, O=kmab, L=Denizli, C=TR`; RSA 3072, validity 10,000 days. Credentials are in ignored `signing.properties`. Neither file belongs in Git.

Certificate SHA-256: `8B:BE:1D:A9:6D:D0:1E:32:F9:71:D5:16:F1:61:9B:67:C5:A1:5B:52:22:B4:08:64:AD:31:79:F6:2A:EF:6A:04`. Generated 2026-11-01; valid until 2054-03-19. This public fingerprint can be used to verify future release APKs.

Keep a secure backup of **both files** outside this checkout. Future release builds must reuse this key so Android can install updates over the existing release. Do not regenerate it if it is missing. The password was supplied by the user and is deliberately omitted from this document.

From PowerShell:

```powershell
$env:JAVA_HOME = 'D:/Code/java/jdk-21.0.12.1'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
.\gradlew.bat :app:assembleRelease '-Dorg.gradle.java.home=D:/Code/java/jdk-21.0.12.1' --console=plain
```

Set `JAVA_HOME` as well as the quoted JVM argument because the wrapper itself otherwise uses the machine's default Java. The release build fails explicitly when signing configuration is missing. Debug builds use the usual Android debug certificate; they cannot update an installation signed with the release certificate. Use release APKs consistently on the test phone to preserve update continuity.

APK paths: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release.apk`. See the P6 checkpoint for actual build and signature verification results.

Release builds enable R8 optimization/obfuscation and resource shrinking. Preserve `app/build/outputs/mapping/release/mapping.txt` alongside each distributed APK's checksum to decode obfuscated crash traces. The current candidate's mapping is archived under `artifacts/mapping/`, named by its APK SHA-256. These files remain local and ignored by Git.
