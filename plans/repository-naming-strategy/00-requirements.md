# Repository Naming Strategy Requirements

## Mission
Add configurable naming strategies for Bazel repository base names, separate from target naming, while preserving current behavior by default and providing an explicit per-artifact repository-name override.

## Scope Boundaries
In scope:
- New global `repositoryNameStrategy` option.
- New per-artifact `repositoryNameStrategy` override.
- New per-artifact `repositoryName` explicit override.
- Validation for conflicting repository naming config.
- Validation for duplicate repository names.
- Validation for forbidden `__` in user-provided naming overrides.
- Tests, template docs, and changelog updates.

Out of scope:
- Changes to replacement dependency behavior.
- Changes to Maven resolution.
- Changes to generated label suffix conventions like `__sources`.
- Arbitrary naming for sources/js/annotations repositories beyond the shared base-name override.

## Locked Decisions
- Target naming and repository naming are separate axes.
- `nameStrategy` continues to control target names.
- New `repositoryNameStrategy` controls repository base names.
- `repositoryName` is a literal per-artifact escape hatch.
- `repositoryName` is absolute: it bypasses `namePrefix`.
- `repositoryName` bypasses sanitization/cleanup and is validated directly rather than normalized.
- `repositoryName` and `repositoryNameStrategy` are mutually exclusive on the same artifact.
- `NameStrategy` will support:
  - `GroupIdAndArtifactIdAndVersion`
  - `GroupIdAndArtifactId`
  - `ArtifactId`
- `OptionsConfig.nameStrategy` default remains `GroupIdAndArtifactId`.
- `OptionsConfig.repositoryNameStrategy` default is `GroupIdAndArtifactIdAndVersion`.
- `NameStrategy.GroupIdAndArtifactIdAndVersion` is valid for target naming too.
- The accepted `repositoryName` shape is `[a-z][a-z0-9_]*`.
- `repositoryName` must not contain `__`.
- The `__` restriction applies to all user-supplied naming overrides:
  - `repositoryName`
  - `java.name`
  - `j2cl.name`
  - `plugin.name`
  - `namePrefix`
- Derived repository names append fixed suffixes to the chosen repository base:
  - `__sources`
  - `__js_sources`
  - `__annotations`
- Private plugin rule names should no longer be derived from repository base naming; they should derive from Java target naming.
- Emitted-name validation excludes replacement records entirely.
- The emitted Bazel target namespace must remain unique across:
  - public targets
  - built-in helper targets such as `verify_config_sha256` and `regenerate_depgen_extension`, including prefixed variants when `namePrefix` is set
  - private `__plugin_library` targets
  - private `__plugin` targets, including processor-specific variants
- Replacement records must not create false public/private target or repository collisions because they do not emit local targets or repository rules.

## Behavior Expectations
- With no repository naming config, generated repository names remain versioned as today.
- Explicit repository naming config changes repository naming only, not target naming.
- Explicit target naming config changes target naming only, not repository naming.
- `repositoryName` wins over generated repository naming for that artifact.
- `repositoryName` is not prefixed by `namePrefix`.
- `repositoryName` must already be a valid identifier and will fail validation if not.
- When `repositoryName` is used, derived repositories reuse the same base with fixed suffixes.
- Repository name collisions must fail validation before emitting Bazel.
- Repository uniqueness is checked against the exact repository rules that would be emitted for the resolved artifact set, not against theoretical family members that would not be emitted.
- Example: an artifact whose repository base is `foo` only conflicts with another artifact named `foo__sources` if the `foo` artifact would actually emit a sources repository.
- Plugin internal names must remain deterministic and stable within the generated extension.
- Plugin internal names must be validated against public names, built-in helper targets, and against each other before Bazel is emitted.
- Replacement records are excluded from emitted-name validation for repositories, public targets, and private plugin targets.

