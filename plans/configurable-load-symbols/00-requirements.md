# Configurable Load Symbols Requirements

Status: implemented

## Mission

Add global configuration that lets users suppress specific load or binding declarations in direct generated outputs so multiple `dependencies.yml` files can target the same `MODULE.bazel` or `BUILD.bazel` without duplicate load or binding declarations.

## Scope

- Add `options.repositoryRuleLoadSymbols` for repository rule bindings emitted into `MODULE.bazel` when `repositoryRuleGenerationStrategy: module`.
- Add `options.targetRuleLoadSymbols` for target rule loads emitted into `BUILD.bazel` when `targetGenerationStrategy: build`.
- Keep generated call sites using depgen's existing underscored aliases such as `_http_file(...)` and `_java_import(...)`.
- Keep generated `.bzl` extension files self-contained and unaffected by these options.
- Add strict key validation and strategy-level semantic validation.
- Update tests, configuration template documentation, `CHANGELOG.md`, and the existing workaround note in `tools/update_java_deps.sh`.

## Out Of Scope

- Do not auto-detect duplicate symbols across generated sections.
- Do not rewrite user-managed top-level `load(...)` or `use_repo_rule(...)` declarations.
- Do not change generated call-site symbol names.
- Do not solve cross-section repository-name, target-name, or helper-target-name collisions. Users must continue using existing naming controls such as `namePrefix`, `nameStrategy`, `repositoryNameStrategy`, and per-artifact name overrides to keep generated repositories and targets unique.
- Do not migrate this repository's own `dependencies.yml` files to use the new options until a released depgen version containing this feature is used by `tools/update_java_deps.sh`.
- Do not apply symbol maps to generated `.bzl` extension files.

## Locked Decisions

- The feature covers both repository rule bindings and target rule loads.
- Configuration is grouped by load site:
  - `repositoryRuleLoadSymbols`
  - `targetRuleLoadSymbols`
- Map keys are logical Bazel rule names, not depgen aliases.
- Map values are booleans:
  - omitted key: keep current behavior, emitting when depgen needs the symbol
  - `true`: allow normal emission when depgen needs the symbol
  - `false`: suppress the declaration when depgen needs the symbol
- Null map values are invalid and fail syntactic/model validation.
- Empty maps are syntactically valid. They behave like omitted maps when the corresponding direct generation strategy is active, but still count as configured and therefore fail semantic validation when the corresponding strategy is inactive.
- Unknown keys fail syntactic/model validation.
- A symbol map configured for an inactive strategy fails semantic validation:
  - `repositoryRuleLoadSymbols` requires `repositoryRuleGenerationStrategy: module`
  - `targetRuleLoadSymbols` requires `targetGenerationStrategy: build`
- Semantic validation is strategy-level only; it does not fail when a valid configured symbol is unused by the current resolved graph.
- The repo keeps the current released-version workaround for now and adds a note to remove it after the next release is adopted.

## Command Surface And Behavior

Example:

```yaml
options:
  repositoryRuleGenerationStrategy: module
  targetGenerationStrategy: build
  repositoryRuleLoadSymbols:
    http_file: false
    http_archive: true
  targetRuleLoadSymbols:
    java_import: false
    java_binary: true
    j2cl_library: false
```

Supported `repositoryRuleLoadSymbols` keys:

- `http_file`
- `http_archive`

Supported `targetRuleLoadSymbols` keys:

- `java_binary`
- `java_import`
- `java_library`
- `java_plugin`
- `java_test`
- `j2cl_library`

When a symbol is suppressed, depgen omits only the declaration:

- `MODULE.bazel`: omit `_http_file = use_repo_rule(...)` or `_http_archive = use_repo_rule(...)`.
- `BUILD.bazel`: omit the matching alias from `load("@rules_java//java:defs.bzl", ...)` or omit `load("@j2cl//build_defs:rules.bzl", _j2cl_library = "j2cl_library")`.

Generated calls still use aliases and therefore require the user to define them elsewhere in the shared file.

This feature only controls declaration collisions for imported rule symbols. It does not make all generated names globally unique across multiple configuration files.

## Quality Gates

- Targeted tests while implementing:
  - `bazel test //src/test/java/org/realityforge/bazel/depgen/model:all_tests` if available, or the owning test targets for model tests.
  - `bazel test //src/test/java/org/realityforge/bazel/depgen:all_tests` for generator behavior.
- Full gate before claiming implementation complete:
  - `tools/check.sh`

## Known Intentional Divergences

- Extension files keep current load emission even if symbol maps are configured, because the maps are only meaningful for direct injection into shared `MODULE.bazel` or `BUILD.bazel` files.
- The repository's own generated files continue using the released depgen workflow until the next release containing this feature is adopted.
- Shared direct-output files still require existing naming options to avoid repository, target, and helper-target name collisions.

## Open Questions Register

### Q-01

