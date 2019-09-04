#!/bin/bash

set -e

# shellcheck source=travis_retry.sh
source "${TRAVIS_BUILD_DIR}/scripts/travis/travis_retry.sh"

if [[ "${MAVEN_WRAPPER}" -ne 0 ]]; then
  build_cmd="\"${TRAVIS_BUILD_DIR}/mvnw\""
else
  build_cmd="mvn"
fi

build_cmd="${build_cmd} -f "${TRAVIS_BUILD_DIR}/pom.xml" --batch-mode clean package"

if [[ "${COVERAGE_BUILD}" -ne 0 ]]; then
  build_cmd="${build_cmd} -P jacoco"
fi

echo "Building with: ${build_cmd}"
eval "${build_cmd}"
