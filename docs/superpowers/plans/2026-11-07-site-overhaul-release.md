# Site overhaul and release publication

The user explicitly authorized a major GitHub Pages redesign, release automation, committing/tagging/pushing, and staying on main. This supersedes the earlier merge-only publication restriction for v1.2.0.

## Design

Use a music-poster direction: oversized Inter headlines, restrained Lora italic accents, PT Mono utility labels, near-black ink (#111016), pearl (#f1edf7), violet (#bda0f5), mist (#a5a0b1) and quiet glass surfaces. Replace the previous page structure and CSS rather than append another feature section.

Lead with a clear Android download and a layered floating-lyrics demonstration over original illustrated artwork. Use a pale contrasting band for the share-card gallery, with selectable cover palettes and a format control. Follow with concise capabilities, three-step setup and honest FAQ. Every illustration is labelled; sample words/artwork are original and not Android screenshots. Use native controls, keyboard focus, 44px targets, bounded motion and reduced-motion support. Self-host fonts and preserve logo downloads and feedback links.

## Release

Existing main/Pages CI succeeded; release CI was tag-only and no v1.2.0 tag was pushed. Add main pushes and a manual retry to release automation. Derive the tag from version.properties, skip already-published versions, validate main ancestry, retain certificate checks, and create an annotated tag only after checks pass. A tag event continues to work. Serialize publication to avoid main/tag races; never replace a release or move a version tag.

Validate source/version/docs and website interactions at phone/tablet/desktop widths. Commit on main and push main plus the annotated v1.2.0 tag. Wait for release and Pages results; verify uploaded APK, certificate, checksums, mapping and live download/UI before claiming publication complete.