- status: resolved
- question: Should the planned feature cover only `MODULE.bazel` repository bindings or both repository bindings and target-rule loads?
- context: Code inspection showed `MODULE.bazel` duplicate aliases for `use_repo_rule`, and analogous `BUILD.bazel` duplicate aliases for `@rules_java//java:defs.bzl` loads.
- options: A) module only, B) both repository bindings and target loads
- tradeoffs: A is narrower but leaves the analogous Java load problem unsolved. B handles both shared direct-output files coherently.
- recommended_default: B
- user_decision: B
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-02

- status: resolved
- question: Should per-symbol control use exact generated aliases, logical rule identifiers, or grouped source/load-site objects?
- context: The implementation uses private underscored aliases, but users think in terms of imported Bazel rules.
- options: A) exact aliases, B) logical rule identifiers, C) grouped objects
- tradeoffs: Exact aliases expose internal naming. Logical identifiers are simpler but less structured. Grouped objects make source/load-site intent explicit.
- recommended_default: B
- user_decision: C
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-03

- status: resolved
- question: What should an omitted symbol mean in a grouped symbol map?
- context: The feature must preserve current behavior by default and allow partial suppression.
- options: A) omitted means normal emit when needed, false suppresses, true allows; B) omitted means do not emit; C) split include/exclude lists
- tradeoffs: A is backwards-compatible and concise. B requires exhaustive config. C is more verbose and can create conflicting entries.
- recommended_default: A
- user_decision: A
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-04

- status: resolved
- question: Should target-load symbol control cover only `@rules_java//java:defs.bzl`, or all depgen-emitted target rule loads?
- context: `writeTargetLoadsIfRequired` emits both rules_java and J2CL loads.
- options: A) rules_java only, B) all target rule loads, C) separate groups per source
- tradeoffs: A misses J2CL duplicate loads. C adds structure without current need. B covers all emitted target load aliases with one map.
- recommended_default: B
- user_decision: B
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-05

- status: resolved
- question: Should depgen validate unknown symbols in these maps?
- context: Unknown keys are usually typos that would leave duplicate symbols in output.
- options: A) strict validation, B) lenient ignore, C) warn only
- tradeoffs: Strict validation catches typos early. Lenient mode is forward-compatible but hides mistakes. Warnings may be missed.
- recommended_default: A
- user_decision: A
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-06

- status: resolved
- question: When a symbol is set to `false`, should depgen only suppress the declaration, or also change call sites?
- context: Current checked-in files keep call sites on `_http_file(...)` while centralizing the declaration elsewhere.
- options: A) suppress declaration only, B) suppress declaration and switch call sites to public names, C) configure call-site names separately
- tradeoffs: A matches current workaround and keeps generated calls stable. B changes output shape more broadly. C adds extra API surface.
- recommended_default: A
- user_decision: A
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-07

- status: resolved
- question: Should these symbol maps affect only direct injection outputs, or also generated extension files?
- context: Duplicate symbols occur when multiple generated sections share one direct output file.
- options: A) direct injection only, B) all outputs, C) separate config for direct outputs and extension files
- tradeoffs: A avoids breaking self-contained generated `.bzl` files. B could produce broken extensions. C adds unused complexity.
- recommended_default: A
- user_decision: A
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-08

- status: resolved
- question: Should this change update this repo's own dependency configs and remove the existing post-generation workaround?
- context: `tools/update_java_deps.sh` currently strips duplicate module bindings after running the released depgen jar.
- options: A) update configs and remove workaround, B) leave self-use unchanged, C) add config but keep workaround
- tradeoffs: A validates the feature locally but cannot work until the released jar includes the new option. B keeps the current released-version workflow intact. C creates config ignored by the released jar.
- recommended_default: A
- user_decision: B, with a note to remove the workaround after adopting the next release
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-09

- status: resolved
- question: What exact option names should the YAML API use?
- context: Names should align with existing `repositoryRuleGenerationStrategy` and `targetGenerationStrategy`.
- options: A) `repositoryRuleLoadSymbols` and `targetRuleLoadSymbols`, B) shorter `repositoryRuleSymbols` and `targetRuleSymbols`, C) direct-output names
- tradeoffs: A is explicit without tying names to file names. B is shorter but less precise. C is precise but overfits to current file names.
- recommended_default: A
- user_decision: A
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-10

- status: resolved
- question: Should symbol maps be validated even when the corresponding generation strategy is inactive?
- context: Typo checks should be consistent, but inactive maps are semantically unused.
- options: A) always validate known keys, B) validate only when active, C) warn when inactive and error when active
- tradeoffs: Always validating catches typos before strategy changes. Semantic validation can still reject inactive maps later.
- recommended_default: A
- user_decision: A; syntactic/type checks always run, and a later semantic error should report inactive configuration as unused
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-11

- status: resolved
- question: How far should unused-configuration semantic validation go?
- context: Semantic validation could reject inactive strategy maps or graph-unused symbols.
- options: A) strategy-level only, B) per-symbol graph usage, C) strategy-level errors and per-symbol warnings
- tradeoffs: Strategy-level validation catches impossible configuration without making future-facing symbol entries fail as dependencies change.
- recommended_default: A
- user_decision: A
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
