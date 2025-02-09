if (${env:COVERAGE_BUILD} -ne 0) {
  $codecov_root_folder = ${env:APPVEYOR_BUILD_FOLDER} -replace "\\", "/"
  $codecov_coverage_file = "${codecov_root_folder}/target/site/jacoco/jacoco.xml"
  Write-Host "Sending coverage data to Codecov"
  appveyor-retry codecov --required --token "${env:CODECOV_TOKEN}" --file "${codecov_coverage_file}" --flags "${env:CODECOV_FLAG}" --root "${codecov_root_folder}" -X gcov;
  if (${LastExitCode} -ne 0) {
    throw "Failed to send coverage data to Codecov"
  }
}
