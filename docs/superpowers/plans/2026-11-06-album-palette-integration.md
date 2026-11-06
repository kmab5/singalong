# Album-art palette and main integration

The user authorized implementation, cleanup, commit, merge into main and push. Public release tagging/publication is separate.

- Add an Album art palette alongside the curated palettes. Derive it locally from a bounded sample of the current album cover, never the independent background photo. Selecting another cover refreshes it; hiding the foreground cover does not remove it. Explain unavailable artwork instead of inventing colors from a missing image.
- Quantize sampled colors by population, use a predominant color for a dark tinted base and a second prevalent color for the accent, and ensure readable foreground contrast. Keep extraction platform-independent except for bitmap sampling; compute off the UI thread after image decoding and reuse the immutable result for swatches, previews and PNGs.
- Review all application modules, site, docs and release tooling. Fix concrete issues and remove obsolete generated files without deleting signing credentials, user feedback, historical documentation or crash mappings.
- Refresh the website to distinguish prepared 1.2 source from the latest published APK. Show a labelled card illustration using original sample text, not a purported app screenshot.
- Promote source to 1.2.0 (20), run debug/release tests, lint, APK builds, site browser checks, docs/version/tool checks, and verify the existing signing certificate. Commit focused changes, merge main, push and check CI status. Do not tag or publish an unrequested release.
