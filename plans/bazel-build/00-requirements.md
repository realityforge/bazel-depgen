# Bazel Build Conversion Requirements

## Mission

Add a Bazel build for `bazel-depgen` modeled on `~/Code/ariake`, while keeping this project on Java 17.

## Scope Boundaries

- Add root Bazel module/build files, third-party dependency configuration, wrapper macros, source/test targets, and check/update scripts.
- Preserve the existing Java source behavior and runtime resource expectations.
- Keep Java compilation on release 17 with `-Xlint:all,-processing,-serial` and `-Werror`, matching the existing Buildr build intent.
- Use Ariake's depgen-generated `MODULE.bazel`/`third_party/java/BUILD.bazel` structure and buildifier targets as the pattern.
- Do not convert Maven Central release automation in this change.
- Do not remove the existing Buildr files unless they block the Bazel build.

## Locked Decisions

- Bazel is pinned to `9.1.1`, matching the Ariake example after user approval to move off the existing `8.6.0` pin.
- `rules_java` and `buildifier_prebuilt` versions are copied from Ariake.
- `third_party/java/dependencies.yml` is the checked-in dependency source for generated Maven repository and target content.
- The bootstrap depgen used by `tools/update_java_deps.sh` is the latest released version already referenced by this repository, `0.25`.
- Tests remain TestNG tests.

## Command Surface

- `bazel build //...` builds the project.
- `bazel test //...` runs the Bazel tests.
- `tools/update_java_deps.sh` regenerates Maven dependency outputs.
- `tools/check.sh` updates generated dependency outputs, runs buildifier check, then runs Bazel build and tests.

## Quality Gates

- `tools/update_java_deps.sh`
- `bazel run //:buildifier -- MODULE.bazel third_party/java/BUILD.bazel`
- `bazel build //...`
- `bazel test //...`

## Intentional Divergences

- Ariake uses Java 25 plus Error Prone/NullAway wrapper defaults; this project keeps Java 17 and the existing lint policy.
- Ariake uses JUnit tests; this project uses a TestNG runner target.

## Open Questions Register

No open questions. The Buildr release flow is preserved because the requested conversion is the build/test surface and release publishing was not requested.
