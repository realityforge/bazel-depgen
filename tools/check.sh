#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"
COVERAGE_FILTER="^//src/main/java/org/realityforge/bazel/depgen[/:]"
BAZEL_INFO="$(bazel info execution_root output_base)"
EXECUTION_ROOT=""
OUTPUT_BASE=""
while IFS= read -r line; do
  case "${line}" in
    "execution_root: "*)
      EXECUTION_ROOT="${line#execution_root: }"
      ;;
    "output_base: "*)
      OUTPUT_BASE="${line#output_base: }"
      ;;
  esac
done <<< "${BAZEL_INFO}"
if [[ -z "${EXECUTION_ROOT}" || -z "${OUTPUT_BASE}" ]]; then
  echo "Unable to determine Bazel execution_root and output_base" >&2
  exit 1
fi
COVERAGE_REPORT="${EXECUTION_ROOT}/bazel-out/_coverage/_coverage_report.dat"

BAZEL_OUTPUT_BASE="${OUTPUT_BASE}" tools/update_java_deps.sh
bazel run //:buildifier_check
tools/java_format.sh check
bazel build //...

# The main Java suite is exercised by the coverage run below. Keep this normal
# test pass limited to targets outside that aggregate so check.sh preserves full
# test coverage without running the main suite twice.
bazel test \
  //third_party/java:verify_config_sha256 \
  //tools/release/org/realityforge/bazel/depgen/release:all_jar_integration_test \
  //tools/release/org/realityforge/bazel/depgen/release:jar_builder_test \
  //tools/release/org/realityforge/bazel/depgen/release:release_lifecycle_tool_test
bazel coverage //src/test/java/org/realityforge/bazel/depgen:all_tests --combined_report=lcov --instrumentation_filter="${COVERAGE_FILTER}"
tools/check_coverage.py "${COVERAGE_REPORT}" 0.95 0.85
