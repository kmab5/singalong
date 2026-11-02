# Security reports

Report suspected vulnerabilities through the [kmab feedback form](https://kmab5.github.io/kmab-feedback/?product=singalong&type=bug), identifying the report as security-related. Do not publish working exploits, credentials or private user information in public issues. Include affected versions, a minimal reproduction and impact. There is no promised response SLA or bounty program.

The latest published release is the maintenance target. Older APKs may lack fixes. Verify release APKs using the certificate fingerprint in [release-certificate.sha256](release-certificate.sha256) and the published `SHA256SUMS`.

Never share the signing keystore/passwords. Release signing uses encrypted repository secrets and checks the certificate before artifact upload or publication. If signing material is suspected compromised, stop publishing and contact the maintainer; do not silently replace the key or attempt to overwrite an existing release.
