# Error Prone Rules Requirements

## Mission

Copy Ariake's Error Prone rule strictness into Bazel-Depgen where it can be enforced without a massive current-source rewrite.

## Scope

- Keep Bazel-Depgen on Java 17.
- Replace the current `-XepDisableAllChecks` wrapper default with Ariake's explicit built-in Error Prone checks.
- Add Ariake's test-only Error Prone check if the active Bazel toolchain supports it.
- Enable Ariake's nullness-related Error Prone options if the active Bazel toolchain and current source accept them.
- Enable Ariake's `Varifier` check after the explicit follow-up request, accepting the broad mechanical `var` migration required by the current source.
- Adopt Ariake's NullAway plugin/JSpecify wiring after the explicit follow-up request, while removing the old `javax.annotation` dependency from Bazel-Depgen's source dependencies.
- Update contributor-visible release notes for the stricter build checks.

## Quality Gates

- `bazel build //...` passes after the strict rules are enabled.
- `tools/check.sh` passes before claiming completion.

## Open Questions Register

- id: Q-01
  status: resolved
  question: Should Java language level follow Ariake or remain on Java 17?
  context: Ariake builds with Java 25, while the prior Bazel-Depgen migration explicitly kept this project on Java 17.
  options: Move to Java 25, or keep Java 17 while copying the Error Prone checks.
  tradeoffs: Moving to Java 25 would diverge from the user's earlier Java 17 constraint. Keeping Java 17 preserves the intended runtime baseline while still tightening source checks.
  recommended_default: Keep Java 17.
  user_decision: Keep Java 17.
  artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`
- id: Q-02
  status: resolved
  question: Should NullAway-style checks be included in this change?
  context: Ariake enables NullAway and RequireExplicitNullMarking via extra dependencies and a compiler plugin. Bazel-Depgen currently uses `javax.annotation` rather than JSpecify nullness annotations.
  options: Include the NullAway-related javacopts if they validate cleanly, or limit this change to built-in non-nullness Error Prone checks.
  tradeoffs: NullAway-style checks are stricter, but full Ariake plugin/dependency wiring could require a broad nullness migration. Command-line probes can prove whether the current Bazel toolchain accepts the options without extra source churn.
  recommended_default: Include the NullAway-related javacopts if they validate cleanly; avoid extra plugin/dependency wiring unless needed.
  user_decision: Include the NullAway-related javacopts after validation; avoid extra plugin/dependency wiring because the checked-in source builds without it.
  artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`
- id: Q-03
  status: resolved
  question: Should `Varifier` now be enforced despite the broad mechanical source impact?
  context: The first strict-check pass omitted `Varifier` because it affected hundreds of local declarations. The user explicitly requested implementing the `Varifier` Error Prone check in a follow-up.
  options: Keep omitting `Varifier`, or enable `Varifier` and migrate affected local declarations.
  tradeoffs: Keeping it omitted avoids churn but leaves one Ariake check unenforced. Enabling it creates a broad mechanical diff but completes the requested strictness.
  recommended_default: Enable `Varifier` and migrate the current source.
  user_decision: Enable `Varifier` and migrate the current source.
  artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- id: Q-04
  status: resolved
  question: Should Bazel-Depgen now adopt Ariake's NullAway plugin/JSpecify wiring and drop `javax.annotation`?
  context: The earlier strict-check pass enabled NullAway javacopts without the plugin because the source still used `javax.annotation`. The user explicitly requested migrating annotations to JSpecify, enabling Ariake-style strict NullAway checks, and removing the old dependency.
  options: Keep the javacopts-only setup, or add the plugin and migrate source/build dependencies to JSpecify.
  tradeoffs: Keeping javacopts-only minimizes churn but does not enforce NullAway. Adding the plugin and JSpecify annotations enables real nullness checking and removes the legacy annotation artifact, but requires source and BUILD migration.
  recommended_default: Add the plugin and migrate to JSpecify.
  user_decision: Add the plugin and migrate to JSpecify.
  artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
