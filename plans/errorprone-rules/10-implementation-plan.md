# Error Prone Rules Implementation Plan

## Phases

1. Compare Ariake and Bazel-Depgen Java wrapper rules.
2. Port Ariake's built-in Error Prone checks into `third_party/java/rules.bzl` while preserving `--release 17`.
3. Run `bazel build //...` and fix source issues only when the fixes are small and improve the current code.
4. Document the `Varifier` follow-up decision and migration evidence.
5. Run `tools/check.sh` and record results.

## Delivery Approach

- Keep the change concentrated in the Java wrapper macros and minimal source fixes.
- Prefer enabling strict checks over suppressing them.
- Treat broad nullness annotation migration as out of scope unless it proves trivial.
- Earlier decision superseded: `Varifier` was omitted because it caused broad mechanical churn, but the follow-up request explicitly asks to implement it. Enable it and migrate the affected local declarations.

## Required Full Gate

- `tools/check.sh`

## Decision Log

- Q-01: Keep Java 17 by preserving `--release 17` in the wrapper macro.
- Q-02: Include Ariake's NullAway-related javacopts after validation showed they build cleanly; avoid extra plugin/dependency wiring because it is unnecessary for the current source.
- Q-03: Enable `Varifier` despite broad mechanical churn and validate the migrated source with `tools/check.sh`.
