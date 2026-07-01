# Missing Source Dependency Trace Implementation Plan

Plan status: accepted.

## Phase Sequence

1. Preserve and expose dependency node formatting.
2. Compute a root-to-artifact path from the resolved dependency graph.
3. Append the path to missing-source validation errors and cover direct, transitive, and J2CL required-source cases.
4. Update user-facing docs and run required gates.

## Delivery Approach

- Execute one task at a time with minimal diffs.
- Start with formatter reuse so the diagnostic can rely on existing graph conventions.
- Keep path computation inside the record layer where both `ApplicationRecord.getNode()` and `ArtifactRecord.getNode()` are available.
- Seed path computation from non-system resolved root dependencies so the automatic depgen system artifact is not shown as the reason a user dependency is present.
- Validate with targeted Bazel tests while iterating.
- Run `tools/check.sh` before marking the implementation complete.

## Detailed Plan

### Phase 1: Formatter Reuse

- Refactor `DependencyGraphEmitter` so model-aware node formatting is reusable without changing existing graph output.
- Prefer a small public static formatter method on `DependencyGraphEmitter` over a new class unless implementation clarity requires extraction.
- Keep `DependencyGraphEmitterTest` and `PrintGraphCommandTest` output stable.
- Add or update a test that proves the shared formatter still carries model-aware decorations such as replacement or optional markers when used by diagnostics.

### Phase 2: Dependency Path Computation

- Add an `ApplicationRecord` helper that accepts an `ArtifactRecord` and returns the shortest path of `DependencyNode` instances from a non-system graph root child to the artifact record node.
- Traverse the resolved `DependencyNode` graph breadth-first so multiple roots or converging dependencies choose the shortest path.
- Match the destination by `DependencyNode` identity.
- Add a regression case where the automatic depgen system artifact is present and the reported path still begins at the non-system root dependency.
- Format the resulting path as:

```text
Dependency path:
  root:root-artifact:jar:1.0 [compile]
  -> transitive:artifact:jar:1.0 [compile]
```

### Phase 3: Validation Message

- Update the generic missing-source branch in `ArtifactRecord.validate()` to append the dependency path block.
- Update the J2CL required-source branch in `ArtifactRecord.validate()` to append the same dependency path block.
- Keep the existing first sentence unchanged:

```text
Unable to locate source for artifact '<artifact>'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.
```

- Add a direct dependency regression expectation to the existing missing-source test.
- Add a transitive missing-source test using a fixture where the root dependency has sources and the transitive dependency does not.
- Add a J2CL required-source test using a fixture where the J2CL artifact has no sources.

### Phase 4: Documentation And Gates

- Add an `Unreleased` changelog entry for the improved missing-source diagnostic.
- Remove or revise the resolved `TODO.md` item.
- Run targeted tests:
  - `bazel test //src/test/java/org/realityforge/bazel/depgen:core_tests`
  - `bazel test //src/test/java/org/realityforge/bazel/depgen:command_output_tests`
  - `bazel test //src/test/java/org/realityforge/bazel/depgen/record:application_record_tests`
- Run full gate:
  - `tools/check.sh`

## High-Risk Areas

- Risk: Changing graph node formatting could alter generated dependency graph comments or `print-graph` output.
  - Impact: Existing snapshots and generated outputs could change unexpectedly.
  - Mitigation: Reuse the exact existing formatting logic and run `core_tests` plus command output tests through the full gate.

- Risk: Dependency graph traversal could pick a confusing path when multiple roots include the same artifact.
  - Impact: The diagnostic could be valid but less useful.
  - Mitigation: Use breadth-first traversal to return the shortest path and preserve resolved graph order.

- Risk: The automatic depgen system artifact could appear as the path root.
  - Impact: The diagnostic would explain an internal helper dependency instead of the user's declared dependency.
  - Mitigation: Seed path traversal from non-system root children and cover this with a regression test.

- Risk: Generic missing-source and J2CL required-source validation branches could diverge.
  - Impact: One source-missing failure mode could remain opaque.
  - Mitigation: Append the shared path block from both validation branches and add branch-specific tests.

- Risk: The validation error currently has exact-string tests.
  - Impact: Tests must be updated intentionally without weakening diagnostics.
  - Mitigation: Assert the full expected direct and transitive messages, including the path block.

## Required Full Gates

`tools/check.sh`

## Decision Log

### Q-01

- Decision: Append the shortest root-to-artifact dependency path instead of dumping the full graph.
- Plan effect: Implement BFS path calculation in `ApplicationRecord` from non-system roots, append a compact path block in both missing-source branches in `ArtifactRecord.validate()`, and avoid new CLI flags or broad graph dumping.

## Completion Criteria

- All planned implementation tasks are complete.
- No open questions remain.
- User review has been requested and any feedback has been incorporated before plan acceptance.
- Targeted test evidence is recorded in `20-task-board.yaml`.
- `tools/check.sh` passes and evidence is recorded.
- Completed tasks have commit metadata or `not_required`.
- Working tree is clean unless the user explicitly asks to defer commit.
