# Releasing

Changes merged to main are released automatically on a daily basis via a scheduled Github action.
Once the release process completes, there's a Jetbrains approval process that can take a couple of days.

Block engineers are able to cut a release on demand if waiting for the daily release is not desirable.

To release a new version of the Hermit IntelliJ plugin:

```bash
just release
```

This will show the latest version, calculate the next patch version, and prompt for confirmation before creating and pushing the tag.

The [release workflow](.github/workflows/release.yml) will then automatically:
- Run tests
- Publish to the JetBrains plugin marketplace (both stable and EAP channels)
