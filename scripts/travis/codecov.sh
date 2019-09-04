#!/bin/bash

set -e

# shellcheck source=travis_retry.sh
source "${TRAVIS_BUILD_DIR}/scripts/travis/travis_retry.sh"

if [[ "${COVERAGE_BUILD}" -ne 0 ]]; then
  codecov_coverage_file="${TRAVIS_BUILD_DIR}/target/site/jacoco/jacoco.xml"
  echo "Sending coverage data to Codecov"
  travis_retry codecov \
    --required \
    --token "${CODECOV_TOKEN}" \
    --file "${codecov_coverage_file}" \
    --root "${TRAVIS_BUILD_DIR}" -X gcov
fi
