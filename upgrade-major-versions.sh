#!/bin/bash

# This script upgrades the versions of the IDEs in the build file, if the latest
# major version is larger than the one in build.gradle

# Returns the latest released version for the given JetBrains product code.
latestRelease() {
  typeCode=$1
  url="https://data.services.jetbrains.com/products?fields=code,releases.build,releases.type&code=${typeCode}"
  curl -s "${url}" | jq '[.[0]|.releases|.[]|select(.type=="release")|.build][0]' | tr -d '"'
}

# Returns the latest IDE version used to build the plugin for the given IDE
currentVersion() {
  typeCode=$1
  grep "def ${typeCode}_VERSION" build.gradle | sed "s/^[^']*'\([^']*\)'.*/\1/"
}

# Returns the major version for the given full version string
majorVersion() {
  version=$1
  echo "${version}" | sed "s/^\([^.]*\).*/\1/"
}

# Sets the version for the given product type in the build file
setVersion() {
  typeCode=$1
  version=$2
  sed -i.bak "s/^def ${typeCode}_VERSION = .*$/def ${typeCode}_VERSION = \'${version}\'/g" build.gradle
}

# Updates the version for given product type in the build file if the major version
# of the latest release differs
updateIfNeeded() {
  typeCode=$1

  latest=$(latestRelease "${typeCode}")
  current=$(currentVersion "${typeCode}")
  latestMajor=$(majorVersion "${latest}")
  currentMajor=$(majorVersion "${current}")

  if [ "$latestMajor" != "$currentMajor" ]; then
    echo "Upgrading ${typeCode} to ${latest}"
    setVersion "$typeCode" "$latest"
  else
    echo "No upgrade needed for ${typeCode}"
  fi
}

updateIfNeeded IIC
updateIfNeeded GO