# Configurable Load Symbols Implementation Plan

Status: implemented

## Delivery Approach

Implement a narrow configuration feature that affects only direct generated sections. Preserve default output exactly when the new options are absent. Add strict model validation, strategy-level semantic validation, focused generation tests, documentation, changelog entry, and a note documenting when the existing released-version workaround can be removed.

## Phase Sequence

### Phase 1: Config Surface And Model Validation

- Add nullable `Map<String, Boolean>` properties to `OptionsConfig`:
  - `repositoryRuleLoadSymbols`
  - `targetRuleLoadSymbols`
- Add `OptionsModel` accessors that answer whether a rule declaration should be emitted:
  - default to `true` when the symbol key is absent
  - return the configured boolean when present
- Validate allowed keys during `OptionsModel.parse(...)`:
  - repository keys: `http_file`, `http_archive`
  - target keys: `java_binary`, `java_import`, `java_library`, `java_plugin`, `java_test`, `j2cl_library`
- Validate every configured map value is non-null.
- Treat an empty map as configured for inactive-strategy semantic validation, while making it a no-op when the corresponding direct generation strategy is active.
- Add semantic validation in `ApplicationModel.ensureOptionCombinationIsValid()`:
  - `repositoryRuleLoadSymbols` requires `repositoryRuleGenerationStrategy: module`
  - `targetRuleLoadSymbols` requires `targetGenerationStrategy: build`

### Phase 2: Direct Output Generation

- Update `writeRepositoryRuleUseRepoBindingsIfRequired(...)` so `MODULE.bazel` only emits configured-allowed `use_repo_rule` bindings.
- Update `writeTargetLoadsIfRequired(...)` to distinguish extension generation from direct `BUILD.bazel` generation:
  - extension `.bzl` generation keeps current self-contained loads
  - direct `BUILD.bazel` generation filters aliases through `targetRuleLoadSymbols`
- Keep all call sites unchanged:
  - `_http_file(...)`
  - `_http_archive(...)`
  - `_java_*...`
  - `_j2cl_library(...)`

### Phase 3: Tests

- Add config parsing tests for both new maps.
- Add `OptionsModel` tests for defaults, explicit true/false, unknown repository symbol, and unknown target symbol.
- Add `OptionsModel` tests for null map values and empty-map defaults.
- Add `ApplicationModel` tests for unused map semantic errors when the corresponding strategy is inactive.
- Add generation tests for:
  - module output suppressing `_http_file` while still emitting `_http_file(...)` calls
  - module output suppressing only `_http_archive` when J2CL JS assets require archive rules
  - direct build output suppressing a subset of rules_java aliases
  - direct build output suppressing `j2cl_library`
  - default/absent-map extension output remains unchanged
  - inactive-strategy maps are rejected before generation
  - mixed strategies where an active direct-output map does not alter the extension-generated side

### Phase 4: Docs And Repo Workflow Notes

- Update `src/main/resources/org/realityforge/bazel/depgen/templates/dependencies.yml` with concise option documentation and examples.
- Document that symbol suppression only resolves load/binding declaration collisions; users must still keep generated repository names, targets, and helper targets unique with existing naming options.
- Update `CHANGELOG.md` under `Unreleased`.
- Update `tools/update_java_deps.sh` comment near the duplicate-binding workaround to state that it should be removed after the repo adopts the next released depgen version containing configurable load symbols.
- Do not add the new options to this repo's `third_party/java/dependencies.yml` or `tools/java-format/dependencies.yml` yet.

### Phase 5: Validation

- Run targeted tests while iterating.
- Run full gate before completion:
  - `tools/check.sh`

## High-Risk Areas And Mitigations

- Risk: Suppressing a declaration can intentionally produce unresolved aliases if users do not define the symbol elsewhere.
  - Mitigation: Document that suppression only removes declarations and generated call sites still use depgen aliases.
- Risk: Users may assume this feature resolves every collision when multiple configurations share one direct output file.
  - Mitigation: Document that existing naming options remain required for repository names, generated targets, and helper targets.
- Risk: Filtering loads in generated `.bzl` extension files would break self-contained outputs.
  - Mitigation: Apply filtering only to direct `MODULE.bazel` and `BUILD.bazel` sections.
- Risk: Empty load lines after filtering all aliases.
  - Mitigation: Emit a `load(...)` or `use_repo_rule(...)` declaration only when at least one needed symbol remains enabled.
- Risk: Existing tests assert exact output strings.
  - Mitigation: Keep default behavior unchanged and add new tests for filtered behavior.
- Risk: `tools/check.sh` uses the released depgen jar through `tools/update_java_deps.sh`.
  - Mitigation: Keep the workaround in place and add only a removal note until a future release is adopted.

## Required Full Gate

```bash
tools/check.sh
```

## Decision Log

- Q-01: Implement both repository binding and target load symbol controls.
- Q-02: Use grouped source/load-site configuration maps.
- Q-03: Use omitted-as-default, `true` as allow, and `false` as suppress semantics.
- Q-04: Cover rules_java and J2CL target load symbols.
- Q-05: Reject unknown symbol keys.
- Q-06: Suppress declarations only; preserve existing call-site aliases.
- Q-07: Apply maps only to direct injected `MODULE.bazel` and `BUILD.bazel` sections.
- Q-08: Keep repo self-use on released depgen and add a note to remove the workaround after the next release is adopted.
- Q-09: Use option names `repositoryRuleLoadSymbols` and `targetRuleLoadSymbols`.
- Q-10: Always run syntactic/type/key validation; emit semantic errors for inactive-strategy maps.
- Q-11: Limit unused-configuration semantic validation to strategy-level checks.

## Acceptance Criteria

- Existing configurations without the new options generate identical output.
- Users can suppress individual direct `MODULE.bazel` repository rule bindings while preserving generated `_http_*` calls.
- Users can suppress individual direct `BUILD.bazel` target rule load aliases while preserving generated `_java_*` and `_j2cl_library` calls.
- Documentation states that generated repository names, target names, and helper target names must still be made unique with existing naming options when multiple configurations share one output file.
- Unknown symbol keys fail during model validation.
- Null symbol map values fail during model validation.
- Empty symbol maps are no-ops when active and unused-configuration errors when inactive.
- Symbol maps configured for inactive generation strategies fail semantic validation.
- Generated extension files remain self-contained and unaffected.
- Template documentation and changelog describe the new behavior.
- `tools/update_java_deps.sh` documents when its workaround can be removed.
- `tools/check.sh` passes before implementation is claimed complete.
