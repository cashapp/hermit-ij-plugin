# Releasing

Releases are managed by Block engineers.

To release a new version of the Hermit IntelliJ plugin:

```bash
just release
```

This will show the latest version, calculate the next patch version, and prompt for confirmation before creating and pushing the tag.

The [release workflow](.github/workflows/release.yml) will then automatically:
- Run tests
- Publish to the JetBrains plugin marketplace (both stable and EAP channels)
