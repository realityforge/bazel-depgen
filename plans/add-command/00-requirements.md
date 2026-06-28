# Add Command Requirements

Status: accepted

## Mission

Add an `add` CLI command that appends a Maven dependency artifact entry to the configured `dependencies.yml` without rewriting unrelated configuration.

## Scope Boundaries

- In scope:
  - Register `add` in the main command map.
  - Parse one required coordinate argument and modern artifact option flags.
  - Insert a new artifact under the top-level `artifacts:` section.
  - Preserve existing comments and unrelated YAML text as much as practical.
  - Validate command arguments and the resulting configuration.
  - Update `TODO.md` and tests.
- Out of scope:
  - Removing or updating existing dependencies.
  - Resolving latest versions.
  - Adding repositories.
  - Running `generate` automatically by default.
  - Rewriting the full YAML document through `YamlUtil`.

## Locked Decisions

- `add` is a config-edit command by default. It does not run `generate`.
- The command uses the current config vocabulary, not old alias terminology.
- `--alias` is not supported.
- Nature values map to existing enum values: `Java`, `Plugin`, `J2cl`.
- Repeated singular flags are preferred: `--nature`, `--repository`, `--exclude`, `--visibility`, `--j2cl-suppress`.
- Per-nature names are explicit: `--java-name`, `--j2cl-name`, `--plugin-name`.
- Java export behavior is nested under the Java nature via `--java-export-deps`.
- Plugin API generation is nested under the Plugin nature via `--plugin-generates-api true|false`.
- J2CL options are nested under the J2CL nature via `--j2cl-mode` and `--j2cl-suppress`.
- Coordinates use the same 2-5 component forms accepted by `ArtifactConfig`.
- `add` does not resolve or infer versions. A 2-part `groupId:artifactId` coordinate is allowed for the same customization/transitive-exposure use cases supported by the configuration model.
- Duplicate detection uses the declared artifact key `groupId:artifactId`, regardless of coordinate arity, type, classifier, or version.
- Nature-specific flags do not imply natures. They require the resolved nature set to include the matching nature. The resolved nature set comes from explicit `--nature` flags when present, otherwise from `options.defaultNature`.
- Option parsing only records requested flags. Validation that depends on config defaults, including nature-specific flag checks against `options.defaultNature`, occurs in `run` after the existing configuration has been loaded.

## Command Surface

Baseline shape:

```text
add [coord]
  [--nature Java|Plugin|J2cl]...
  [--name-strategy GroupIdAndArtifactIdAndVersion|GroupIdAndArtifactId|ArtifactId]
  [--repository-name name]
  [--repository-name-strategy GroupIdAndArtifactIdAndVersion|GroupIdAndArtifactId|ArtifactId]
  [--include-optional]
  [--include-source]
  [--no-include-source]
  [--include-external-annotations]
  [--repository name]...
  [--exclude group[:artifact]]...
  [--visibility label]...
  [--java-name name]
  [--java-export-deps]
  [--j2cl-name name]
  [--j2cl-mode Library|Import]
  [--j2cl-suppress check]...
  [--plugin-name name]
  [--plugin-generates-api true|false]
```

## Behavior Expectations

- If `artifacts:` is absent, create it at the end of the file.
- If `artifacts:` exists, append the new artifact to the existing list.
- If an artifact with the same `groupId:artifactId` is already declared in `artifacts:`, fail with a clear error and leave the file unchanged.
- Emit only explicitly requested options; rely on existing defaults otherwise.
- Keep the generated snippet valid for `ApplicationConfig.load`.
- Ignore commented sample keys such as `#artifacts:` when locating insertion points.
- Treat only an uncommented top-level `artifacts:` key as the artifact section.
- When appending to an existing `artifacts:` section, insert before the next uncommented top-level key.
- Support only block-style artifact sections: `artifacts:` followed by list entries, or a missing section. Unsupported inline or non-list forms such as `artifacts: []` or `artifacts: value` fail with a clear error and leave the file unchanged.
- Preserve a trailing newline.
- Report the config file path changed.
- Use the configured file from `--config-file` or the existing default resolution.
- Generate candidate file content in memory and run all duplicate and model validation before replacing the real config file.
- Write through a temporary file in the config directory and use atomic replace where supported.
- Delete temporary validation/write files after success and failure.
- `--include-source` and `--no-include-source` are mutually exclusive explicit artifact overrides. Omit both to rely
  on `options.includeSource`.
- A dependency with the J2CL nature must have `includeSource` resolve to `true`.
- `--j2cl-suppress` is rejected with `--j2cl-mode Import`, matching generation-time validation.

## Quality Gates

- Targeted tests:
  - `bundle exec buildr test:AddCommandTest`
  - `bundle exec buildr test:MainTest`
- Full gate:
  - `bundle exec buildr test`
- Required focused coverage:
  - default template with only commented `#artifacts:` sample content
  - existing artifacts section followed by another top-level section
  - config with no artifacts section
  - unsupported inline/non-list artifacts section leaves the original file unchanged
  - duplicate declarations across 2-part, 3-part, typed, and classified coordinate forms
  - per-nature flag without a matching resolved nature
  - validation failure leaves the original file unchanged
  - failure paths do not leave temporary files in the config directory

## Known Intentional Divergences

- The old TODO mentioned `--alias`; current code and changelog indicate aliasing is obsolete.
- The old TODO used plural `--excludes`; the plan uses repeated singular `--exclude`.
- The old TODO had no repository flags; current schema supports per-artifact `repositories`.
- The original TODO mentioned `--include-source`; implementation keeps that positive override as well as
  `--no-include-source` so J2CL artifacts can be added when the global `options.includeSource` default is `false`.

## Open Questions Register

### Q-01

- status: resolved
- question: What should `add` do if the dependency already exists in `artifacts:`?
- context: The command can identify duplicates by declared artifact key, usually `groupId:artifactId`. Adding a second matching entry can make the config harder to reason about, but updating an existing entry risks surprising edits across nested options.
- options:
  - Fail with a clear error and leave the file unchanged.
  - Update the existing artifact entry in place.
  - Append a second entry and rely on later validation.
- tradeoffs:
  - Fail: simplest, safest, no accidental config mutation; user must edit or use a future `update` command.
  - Update: convenient, but needs deeper YAML editing semantics and conflict rules for every option.
  - Append: easiest mechanically, but likely creates ambiguous config and worse user experience.
- recommended_default: Fail on duplicate and point users toward manual edit or the future `update` command.
- user_decision: Fail with a clear error and leave the file unchanged.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

## Review History

- Round 1 plan review findings accepted and incorporated:
  - Model validation must be explicit before writing.
  - YAML insertion rules must ignore commented sample sections and stop at the next top-level key.
  - Nature-specific flag behavior must be explicit.
  - File replacement mechanics must preserve unchanged files on failure.
  - Coordinate arity and duplicate-key behavior must be recorded.
  - Task board status must not block plan approval on a stale in-progress planning task.
- Round 2 plan review findings accepted and incorporated:
  - Nature-specific validation must run after config loading, not during raw option parsing.
  - Unsupported inline/non-list `artifacts:` shapes must fail unchanged.
  - Temporary validation/write files must be cleaned up on success and failure.
- Implementation discovery:
  - Added `--include-source` alongside `--no-include-source` because `options.includeSource` can be globally false
    while J2CL artifacts require source artifacts.
