# Add Command Implementation Plan

Status: accepted

## Delivery Approach

Implement `add` as a minimal config-edit command that appends a valid artifact YAML snippet while preserving the rest of the file. Avoid whole-document serialization because `YamlUtil.asYamlString(...)` emits plain YAML and would discard template comments.

## Phase Sequence

1. CLI and model construction
   - Add `AddCommand`.
   - Parse the command surface from `00-requirements.md` and store requested flags.
   - Defer validation that depends on loaded config defaults until `run`.
   - Build an `ArtifactConfig` from parsed arguments.
   - Accept 2-5 component coordinates using the same parsing rules as `ArtifactModel`.
2. YAML insertion
   - Implement focused insertion into `dependencies.yml`.
   - Create `artifacts:` at EOF when no uncommented top-level `artifacts:` key exists.
   - Append to an existing uncommented top-level `artifacts:` section when present.
   - Insert before the next uncommented top-level key after `artifacts:`.
   - Ignore commented sample lines such as `#artifacts:`.
   - Reject unsupported inline or non-list artifact section forms, including `artifacts: []` and `artifacts: value`, without modifying the file.
   - Preserve unrelated comments and text.
3. Validation and duplicate handling
   - Load the original config.
   - Resolve the effective nature set from explicit `--nature` flags or `options.defaultNature`.
   - Reject per-nature flags when the resolved nature set does not include the matching nature.
   - Reject source and J2CL option combinations that generation-time validation can prove invalid without dependency
     resolution.
   - Reject dependencies whose `groupId:artifactId` is already declared in `artifacts:`.
   - Leave the file unchanged on duplicate or validation failure.
   - Generate candidate content in memory.
   - Load the candidate through `ApplicationConfig.load` from a temporary validation path.
   - Construct `ApplicationModel` from the candidate so artifact naming, repository references, coordinate shape, and option combinations are validated before write.
   - Write candidate bytes to a temporary file in the config directory, then replace the real file atomically where supported.
   - Clean up temporary validation/write files on success and failure.
4. Tests and documentation
   - Add `AddCommandTest`.
   - Extend `MainTest` usage expectations.
   - Update `TODO.md` when the command is implemented.
   - Add `CHANGELOG.md` Unreleased entry because this is user-facing behavior.

## High-Risk Areas

- Comment preservation in YAML.
  - Mitigation: append text surgically instead of serializing the entire config; test the default template's commented `#artifacts:` sample.
- Duplicate artifact behavior.
  - Mitigation: fail on duplicate `groupId:artifactId` and leave the file unchanged.
- Per-nature option mismatch.
  - Mitigation: parse flags first, then validate nature-specific flags in `run` after loading config and resolving explicit `--nature` values or `options.defaultNature`.
- Unsupported artifact section shapes.
  - Mitigation: support only block-style `artifacts:` sections and fail unchanged for inline/non-list forms.
- Coordinate arity ambiguity.
  - Mitigation: support existing 2-5 component config forms and document that `add` never infers missing versions.
- Validation requiring network/artifact resolution.
  - Mitigation: validate syntax/model separately from dependency resolution; targeted tests should use local fixtures where resolution is required.
- Source handling for J2CL artifacts.
  - Mitigation: support both `--include-source` and `--no-include-source`, then reject J2CL artifacts when the
    effective per-artifact/global `includeSource` value resolves to `false`.
- Java 17 lint settings.
  - Mitigation: follow existing command/test patterns and run the full Buildr gate.

## Required Full Gate

```bash
bundle exec buildr test
```

## Decision Log

- Q-01: Resolved. `add` fails on duplicate `groupId:artifactId` declarations and leaves the config file unchanged. This keeps the command append-only and reserves in-place mutation for a future `update` command.
- Review Round 1: Accepted findings requiring explicit candidate model validation, top-level uncommented `artifacts:` insertion rules, strict nature-specific flag checks, temp-file replacement, coordinate arity policy, and task-board status cleanup.
- Review Round 2: Accepted findings requiring deferred config-dependent nature validation, explicit rejection of inline/non-list `artifacts:` shapes, and temp-file cleanup on success and failure.
- Implementation discovery: Added `--include-source` because global `options.includeSource` can be false, and added
  deterministic J2CL/source and J2CL suppress/import checks before writing.

## Plan Finalization State

This plan is accepted. User approval was recorded after iterative plan review completed with no remaining findings.
