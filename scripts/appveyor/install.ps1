if (-not (Test-Path -Path env:CODECOV_TOKEN) -or (${env:CODECOV_TOKEN} -eq "")) {
  Write-Warning "CODECOV_TOKEN is missing, skipping code coverage"
  $env:COVERAGE_BUILD = 0
}
if (${env:COVERAGE_BUILD} -ne 0) {
  pip install --disable-pip-version-check --retries "${env:PIP_RETRY}" codecov=="${env:CODECOV_VERSION}"
  if (${LastExitCode} -ne 0) {
    throw "Failed to install Codecov pip package"
  }
}

if ([int] ${env:JDK_VERSION} -le 8) {
  $env:JAVA_HOME = "C:\Program Files\Java\jdk1.${env:JDK_VERSION}.0"
} else {
  $env:JAVA_HOME = "C:\Program Files\Java\jdk${env:JDK_VERSION}"
}

$env:PATH = "${env:JAVA_HOME}\bin;${env:PATH}"
$env:MAVEN_OPTS = "-Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2 ${env:MAVEN_OPTS}"

Write-Host "JAVA_HOME: ${env:JAVA_HOME}"
$java_version=(java -version 2>&1)
if (${LastExitCode} -ne 0) {
  throw "Failed to run JVM"
}
Write-Host "${java_version}"
mvn -version
if (${LastExitCode} -ne 0) {
  throw "Failed to run Maven"
}
