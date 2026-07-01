# Test Speed Phase 2 Requirements

Status: accepted

## Mission

Reduce the remaining local and CI validation latency after the completed `plans/test-speed` work, while preserving test coverage, coverage thresholds, and meaningful integration coverage for Bazel and Maven-resolution behavior.

## Scope Boundaries

- In scope:
  - Reduce repeated test fixture setup cost in the record and metadata tests.
  - Keep resolver coverage for consuming Maven-layout repositories.
  - Make shared test setup cheaper when real Bazel isolation is not needed.
  - Remove small known waits in HTTP-backed tests when they are not semantically required.
  - Consolidate avoidable Bazel startup calls in `tools/check.sh` and dependency-sync tooling.
  - Re-measure before deciding whether deeper TestNG class or method splits are still justified.
  - Update `CHANGELOG.md` for developer-facing validation behavior or target-structure changes.
- Out of scope:
  - Lowering coverage thresholds.
  - Removing covered behavior to gain speed.
  - Replacing Bazel or TestNG.
  - Introducing Bazel `glob()` usage.
  - Moving source ownership across directory-local `BUILD.bazel` files.
  - Reworking production resolver semantics solely for test speed.

## Locked Decisions

- `tools/check.sh` remains the required full gate before claiming implementation work complete.
- Existing coverage thresholds remain unchanged.
- Existing aggregate test targets must keep discovering all split targets.
- Real Bazel coverage remains isolated in explicit integration tests.
- New or changed Bazel targets must list sources explicitly and obey directory-local `BUILD.bazel` ownership.
- The completed `plans/test-speed/` tree is historical state and must not be rewritten for this phase-two plan.

## Command Surface

No public CLI command syntax is expected to change.

Validation command expectations:

```bash
tools/check.sh
```

Targeted commands expected during implementation:

```bash
bazel test //src/test/java/org/realityforge/bazel/depgen/record:all_tests --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen:resolver_tests --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen/metadata:record_util_tests --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen/metadata:record_util_tests --test_output=errors --cache_test_results=no --runs_per_test=5
bazel test //src/test/java/org/realityforge/bazel/depgen/util:bazel_util_integration_test --test_output=errors --cache_test_results=no
bazel coverage //src/test/java/org/realityforge/bazel/depgen:all_tests --combined_report=lcov --instrumentation_filter='^//src/main/java/org/realityforge/bazel/depgen[/:]' --cache_test_results=no
```

The exact targeted commands may be revised in `20-task-board.yaml` as tasks are activated.

## Behavior Expectations

- Tests that resolve dependencies from local Maven repositories continue to exercise production resolver behavior.
- Any faster fixture writer must generate repository content equivalent enough for the existing resolver-backed tests to pass without special production code paths.
- Tests should not write or start Bazel infrastructure unless the test actually needs real Bazel behavior.
- `BazelUtilIntegrationTest` remains the explicit owner of real Bazel process coverage.
- Check-script optimizations must preserve buildifier, Java format, dependency sync, build, test, coverage generation, and coverage-threshold validation.
- Any target split must keep before/after TestNG method coverage equivalent.
- `CHANGELOG.md` updates for developer-facing check-script or target-structure changes are made in the same task as the behavior change, not deferred to final cleanup.

## Fixture Writer Contract

- The direct Maven-layout fixture writer prototype must generate fixed-version local repository paths equivalent to Maven's default layout:
  - artifact file: `<group path>/<artifactId>/<version>/<artifactId>-<version>[-classifier].<extension>`
  - POM file: `<group path>/<artifactId>/<version>/<artifactId>-<version>.pom`
- `deployArtifactToLocalRepository(...)` must continue creating both the main artifact and its implicit `sources` artifact.
- `deployTempArtifactToLocalRepository(...)` must preserve supplied file bytes for custom jar/source/annotation fixtures.
- Generated POMs must preserve the existing helper behavior for dependencies with type, classifier, scope, optional flag, and system scope.
- Missing-artifact scenarios must remain possible by not writing an artifact fixture.
- Maven metadata and checksum files are intentionally not required for the fixed-version local repository fixtures unless an existing test proves they are needed.
- Focused validation must include existing resolver-backed coverage for:
  - sources and annotations classifiers,
  - custom jar files,
  - dependency type/classifier/scope/optional/system cases,
  - missing artifact lookup,
  - multi-repository lookup.

## Timing Measurement Protocol

