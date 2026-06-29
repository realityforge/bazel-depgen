#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"
COVERAGE_FILTER="^//src/main/java/org/realityforge/bazel/depgen[/:]"
COVERAGE_REPORT="$(bazel info execution_root)/bazel-out/_coverage/_coverage_report.dat"

tools/update_java_deps.sh
bazel run //:buildifier_check
tools/java_format.sh check
bazel build //...
bazel test //...
bazel coverage //src/test/java/org/realityforge/bazel/depgen:all_tests --combined_report=lcov --instrumentation_filter="${COVERAGE_FILTER}"
tools/check_coverage.py "${COVERAGE_REPORT}" 0.95 0.85