## Quality Gates
- Unit tests for config parsing.
- Unit tests for config/model validation.
- Unit tests for artifact/application record naming behavior.
- Unit tests for emitted-repository collision behavior, including non-emitted family-member non-conflicts.
- Unit tests for internal plugin target namespace collisions.
- Unit tests for helper-target namespace collisions, including prefixed helper-target names.
- Unit tests proving replacements do not trigger false emitted-name conflicts.
- Template documentation update in `dependencies.yml`.
- `CHANGELOG.md` update under `Unreleased`.
- Full-gate command: `bundle exec buildr test`.

## Known Intentional Divergences
- None currently expected.

## Open Questions Register
- `Q-01`
  - `status`: resolved
  - `question`: Should target and repository naming share one setting?
  - `context`: Existing `nameStrategy` only controls target names; repository names are versioned separately.
  - `options`: shared setting, shared-with-compatibility-rules, separate settings
  - `tradeoffs`: shared setting reduces surface area but couples unrelated behaviors; separate settings preserve clarity and compatibility
  - `recommended_default`: separate settings
  - `user_decision`: separate `nameStrategy` and `repositoryNameStrategy`
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-02`
  - `status`: resolved
  - `question`: What is the repository-name escape hatch shape?
  - `context`: Need a literal per-artifact override distinct from nature-specific target names.
  - `options`: `repositoryName`, top-level `name`, nested repository block
  - `tradeoffs`: `repositoryName` is explicit and least ambiguous
  - `recommended_default`: `repositoryName`
  - `user_decision`: add `artifact.repositoryName`
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-03`
  - `status`: resolved
  - `question`: How should repository collisions be handled?
  - `context`: Versionless or explicit names can collide.
  - `options`: validation error, auto-fallback, auto-suffix
  - `tradeoffs`: validation is deterministic and easiest to reason about
  - `recommended_default`: validation error
  - `user_decision`: validation error
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-04`
  - `status`: resolved
  - `question`: How should explicit `repositoryName` be interpreted?
  - `context`: Need to choose between literal, normalized, or deferred validation semantics.
  - `options`: literal + validate, normalize, literal no validation
  - `tradeoffs`: literal + validate is strict and predictable
  - `recommended_default`: literal + validate
  - `user_decision`: literal + validate
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-05`
  - `status`: resolved
  - `question`: How should derived repositories behave with `repositoryName`?
  - `context`: Sources/js/annotations repos share the same base today.
  - `options`: fixed suffix family, override binary only, separate explicit fields
  - `tradeoffs`: fixed suffix family keeps the model compact
  - `recommended_default`: fixed suffix family
  - `user_decision`: fixed suffix family
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-06`
  - `status`: resolved
  - `question`: What should private plugin rule names derive from?
  - `context`: Current code reuses the versioned repository-style base for `__plugin` names.
  - `options`: Java target name, Plugin target name, leave current behavior
  - `tradeoffs`: Java target name matches the implementation jar/import and decouples plugin internals from repository naming
  - `recommended_default`: Java target name
  - `user_decision`: Java target name
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-07`
  - `status`: resolved
  - `question`: Should `__` be forbidden in user-provided names?
  - `context`: Generated names use `__`, but user overrides using `__` can collide with generated suffix families.
  - `options`: overrides only, all names, no ban
  - `tradeoffs`: overrides-only protects naming space without redesigning generated formats
  - `recommended_default`: overrides only
  - `user_decision`: reject `__` in user-provided override names and `namePrefix`
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
- `Q-08`
  - `status`: resolved
  - `question`: Should `GroupIdAndArtifactIdAndVersion` be valid for target naming too?
  - `context`: Reusing `NameStrategy` means the target-side property can parse the new enum value.
  - `options`: reject on targets, support on targets, separate enums
  - `tradeoffs`: supporting it avoids type-level inconsistency and surprise
  - `recommended_default`: support on targets
  - `user_decision`: support on targets
  - `artifacts_updated`: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
