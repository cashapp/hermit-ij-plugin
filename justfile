# List available recipes
default:
    @just --list

# Release a new version by creating and pushing a tag
release:
    #!/usr/bin/env bash
    set -euo pipefail
    latest=$(git tag --sort=-v:refname | head -1)
    echo "Latest version: $latest"

    # Extract version number and increment patch
    version=${latest#v}
    IFS='.' read -r major minor patch <<< "$version"
    next_patch=$((patch + 1))
    next_version="v${major}.${minor}.${next_patch}"

    echo "Next version: $next_version"
    read -p "Create and push $next_version? [y/N] " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        git tag "$next_version"
        git push origin "$next_version"
        echo "Released $next_version"
    else
        echo "Aborted"
    fi
