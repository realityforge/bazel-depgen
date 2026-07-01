# Missing Source Dependency Trace Requirements

Plan status: accepted.

## Mission

Improve the validation error emitted when source artifacts are required but unavailable so the user can see why the failing artifact is present in the resolved dependency graph.

## Scope Boundaries

In scope:

- Append a concise root-to-artifact dependency path to the existing missing-source validation error.
- Cover both generic `includeSource` failures and J2CL required-source failures.
- Cover direct root dependencies and transitive dependencies.
- Reuse the existing dependency graph node formatting where practical so scopes, optional markers, managed versions, conflicts, and target overrides stay consistent with `print-graph`.
- Add targeted regression tests for the diagnostic text.
- Update `CHANGELOG.md` and remove or revise the resolved `TODO.md` entry.

Out of scope:

- Changing source artifact resolution behavior.
- Adding a new CLI flag or changing command-line syntax.
- Emitting the full dependency graph in this validation error.
- Changing `print-graph` behavior except for any internal formatter extraction that preserves output.
- Broad refactors outside the dependency graph formatting and validation path.

## Locked Decisions And Non-Negotiables

- Keep the first sentence of the existing missing-source error intact for continuity.
- Emit a shortest resolved path from a declared root dependency to the artifact whose source artifact is missing.
- Use non-system resolved root dependencies as path roots; do not let the automatic depgen system artifact become the displayed explanation root.
- Format the path as a compact multiline block headed by `Dependency path:`.
- Match the failing artifact by its resolved `DependencyNode`, not by a lossy `groupId:artifactId` string.
- Do not add fallback compatibility code for old behavior.
- Follow repository Bazel rules: no `glob()`, each directory owns its own explicit `BUILD.bazel` sources.
- Run `tools/check.sh` before claiming implementation complete.

## Command Surface And Behavior Expectations

- `generate`, `hash`, `print-graph`, and other commands retain their current command-line surface.
- Commands that currently fail during `ApplicationRecord.build()` for missing sources still fail, but the validation message includes the dependency path.
- J2CL artifacts that fail because required sources are unavailable still use their existing J2CL-specific explanation, with the dependency path appended.
- `print-graph` remains a separate command and is not used as a workaround for this failure mode.
- Existing generated dependency graph comments remain unchanged except for any deliberately tested formatter-preserving refactor.

## Expected Error Output

Direct dependency:

```text
Unable to locate source for artifact 'com.example:myapp:jar:1.0'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.

Dependency path:
  com.example:myapp:jar:1.0 [compile]
```

Transitive dependency:

```text
Unable to locate source for artifact 'colt:colt:jar:1.2.0'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.

Dependency path:
  tapestry:tapestry:jar:4.0.2 [compile]
  -> colt:colt:jar:1.2.0 [compile]
```

J2CL required-source dependency:

```text
Unable to locate the sources classifier artifact for the artifact 'com.example:widget:jar:1.0' but the artifact has the J2cl nature which requires that sources be present.

Dependency path:
  com.example:app:jar:1.0 [compile]
  -> com.example:widget:jar:1.0 [compile]
```

## Acceptance Criteria

- Missing source errors for direct dependencies include the existing guidance plus a one-node dependency path.
- Missing source errors for transitive dependencies include the existing guidance plus a root-to-failing-artifact path.
- J2CL required-source errors include the existing J2CL-specific guidance plus a dependency path.
- The dependency path uses the same artifact/scope formatting conventions as `DependencyGraphEmitter`.
- At least one diagnostic regression exercises non-trivial graph formatting, such as replacement or optional text, to prove the diagnostic uses the shared model-aware formatter.
- Path tests cover the presence of the automatic depgen system artifact and verify the displayed path still starts from the relevant non-system root.
- Existing `print-graph` tests continue to pass.
- Targeted tests cover direct and transitive missing-source diagnostics.
- `CHANGELOG.md` records the user-visible diagnostic improvement under `Unreleased`.
- `TODO.md` no longer lists this item once implementation is complete.

## Quality And Coverage Gates

Targeted gates:

- `bazel test //src/test/java/org/realityforge/bazel/depgen:core_tests`
- `bazel test //src/test/java/org/realityforge/bazel/depgen:command_output_tests`
- `bazel test //src/test/java/org/realityforge/bazel/depgen/record:application_record_tests`

Required full gate:

- `tools/check.sh`

## Known Intentional Divergences

- The validation error will show only the shortest dependency path, not the full graph. This keeps the diagnostic focused on the question "why is this artifact included?"

## Open Questions Register

### Q-01

- status: resolved
- question: Should the diagnostic append a shortest dependency path or emit the full dependency graph?
- context: The current missing-source error names the artifact but does not explain why a transitive artifact is present. The repository already has full graph formatting, but a full graph can be noisy in an exception.
- options:
  - Append the shortest root-to-artifact path.
  - Emit the full dependency graph.
  - Leave the current error unchanged and rely on `print-graph`.
- tradeoffs:
  - Shortest path directly answers the inclusion question with minimal noise.
  - Full graph gives maximum context but can bury the actionable path.
  - Relying on `print-graph` is insufficient because validation fails before `print-graph` can emit a graph.
- recommended_default: Append the shortest root-to-artifact path.
- user_decision: Use the proposed shortest dependency path diagnostic.
- artifacts_updated:
  - `plans/missing-source-dependency-trace/00-requirements.md`
  - `plans/missing-source-dependency-trace/10-implementation-plan.md`
  - `plans/missing-source-dependency-trace/20-task-board.yaml`
