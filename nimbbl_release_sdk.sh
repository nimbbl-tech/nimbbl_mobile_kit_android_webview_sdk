#!/bin/bash

set -e -u


THIS_DIR=$(cd -P "$(dirname "$(readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")")" && pwd)
MVN_REPO="https://gitlab.com/api/v4/projects/25847308/packages/maven"
DEFAULT_SDK_VERSION="3.0.5"
SDK_VERSION="3.0.5"

echo "Releasing Nimbbl SDK ${SDK_VERSION}"

echo "Building and publishing the Nimbbl Checkout SDK"
echo "Using ${MVN_REPO} as the Maven repo"

pushd ${THIS_DIR}/
./gradlew clean 
./gradlew assembleRelease 
./gradlew publish
popd

# Done!
echo "Finished! Don't forget to push the tag and the Maven repo artifacts."
