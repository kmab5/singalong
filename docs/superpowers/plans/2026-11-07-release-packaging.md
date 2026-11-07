# 1.2.2 release packaging

Ship version **1.2.2 (22)** from main using the existing signing identity.

Build a universal APK plus arm64-v8a, armeabi-v7a, x86 and x86_64 variants. Publish all five APKs in one release. Keep mapping, build metadata and checksums in CI artifacts for 90 days, with no diagnostic attachments on the public release.

Preserve automatic publication for new versions on main and matching explicit tags. Complete releases remain immutable; unchanged website/documentation pushes skip publication.

The website keeps its latest-stable universal fallback and adds architecture links only when the release provides them. Condense documentation into current maintainer guides, retaining privacy, licensing, design and verification boundaries.

Before committing, run relevant helper/site checks and Android unit/lint/build checks. Verify each APK’s version, ABI contents and pinned certificate, plus mapping and alignment. Then commit, tag and push the new version. Remote monitoring is outside this change; report local verification and pushed references without assuming hosted publication succeeded.
