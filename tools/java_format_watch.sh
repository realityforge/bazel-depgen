#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

exec bazel run @rules_palantir_java_format//:java_format_watch -- --root=src --root=tools
