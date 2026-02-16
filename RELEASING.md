# Releasing

## Automatic Releases

Changes merged to main are released automatically daily at 2 AM UTC via a scheduled GitHub workflow. The workflow checks if there are new commits since the last tag, bumps the patch version, and creates a new tag. This triggers the release workflow which publishes to JetBrains marketplace (approval takes a couple days).

Documentation-only changes (*.md files, docs/, LICENSE, .gitignore) will skip the automatic release.

## Manual Releases

Block engineers are able to cut a release on demand if waiting for the daily release is not desirable.

To release a new version of the Hermit IntelliJ plugin:

```bash
just release
```

This will show the latest version, calculate the next patch version, and prompt for confirmation before creating and pushing the tag.

The [release workflow](.github/workflows/release.yml) will then automatically:
- Run tests
- Publish to the JetBrains plugin marketplace (both stable and EAP channels)
