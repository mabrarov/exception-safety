#!/bin/bash

set -e

# shellcheck source=travis_retry.sh
source "${TRAVIS_BUILD_DIR}/scripts/travis/travis_retry.sh"

if [[ "${COVERAGE_BUILD}" -ne 0 ]]; then
  travis_retry pip3 install --user --upgrade pip
  travis_retry pip3 install --user \
    --disable-pip-version-check \
    --retries "${PIP_RETRY}" \
    codecov=="${CODECOV_VERSION}"
fi

if [[ -n "${CUSTOM_JDK+x}" ]] && [[ -n "${CUSTOM_JDK_VERSION+x}" ]]; then
  echo "Custom JDK requested: ${CUSTOM_JDK} ${CUSTOM_JDK_VERSION}"
  case "${CUSTOM_JDK}" in
    "amazon-corretto")
      custom_jdk_dist_base_name="amazon-corretto-${CUSTOM_JDK_VERSION}-linux-x64"
      custom_jdk_dist_name="${custom_jdk_dist_base_name}.tar.gz"
      custom_jdk_url="https://d3pxv6yz143wms.cloudfront.net/${CUSTOM_JDK_VERSION}/${custom_jdk_dist_name}"
      custom_jdk_dir_name="${custom_jdk_dist_base_name}"
      ;;
    "azul-zulu")
      custom_jdk_version_major="$(echo "${CUSTOM_JDK_VERSION}" | sed -r 's/([0-9]+)\..*/\1/;t;d')"
      case "${custom_jdk_version_major}" in
        "8")
          custom_jdk_dist_base_name="zulu${custom_jdk_version_major}.40.0.25-ca-jdk${CUSTOM_JDK_VERSION}-linux_x64"
          custom_jdk_dist_name="${custom_jdk_dist_base_name}.tar.gz"
          custom_jdk_url="https://cdn.azul.com/zulu/bin/${custom_jdk_dist_name}"
          ;;
        "11")
          custom_jdk_dist_base_name="zulu${custom_jdk_version_major}.33.15-ca-jdk${CUSTOM_JDK_VERSION}-linux_x64"
          custom_jdk_dist_name="${custom_jdk_dist_base_name}.tar.gz"
          custom_jdk_url="https://cdn.azul.com/zulu/bin/${custom_jdk_dist_name}"
          ;;
        *)
          echo "Unsupported version of custom JDK: ${CUSTOM_JDK} ${CUSTOM_JDK_VERSION}"
          exit 1
      esac
      custom_jdk_dir_name="${custom_jdk_dist_base_name}"
      ;;
    "AdoptOpenJDK")
      custom_jdk_version_major="$(echo "${CUSTOM_JDK_VERSION}" | sed -r 's/([0-9]+)[\.u].*/\1/;t;d')"
      case "${custom_jdk_version_major}" in
        "8")
          custom_jdk_dist_base_name="OpenJDK${custom_jdk_version_major}U-jdk_x64_linux_hotspot_${CUSTOM_JDK_VERSION}"
          custom_jdk_dist_name="${custom_jdk_dist_base_name}.tar.gz"
          custom_jdk_version_without_build="$(echo "${CUSTOM_JDK_VERSION}" | sed -r 's/([0-9]+u[0-9]+)b.*/\1/;t;d')"
          custom_jdk_version_build="$(echo "${CUSTOM_JDK_VERSION}" | sed -r 's/([0-9]+u[0-9]+)b([0-9]+)/\2/;t;d')"
          custom_jdk_url="https://github.com/AdoptOpenJDK/openjdk${custom_jdk_version_major}-binaries/releases/download/jdk${custom_jdk_version_without_build}-b${custom_jdk_version_build}/${custom_jdk_dist_name}"
          custom_jdk_dir_name="jdk${custom_jdk_version_without_build}-b${custom_jdk_version_build}"
          ;;
        "11")
          custom_jdk_dist_base_name="OpenJDK${custom_jdk_version_major}U-jdk_x64_linux_hotspot_${CUSTOM_JDK_VERSION}"
          custom_jdk_dist_name="${custom_jdk_dist_base_name}.tar.gz"
          custom_jdk_version_without_build="$(echo "${CUSTOM_JDK_VERSION}" | sed -r 's/([0-9]+\.[0-9]+\.[0-9]+)_.*/\1/;t;d')"
          custom_jdk_version_build="$(echo "${CUSTOM_JDK_VERSION}" | sed -r 's/([0-9]+\.[0-9]+\.[0-9]+)_([0-9])/\2/;t;d')"
          custom_jdk_url="https://github.com/AdoptOpenJDK/openjdk${custom_jdk_version_major}-binaries/releases/download/jdk-${custom_jdk_version_without_build}%2B${custom_jdk_version_build}/${custom_jdk_dist_name}"
          custom_jdk_dir_name="jdk-${custom_jdk_version_without_build}+${custom_jdk_version_build}"
          ;;
        *)
          echo "Unsupported version of custom JDK: ${CUSTOM_JDK} ${CUSTOM_JDK_VERSION}"
          exit 1
      esac
      ;;
    *)
      echo "Unsupported custom JDK: ${CUSTOM_JDK}"
      exit 1
      ;;
  esac

  custom_jdk_home="${DEPENDENCIES_HOME}/${custom_jdk_dir_name}"
  if [[ ! -d "${custom_jdk_home}" ]]; then
    custom_jdk_dist="${DOWNLOADS_HOME}/${custom_jdk_dist_name}"
    if [[ ! -f "${custom_jdk_dist}" ]]; then
      mkdir -p "${DOWNLOADS_HOME}"
      echo "Downloading custom JDK from ${custom_jdk_url} to ${custom_jdk_dist}"
      curl \
        --connect-timeout "${CURL_CONNECT_TIMEOUT}" \
        --max-time "${CURL_MAX_TIME}" \
        --retry "${CURL_RETRY}" \
        --retry-delay "${CURL_RETRY_DELAY}" \
        -jksSL \
        "${custom_jdk_url}" \
        --output "${DOWNLOADS_HOME}/${custom_jdk_dist_name}"
    fi
    mkdir -p "${DEPENDENCIES_HOME}"
    echo "Extracting custom JDK from ${custom_jdk_dist} into ${DEPENDENCIES_HOME}"
    tar -xzf "${custom_jdk_dist}" -C "${DEPENDENCIES_HOME}"
  fi

  echo "${CUSTOM_JDK} ${CUSTOM_JDK_VERSION} is located at ${custom_jdk_home}"
  export JAVA_HOME="${custom_jdk_home}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

echo "JAVA_HOME: ${JAVA_HOME}"
java -version
mvn -version
