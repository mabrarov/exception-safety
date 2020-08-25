if (${env:MAVEN_WRAPPER} -ne 0) {
  $build_cmd = "& ""${env:APPVEYOR_BUILD_FOLDER}\mvnw.cmd"""
} else {
  $build_cmd = "mvn"
}

$build_cmd = "${build_cmd} -f ""${env:APPVEYOR_BUILD_FOLDER}\pom.xml"" --batch-mode clean package"

if (${env:COVERAGE_BUILD} -ne 0) {
  $build_cmd = "${build_cmd} -P jacoco"
}

if ((Test-Path env:MAVEN_BUILD_OPTIONS) -and (${env:MAVEN_BUILD_OPTIONS} -ne "")) {
  $build_cmd = "${build_cmd} ${env:MAVEN_BUILD_OPTIONS}"
}

Write-Host "Building with: ${build_cmd}"
Invoke-Expression "${build_cmd}"
if (${LastExitCode} -ne 0) {
  throw "Build failed with exit code ${LastExitCode}"
}