- Before retaining the fixture-writer prototype, run the same uncached `record:all_tests` command before and after the change three times each.
- Compare median elapsed time from the Bazel output; record critical-path timing when Bazel reports it.
- The prototype satisfies the speed threshold if the after median improves by at least 10%.
- The prototype satisfies the structural threshold only if per-artifact Maven Resolver deployment sessions are eliminated, targeted tests pass, coverage passes, and the after median is not slower by more than 5%.
- If the prototype fails both thresholds, revert or abandon that implementation slice and record the no-change decision in the task board.

## Check-Script Interface Contract

- `tools/check.sh` may consolidate `execution_root` and `output_base` with one multi-key `bazel info execution_root output_base` call.
- `tools/update_java_deps.sh` must remain runnable as a standalone script.
- `tools/check.sh` passes a precomputed output base to `tools/update_java_deps.sh` via a `BAZEL_OUTPUT_BASE` environment variable.
- `tools/update_java_deps.sh` must retain its current fallback of calling `bazel info output_base` when `BAZEL_OUTPUT_BASE` is not set.
- Validation for this task must cover both `tools/check.sh` and direct dependency-sync usage.

## Target-Splitting Criteria

- Additional target splitting is considered only after setup optimizations are measured.
- A target qualifies for splitting only when post-setup evidence shows it is among the three slowest main Java targets and either:
  - takes at least five uncached seconds, or
  - accounts for at least 20% of aggregate elapsed time or reported critical-path time.
- If no target meets this threshold, record a no-split decision instead of changing target topology.

## Current Evidence

- `bazel query 'tests(//...)'` currently reports 19 test targets: 15 main Java test targets, `//third_party/java:verify_config_sha256`, and three release-tool tests.
- Recent uncached full-suite timing from the second analysis pass:
  - `bazel test //... --test_output=errors --cache_test_results=no` passed in `13.316s` with `13.16s` critical path.
  - Slowest targets were `application_record_tests` (`13.0s`), `bazel_util_integration_test` (`9.4s`), `main_tests` (`8.2s`), and `artifact_record_tests` (`8.0s`).
- Recent uncached coverage timing from the second analysis pass:
  - `bazel coverage //src/test/java/org/realityforge/bazel/depgen:all_tests --combined_report=lcov --instrumentation_filter='^//src/main/java/org/realityforge/bazel/depgen[/:]' --cache_test_results=no` passed in `19.203s` with `16.78s` critical path.
  - Slowest coverage targets were `bazel_util_integration_test` (`13.8s`), `main_tests` (`10.6s`), `application_record_tests` (`9.7s`), `record_util_tests` (`7.7s`), and `artifact_record_tests` (`5.9s`).
- Current TestNG method counts from source:
  - `ApplicationRecordTest.java`: 116 methods.
  - `MainTest.java`: 60 methods.
  - `ArtifactRecordTest.java`: 44 methods.
  - `RecordUtilTest.java`: 22 methods.
  - `BazelUtilIntegrationTest.java`: 2 methods.
- Current fixture-deployment call counts from source:
  - `ApplicationRecordTest.java`: 213 `deploy*ArtifactToLocalRepository(...)` calls.
  - `ArtifactRecordTest.java`: 61 `deploy*ArtifactToLocalRepository(...)` calls.
  - `MainTest.java`: 16 `deploy*ArtifactToLocalRepository(...)` calls.
  - `RecordUtilTest.java`: 7 `deploy*ArtifactToLocalRepository(...)` calls.
- `AbstractTest.run()` writes `.bazelrc` before every TestNG method, and `AbstractTest.writeBazelrc()` creates a fresh Bazel `output_user_root`.
- `AbstractTest.deployTempArtifactToLocalRepository(...)` currently uses Maven Resolver `DeployRequest` to create local repository fixtures.
- `RecordUtilTest` contains three `server.stop(1)` calls.
- `tools/check.sh` calls `bazel info execution_root`; `tools/update_java_deps.sh` separately calls `bazel info output_base`.

## Quality Gates

- Required full gate:
  - `tools/check.sh`
- Focused gates:
  - Record test aggregate after fixture-writer changes.
  - Metadata `record_util_tests` after HTTP shutdown changes.
  - Repeated metadata `record_util_tests` run after HTTP shutdown changes.
  - Util integration target after Bazel integration test changes.
  - Coverage command after any change affecting test topology or fixture semantics.
- Coverage gate:
  - Existing `tools/check.sh` line and branch thresholds must continue to pass.

## Known Intentional Divergences

