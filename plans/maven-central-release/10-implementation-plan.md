# Maven Central Release Packaging Implementation Plan

## Phase 1: Release Package Foundation

- Add `//tools/release` with a version build setting and Starlark rules/macros.
- Replace hardcoded `config.properties` version generation with the release version setting.
- Add `.bazelrc` alias `--release_version`.

## Phase 2: Artifact Builders

- Add Java helper binaries for:
  - deterministic jar creation/merging;
  - source jar assembly;
  - Javadoc jar assembly;
  - POM generation;
  - dist staging/signing/checksum/zipping.
- Build `maven_artifacts` outputs:
  - `bazel-depgen.jar`;
  - `bazel-depgen-sources.jar`;
  - `bazel-depgen-javadoc.jar`;
  - `bazel-depgen-all.jar`;
  - `bazel-depgen.pom`.

## Phase 3: Integration Test

- Add a Java integration test that executes `java -jar` on the curated all jar with no external classpath.
- The test creates a fresh local Maven repository programmatically, then runs `init`, `add`, and `generate`.
- Wire the release test into `//tools/release:all_tests` and top-level `//:all_tests`.

## Phase 4: Dist and Release Ergonomics

- Add `//tools/release:dist` runnable target.
- Add `tools/package_maven_central.sh`.
- Document wrapper and raw commands in `CONTRIBUTING.md`.
- Disable or redirect old Ruby Maven Central packaging/upload tasks.
- Add changelog entry and `/dist` ignore rule.

## High-Risk Areas

- Curated all-jar payload could miss runtime classes. Mitigation: direct `java -jar` integration test.
- Duplicate classes/resources can hide broken fat-jar behavior. Mitigation: fail on duplicate non-identical entries except explicitly merged service files/legal metadata.
- Maven Resolver jars include duplicate `META-INF/sisu/*` service-index files and Plexus component descriptors. Mitigation: merge Sisu indexes with the same unique-line behavior as `META-INF/services/*`, and merge Plexus `<component>` entries into one descriptor.
- Version skew can make generated files inconsistent. Mitigation: one build setting drives all generated release outputs.
- Dist signing can create partial output. Mitigation: fail fast and clean only version-specific outputs.

## Required Full Gate

`tools/check.sh`

## Validation Notes

- The side-effecting dist path can be validated with a temporary signer passed via `--gpg-executable`. The signer must
  create the requested `.asc` output so the run exercises staging, signer invocation, signature presence checks, checksum
  generation, and zip creation without requiring a real private key. Real releases use `gpg` directly with `GPG_USER`
  and optional `GPG_PASS`, or pass `--gpg-key-id` to override `GPG_USER`.

## Decision Log

- Q01-Q03: use build setting version, default snapshot, release rejects snapshots.
- Q04-Q07: repo-local Bazel packaging, empty POM dependencies, executable manifest only in `-all`.
- Q08-Q30: curated `-all` dependency allowlist based on Ruby payload, direct jar labels, add JSpecify, remove javax annotation root.
- Q31-Q32: Java integration test runs `java -jar` against a generated local Maven repository.
- Q33-Q36: place implementation in `//tools/release`, expose `maven_artifacts` and `dist`, add wrapper and docs.
- Q37-Q41: retire old Ruby release path, wire tests into top-level suite, version-specific dist cleanup, update docs/changelog, verify with focused and full gates.
