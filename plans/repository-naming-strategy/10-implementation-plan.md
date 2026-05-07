# Implementation Plan: Repository Naming Strategy

## Delivery Approach
Implement the change in five phases:
1. Extend configuration surface and parsing.
2. Split target naming from repository naming in record generation.
3. Add config/model validation for repository naming shape and exclusivity rules.
4. Add emitted-name validation for repository and plugin-private namespaces.
5. Update tests and user-facing docs.

## Phase Sequence
### Phase 1: Config and Model Surface
- Add `GroupIdAndArtifactIdAndVersion` to `NameStrategy`.
- Add `repositoryNameStrategy` to `OptionsConfig`.
- Add `repositoryNameStrategy` and `repositoryName` to `ArtifactConfig`.
- Update config parsing tests and template examples/comments.

### Phase 2: Naming Derivation Refactor
- Split `ArtifactRecord` naming helpers into:
  - target-symbol derivation
  - repository-base derivation
  - plugin-private-name derivation
- Make `repositoryName` absolute and unprefixed when present.
- Validate explicit `repositoryName` directly rather than sanitizing it.
- Make repository-emission methods use repository-base naming only.
- Make plugin private names derive from Java target naming.

### Phase 3: Config/Model Validation
- Reject `repositoryName` + `repositoryNameStrategy` on the same artifact.
- Reject `__` in all user-provided naming overrides:
  - `repositoryName`
  - `java.name`
  - `j2cl.name`
  - `plugin.name`
  - `namePrefix`
- Reject invalid literal `repositoryName` values using the exact shape `[a-z][a-z0-9_]*`.
- Keep these checks in config/model parsing so invalid static configuration fails before resolution.

### Phase 4: Emitted-Name Validation
- Exclude replacement records entirely from emitted-name validation because they do not emit repository rules or local targets.
- Validate repository uniqueness against the exact repository rules that would be emitted for the resolved artifact set.
- Do not treat non-emitted family members as collisions.
- Validate the full emitted Bazel target namespace across:
  - public targets
  - built-in helper targets such as `verify_config_sha256` and `regenerate_depgen_extension`, including prefixed variants when `verifyConfigSha256` is enabled
  - private `__plugin_library` targets
  - private `__plugin` targets, including processor-specific variants
- Replace the existing public target-name uniqueness check with emitted-target validation that excludes replacements and also accounts for helper targets and private plugin names after the derivation split.

### Phase 5: Tests and Docs
- Update `dependencies.yml` template comments.
- Add `CHANGELOG.md` entry under `Unreleased`.
- Add or update tests for:
  - parsing
  - config/model validation
  - default compatibility
  - target naming independence
  - repository naming strategies
  - explicit override precedence
  - explicit `repositoryName` bypassing `namePrefix`
  - `java.name`, `j2cl.name`, `plugin.name`, and `namePrefix` rejecting `__`
  - plugin private naming derivation
  - helper-target namespace collisions, including prefixed helper-target names
  - private plugin namespace collisions
  - emitted-only repository collision scope
  - replacements being ignored by emitted-name collision validation
  - validation errors

## High-Risk Areas and Mitigations
- Risk: existing `getBaseName()` use is overloaded across repository and plugin naming
  - Mitigation: introduce separate helpers and update tests before changing call sites broadly
- Risk: compatibility regression in default repository names
  - Mitigation: add explicit tests proving no-config behavior remains versioned
- Risk: hidden repository-family collisions
  - Mitigation: validate only the repository rules that would actually be emitted, with explicit tests for non-emitted family members
- Risk: moving private plugin names to Java-target derivation changes collision behavior
  - Mitigation: validate the emitted private plugin namespace against public names and against itself
- Risk: enum expansion impacts target naming semantics
  - Mitigation: add direct tests for `nameStrategy: GroupIdAndArtifactIdAndVersion`

## Required Full-Gate Command
- `bundle exec buildr test`

## Decision Log
- `Q-01`: Added independent `repositoryNameStrategy` config instead of broadening `nameStrategy`.
- `Q-02`: Added literal `artifact.repositoryName` escape hatch.
- `Q-03`: Repository name conflicts will fail validation, not auto-resolve.
- `Q-04`: `repositoryName` is literal, absolute, unprefixed, and validated up front using `[a-z][a-z0-9_]*`.
- `Q-05`: Derived repositories use the explicit base plus existing suffixes.
- `Q-06`: Private plugin names derive from Java target naming.
- `Q-07`: User-specified override names and `namePrefix` reject `__`.
- `Q-08`: `GroupIdAndArtifactIdAndVersion` is valid for target naming too.
- `Q-09`: Emitted-name validation excludes replacement records and applies only to emitted repositories and targets.

## Task Breakdown
- `PLAN-01`: finalize planning artifacts and request user review
- `CFG-01`: add config fields and enum support
- `EMIT-01`: refactor naming derivation and repository emission
- `VALIDATE-CFG-01`: add config/model validation rules
- `VALIDATE-EMIT-01`: add emitted-name validation for repository and plugin-private namespaces
- `TEST-01`: add/update tests for config, emission, and validation
- `DOC-01`: update template docs and changelog
- `PLAN-APPROVAL`: capture user review outcome before implementation starts
