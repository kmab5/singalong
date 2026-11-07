# Security

Report vulnerabilities through the [kmab feedback form](https://kmab5.github.io/kmab-feedback/?product=singalong&type=bug), marked as security-related. Include affected versions, a minimal reproduction and the impact. Keep exploits, credentials and private information out of public issues.

I maintain the latest published release. There is no promised response time or bounty program.

Release APKs must match the signing certificate in [release-certificate.sha256](release-certificate.sha256). CI checks the certificate before publication. From 1.2.2, build metadata, checksums and crash mappings are retained as CI artifacts for 90 days; public release attachments contain APKs only. Earlier releases keep their original assets.

Never share the signing key or passwords. If the key may be compromised, stop publication and contact me through the feedback form. Don’t silently replace the key or overwrite a published release.
