#!/bin/bash

set -e

# shellcheck source=travis_retry.sh
source "${TRAVIS_BUILD_DIR}/scripts/travis/travis_retry.sh"

if [[ "${MAVEN_WRAPPER}" -ne 0 ]]; then
  build_cmd="$(printf "%q" "${TRAVIS_BUILD_DIR}/mvnw")"
else
  build_cmd="mvn"
fi

maven_build_phase=""
if [[ "${COPILOT_BUILD}" -ne 0 ]]; then
  maven_build_phase="install"
else
  maven_build_phase="package"
fi

build_cmd="${build_cmd} -f $(printf "%q" "${TRAVIS_BUILD_DIR}/pom.xml") --batch-mode clean $(printf "%q" "${maven_build_phase}")"

if [[ "${COVERAGE_BUILD}" -ne 0 ]]; then
  maven_profiles="${maven_profiles:+${maven_profiles},}jacoco"
fi

if ! [[ "${maven_profiles}" = "" ]]; then
  build_cmd="${build_cmd} -P $(printf "%q" "${maven_profiles}")"
fi

build_cmd="${build_cmd}${MAVEN_BUILD_OPTIONS:+ }${MAVEN_BUILD_OPTIONS}"

echo "Building with: ${build_cmd}"
eval "${build_cmd}"

if [[ "${COPILOT_BUILD}" -ne 0 ]]; then
  bash <(curl -s https://copilot.blackducksoftware.com/ci/travis/scripts/upload)
fi
