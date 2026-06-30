# Remove Command Requirements

Status: accepted

## Mission

Add a `remove` CLI command that removes one direct dependency artifact from the configured `dependencies.yml`.

## Scope Boundaries

- In scope:
  - Register `remove` in the main command map.
  - Parse exactly one required coordinate argument.
  - Match configured artifacts by `groupId:artifactId`.
  - Remove the matching item from the top-level `artifacts` sequence.
  - Preserve YAML comments using the same SnakeYAML comment-aware node editing approach used by `add`.
  - Validate the current config before mutation and validate the candidate config before replacing the file.
  - Add focused tests and update release notes.
- Out of scope:
  - Removing global excludes, replacements, generated Bazel output, caches, or transitive dependencies.
  - Running `generate` automatically.
  - Batch removal of multiple coordinates.
  - Command-specific options such as `--force`, `--dry-run`, or `--prune-empty-artifacts`.

## Locked Decisions

- The command surface is `remove [coord]`.
- A coordinate may use any 2-5 component form accepted by `ArtifactModel`.
- Matching ignores type, classifier, and version and uses only `groupId:artifactId`.
- Missing coordinates fail during option processing.
- Extra positional arguments fail during option processing.
- If no configured artifact matches, the command fails and leaves the config unchanged.
- If multiple configured artifacts match the same `groupId:artifactId`, the command fails as ambiguous and leaves the config unchanged.
- `remove` only mutates the top-level `artifacts` sequence.
- `remove` uses comment-aware SnakeYAML node mutation, matching the current `add` implementation.
- Existing inline sequence-style `artifacts` values are supported through the SnakeYAML node path.
- Removing the last artifact leaves an empty `artifacts: []` sequence instead of deleting the top-level key.
- Comments attached to the removed artifact item are removed with that item.
- Comments attached to the `artifacts` section or other entries remain.
- The current configuration must load successfully before mutation.
- The candidate configuration must load successfully before replacing the real file.
- File replacement uses the same temp-file and atomic-replace behavior as `add`.
- The command logs an INFO success message using the normalized coordinate from the matched existing artifact.
- The command does not run `generate` automatically.

## Command Surface

```text
remove [coord]
```

## Behavior Expectations

- Load the existing model before any mutation.
- Parse the requested coordinate with the same artifact coordinate rules used by configured artifacts.
- Find matching application artifacts by `groupId:artifactId`.
- Fail clearly if no match exists.
- Fail clearly if more than one match exists.
- Compose the YAML document with `processComments` enabled.
- Locate the top-level `artifacts` entry and require it to be a YAML sequence.
- Remove exactly one matching artifact node from the sequence.
- Serialize with comment processing enabled.
- Validate the candidate by loading `ApplicationConfig` and constructing `ApplicationModel`.
- Replace the config file only after candidate validation succeeds.
- Clean up temporary files after success and failure.

## Quality Gates

- Targeted tests:
  - `bazel test //src/test/java/org/realityforge/bazel/depgen:all_tests --test_filter=org.realityforge.bazel.depgen.RemoveCommandTest`
  - `bazel test //src/test/java/org/realityforge/bazel/depgen:all_tests --test_filter=org.realityforge.bazel.depgen.MainTest`
- Required full gate:
  - `tools/check.sh`

## Open Questions Register

### Q-01

- status: resolved
- question: Should `remove [coord]` remove by `groupId:artifactId`, ignoring version/type/classifier if supplied?
- context: `add` treats dependencies as unique by `groupId:artifactId`.
- options: match by `groupId:artifactId`; match exact full coordinate.
- tradeoffs: group/id matching aligns with `add`; exact matching is narrower but inconsistent with duplicate detection.
- recommended_default: Match by `groupId:artifactId`.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-02

- status: resolved
- question: Should a non-matching coordinate fail or be a successful no-op?
- context: Silent no-op can hide typos.
- options: fail unchanged; no-op success.
- tradeoffs: failing is stricter and safer; no-op is idempotent but less diagnostic.
- recommended_default: Fail unchanged.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-03

- status: resolved
- question: Should `remove` only edit top-level `artifacts`?
- context: Other config sections can reference dependency behavior but are not direct dependencies.
- options: mutate only `artifacts`; also prune related sections.
- tradeoffs: scoped mutation is predictable; pruning related sections is risky and policy-heavy.
- recommended_default: Mutate only `artifacts`.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-04

- status: resolved
- question: Should `remove` use comment-aware SnakeYAML node mutation?
- context: `add` now uses SnakeYAML comment processing instead of manual text patching.
- options: use SnakeYAML nodes; use textual deletion.
- tradeoffs: shared node editing keeps command behavior consistent; textual deletion preserves raw formatting but duplicates YAML handling.
- recommended_default: Use comment-aware SnakeYAML node mutation.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-05

- status: resolved
- question: When removing the last artifact, should `artifacts` remain as an empty sequence?
- context: Deleting the whole section can also delete section comments.
- options: leave `artifacts: []`; remove the key.
- tradeoffs: empty sequence is the smallest node mutation; key removal may be cleaner but can lose section comments.
- recommended_default: Leave `artifacts: []`.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-06

- status: resolved
- question: Should `remove` accept multiple coordinates?
- context: The requested command shape and `add` both use one coordinate.
- options: one coordinate; many coordinates.
- tradeoffs: one coordinate is simple and consistent; many coordinates require batch failure semantics.
- recommended_default: One coordinate.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-07

- status: resolved
- question: Should `remove` have command-specific options?
- context: Strict default behavior covers the first version.
- options: no options; add flags such as `--force` or `--dry-run`.
- tradeoffs: no options keeps the surface minimal; flags add behavior branches before proven need.
- recommended_default: No command-specific options.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-08

- status: resolved
- question: Should `add` and `remove` share the comment-aware YAML editing helpers?
- context: The SnakeYAML setup and validated atomic write path are fragile enough to avoid duplication.
- options: extract a narrow helper; duplicate helpers in each command.
- tradeoffs: a narrow helper reduces duplication; duplicated code avoids abstraction but increases drift.
- recommended_default: Extract a package-private helper.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-09

- status: resolved
- question: Should `remove` log a success message?
- context: `add` logs after a successful write.
- options: log normalized removed coordinate; remain silent.
- tradeoffs: logging mirrors `add`; silence gives less confirmation.
- recommended_default: Log the normalized matched coordinate.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-10

- status: resolved
- question: Should duplicate matching entries be removed or treated as ambiguous?
- context: Duplicate `groupId:artifactId` entries are outside the intended model.
- options: fail ambiguous; remove all; remove first.
- tradeoffs: fail unchanged avoids broad destructive edits; remove all may surprise; remove first hides ambiguity.
- recommended_default: Fail ambiguous.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-11

- status: resolved
- question: Should the existing config have to load successfully before mutation?
- context: Command flow normally validates the current model first.
- options: require load first; allow mutation as a repair operation.
- tradeoffs: load first is predictable; repair mode expands scope.
- recommended_default: Require current model load first.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-12

- status: resolved
- question: Should comments attached to the removed artifact item be removed with it?
- context: Item comments usually describe that dependency.
- options: remove item comments; attempt to preserve them elsewhere.
- tradeoffs: removing item comments avoids orphaned comments; preserving them requires policy for relocation.
- recommended_default: Remove comments attached to the removed item.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-13

- status: resolved
- question: Should `remove` run `generate` automatically?
- context: `add` is a config-edit command.
- options: do not run generate; run generate after edit.
- tradeoffs: no generate matches `add`; automatic generate broadens side effects.
- recommended_default: Do not run `generate`.
- user_decision: Accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
