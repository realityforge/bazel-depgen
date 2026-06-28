# Bazel Build Conversion Plan

## Phase Sequence

1. Add planning artifacts for the conversion scope.
2. Add Bazel root/module files and a Java 17 wrapper macro.
3. Add third-party dependency config and update/check scripts modeled on Ariake.
4. Add production binary/library targets and TestNG test targets.
5. Generate depgen-managed dependency outputs and run Bazel validation.

## Delivery Approach

- Keep targets coarse-grained unless Bazel requires finer dependency partitioning.
- Generate external Maven imports with this project's released depgen jar using `third_party/java/dependencies.yml`.
- Add a runtime resource generation rule for `org/realityforge/bazel/depgen/config.properties` because `DepGenConfig` loads that resource.
- Run buildifier after generated files are present.

## High-Risk Areas

- TestNG execution under Bazel: mitigate by adding a small runner and validating `bazel test`.
- Runtime resource packaging: mitigate by checking the binary/test build includes generated `config.properties`.
- Dependency graph completeness: mitigate by using depgen output and compiling the full source/test tree.
- Generated output bootstrapping: mitigate by using `bazel-depgen` 0.25, matching the repository's current README and changelog.

## Required Full Gate

`tools/check.sh`

## Decision Log

- Move `.bazelversion` to `9.1.1` after user approval, matching Ariake's Bazel version while keeping Java 17.
- Use `bazel_depgen_project` as the module name so it does not collide with the generated external repository named `bazel_depgen`.
- Keep Java 17 compile settings instead of Ariake's Java 25 wrapper.
- Preserve existing Buildr release files because release conversion is out of scope.
