# Remove Command Implementation Plan

Status: accepted

## Delivery Approach

Implement `remove` as the inverse of the current SnakeYAML-backed `add` command. Extract only the shared comment-aware YAML document editing and validated write mechanics so both commands use one parser/emitter path.

## Phase Sequence

1. Shared config editor
   - Add a package-private helper for comment-aware YAML creation, root mapping loading, serialization, common node helpers, and validated atomic writes.
   - Move the duplicated helper behavior out of `AddCommand` without changing the accepted add behavior.
2. Add command alignment
   - Update `AddCommand` to use the shared helper for appending an artifact node and writing the candidate config.
   - Preserve existing option parsing, validation, and artifact-node construction behavior.
3. Remove command
   - Add `RemoveCommand` with no command-specific options and exactly one coordinate argument.
   - Parse the requested coordinate through `ArtifactModel`.
   - Load the current model before mutation.
   - Find matching application artifacts by `groupId:artifactId`.
   - Fail unchanged when no match or multiple matches are found.
   - Remove the matching artifact node from the top-level `artifacts` sequence.
   - Leave an empty sequence if the last artifact is removed.
   - Validate and atomically replace the config file through the shared helper.
   - Log the normalized matched coordinate after success.
4. Registration, tests, and release notes
   - Register `remove` in `Main`.
   - Add `RemoveCommand.java` and `RemoveCommandTest.java` to Bazel targets without `glob()`.
   - Extend `MainTest` expected usage output.
   - Update `CHANGELOG.md` under Unreleased.

## High-Risk Areas

- Preserving existing uncommitted SnakeYAML/Add work.
  - Mitigation: read current `AddCommand` and refactor by moving existing behavior, not recreating it from stale plan text.
- Comment-aware serialization shape.
  - Mitigation: keep expected tests aligned with current SnakeYAML output and add remove-specific comment tests.
- Matching YAML nodes back to model artifacts.
  - Mitigation: parse each artifact node's `coord` scalar into `ArtifactModel` and match by group/id.
- Ambiguous duplicate configs.
  - Mitigation: count matching configured artifacts before editing and fail unchanged on more than one.
- Safe writes.
  - Mitigation: reuse candidate validation, temp file, atomic move, and cleanup logic.

## Required Full Gate

```bash
tools/check.sh
```

## Decision Log

- Q-01: Match removal by `groupId:artifactId`, ignoring type, classifier, and version.
- Q-02: Missing matches fail unchanged.
- Q-03: Only top-level `artifacts` is mutated.
- Q-04: Use comment-aware SnakeYAML node mutation.
- Q-05: Removing the last artifact leaves `artifacts: []`.
- Q-06: Accept exactly one coordinate.
- Q-07: Add no command-specific options.
- Q-08: Extract a narrow shared helper for common comment-aware YAML editing and validated writes.
- Q-09: Log a success message using the normalized matched coordinate.
- Q-10: Duplicate matches fail as ambiguous.
- Q-11: Require the existing config to load successfully before editing.
- Q-12: Remove comments attached to the removed artifact item.
- Q-13: Do not run `generate` automatically.

## Plan Finalization State

This plan is accepted. User decisions were captured one question at a time in the parent thread and every recommendation was accepted.

## Implementation Discovery

- Removing an artifact from a valid config does not currently create a schema-level candidate validation failure; focused tests cover successful candidate validation and the invalid-current-config path instead.