- Phase two may prototype direct Maven-layout fixture writes in tests instead of using Maven Resolver deployment APIs for fixture construction. This is intentionally limited to test fixture setup and must still validate production resolver consumption of the generated repository.
- The direct fixture writer prototype is kept only if `record:all_tests` improves by at least 10% uncached or the change eliminates the per-artifact Maven Resolver deployment session with no test or coverage regression.

## Open Questions Register

### Q-01

- status: resolved
- question: Should phase two include replacing Maven Resolver-based test fixture deployment with direct Maven-layout file writes?
- context: The slowest remaining record tests repeatedly build local Maven repositories. `AbstractTest.deployTempArtifactToLocalRepository(...)` currently creates a resolver and uses `DeployRequest` for every synthetic artifact. The heaviest tests call these helpers hundreds of times, while the production behavior under test is mostly resolver consumption of the resulting repository, not Maven deployment itself.
- options:
  - Include direct Maven-layout fixture writes as the first implementation slice.
  - Do not change fixture deployment; focus only on target splitting and smaller setup tweaks.
  - Prototype direct fixture writes in a dedicated task and keep the change only if targeted tests and timing evidence justify it.
- tradeoffs:
  - Direct writes first: likely highest payoff and keeps resolver-read coverage, but it changes the test fixture construction path and must accurately generate Maven paths, POMs, classifiers, and source artifacts.
  - Avoiding fixture changes: lowest semantic risk, but leaves the largest repeated setup cost in place.
  - Prototype first: gives measured confidence before committing to the design, but adds an explicit decision checkpoint before later tasks.
- recommended_default: Prototype direct Maven-layout fixture writes as the first task, then keep it only if record tests pass and timing improves materially. This gets evidence without committing to a broad rewrite up front.
- user_decision: Option 1 selected. Prototype direct Maven-layout fixture writes as the first implementation slice and keep the change only if targeted tests pass and timing evidence justifies it.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-02

- status: resolved
- question: What evidence threshold should decide whether the direct Maven-layout fixture writer prototype is kept?
- context: Q-01 approved a prototype rather than an unconditional fixture rewrite. The task needs an objective keep/drop rule before implementation starts, otherwise a passing but marginally faster prototype could become a subjective call. Existing timing evidence shows `record:application_record_tests` and `record:artifact_record_tests` are among the slowest targets, and they contain most fixture deployment calls.
- options:
  - Keep the prototype if the uncached `record:all_tests` elapsed time improves by at least 20% and all targeted tests pass.
  - Keep the prototype if either `record:all_tests` improves by at least 10% or the implementation clearly removes the per-artifact Maven Resolver deployment session while all targeted tests pass.
  - Keep the prototype if tests pass, regardless of measured speedup.
- tradeoffs:
  - 20% threshold: strong evidence of value, but may reject a clean structural improvement whose benefit is split across coverage and full-check runs.
  - 10% or eliminated deployment session: pragmatic for this codebase because the current hotspot is known repeated fixture setup, but still prevents no-op churn.
  - Tests-pass-only: easiest to apply, but does not match the stated speedup goal.
- recommended_default: Keep the prototype if `record:all_tests` improves by at least 10% uncached or the change eliminates the per-artifact Maven Resolver deployment session with no test or coverage regression. This balances measurable speedup with the structural goal Q-01 approved.
- user_decision: Option 2 selected. Keep the prototype if `record:all_tests` improves by at least 10% uncached, or if the implementation eliminates the per-artifact Maven Resolver deployment session with no test or coverage regression.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

## Review History

- Draft created as phase two after `plans/test-speed` completed in `fbe3d092c1b22a4636cf3b9ccfeb7772385ac6a9`.
- Q-01 resolved by user selecting option 1: prototype direct Maven-layout fixture writes first.
- Q-02 resolved by user selecting option 2: use the 10% uncached record-suite improvement or eliminated per-artifact deployment-session threshold.
- Iterative plan review round 1 accepted six findings: same-task changelog sequencing, direct fixture-writer contract, timing measurement protocol, repeated HTTP shutdown validation, check-script cross-script interface, and no-op completion criteria for conditional tasks.
- Iterative plan review round 2 accepted three findings: align task-board fixture keep/drop criteria with the 5% slowdown cap, define quantitative target-splitting criteria, and specify `BAZEL_OUTPUT_BASE` as the cross-script output-base interface.
- User approved implementation with "implement in subagent"; plan marked accepted.
- Implementation completed in `ac3943f42e43ea2d42fcdfe056801fc1cb54a06e`; final `tools/check.sh` passed with line coverage 95.10% and branch coverage 85.39%.
