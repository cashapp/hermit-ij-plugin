#!/bin/bash

# This script upgrades the versions of the IDEs in the build file, if the latest
# major version is larger than the one in build.gradle

# Returns the latest released version for the given JetBrains product code.
latestRelease() {
  typeCode=$1
  releaseType=$2
  url="https://data.services.jetbrains.com/products?fields=code,releases.build,releases.type&code=${typeCode}"
  curl -s "${url}" | jq "[.[0]|.releases|.[]|select(.type==\"${releaseType}\")|.build][0]" | tr -d '"'
}

# Returns the latest IDE version used to build the plugin for the given IDE
currentVersion() {
  typeCode=$1
  releaseType=$(echo "$2" | tr "[:lower:]" "[:upper:]")
  grep "val ${typeCode}_${releaseType}_VERSION" build.gradle.kts | sed "s/^[^\"]*\"\([^\"]*\)\".*/\1/"
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
  releaseType=$(echo "$3" | tr "[:lower]" "[:upper]")
  sed -i.bak "s/^val ${typeCode}_${releaseType}_VERSION = .*$/val ${typeCode}_${releaseType}_VERSION = \"${version}\"/g" build.gradle.kts
}

# Updates the version for given product type in the build file if the major version
# of the latest release differs
updateIfNeeded() {
  typeCode=$1
  releaseType=$2

  latest=$(latestRelease "${typeCode}" "${releaseType}")
  current=$(currentVersion "${typeCode}" "${releaseType}")
  latestMajor=$(majorVersion "${latest}")
  currentMajor=$(majorVersion "${current}")

  if [ "$latestMajor" != "$currentMajor" ]; then
    echo "Upgrading ${typeCode} (${releaseType}) to ${latest}"
    setVersion "$typeCode" "$latest" "$releaseType"
  else
    echo "No upgrade needed for ${typeCode} ${releaseType}"
  fi
}

updateIfNeeded IIC release
updateIfNeeded IIC eap
updateIfNeeded GO release
updateIfNeeded GO eap
