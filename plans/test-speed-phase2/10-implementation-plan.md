# Test Speed Phase 2 Implementation Plan

Status: accepted

## Delivery Approach

Use small, measurable changes against the remaining hotspots. Prefer removing repeated fixture/setup overhead before creating more test targets. Re-measure after each high-impact slice so later class or method splits are only done if the data still justifies them.

The plan is accepted. User approval was recorded after iterative plan review completed with no remaining findings.

## Phase Sequence

1. Resolve fixture-construction strategy
   - Q-01 is resolved: prototype direct Maven-layout fixture writes first.
   - Q-02 is resolved: keep the prototype if `record:all_tests` improves by at least 10% uncached or if the implementation eliminates the per-artifact Maven Resolver deployment session with no test or coverage regression.
2. Optimize local Maven repository fixture construction
   - Prototype replacing per-artifact Maven Resolver deployment in test fixtures with direct Maven-layout file writes.
   - Follow the fixture-writer contract in `00-requirements.md`.
   - Preserve existing helper method names where useful so test bodies stay focused on behavior.
   - Validate with record tests, resolver-heavy tests, and coverage.
   - Record three before and three after uncached `record:all_tests` timings, compare median elapsed time, and apply the resolved Q-02 keep/drop threshold.
3. Make shared Bazel test setup opt-in
   - Stop writing `.bazelrc` and creating a fresh Bazel `output_user_root` for every `AbstractTest` method by default.
   - Keep real Bazel isolation explicit for `BazelUtilIntegrationTest`.
   - Validate util integration and the main aggregate.
4. Remove known HTTP shutdown waits
   - Change `RecordUtilTest` authenticated HTTP server shutdown to avoid unnecessary one-second waits if targeted validation is stable.
   - Validate `record_util_tests` with `--runs_per_test=5` to catch lifecycle instability.
5. Shrink remaining real Bazel integration cost
   - Reassess `BazelUtilIntegrationTest` after opt-in Bazel setup.
   - Keep only the minimum real Bazel process coverage needed for confidence.
   - Move any remaining default-cache behavior to fake-backed unit coverage if still redundant.
   - If no safe minimization remains, record the no-change decision and timing evidence instead of forcing a code change.
6. Consolidate check-script Bazel info calls
   - Replace separate `bazel info execution_root` and `bazel info output_base` calls with one multi-key query if shell parsing remains simple and robust.
   - Keep `tools/update_java_deps.sh` standalone by accepting `BAZEL_OUTPUT_BASE` from `tools/check.sh` while falling back to `bazel info output_base` when invoked directly.
   - Keep dependency sync and coverage report path behavior equivalent.
   - Update `CHANGELOG.md` in this same task if the check-script behavior change is developer-facing.
7. Re-measure and split only remaining justified targets
   - Run the main aggregate uncached and the coverage command uncached.
   - Split `ApplicationRecordTest`, `MainTest`, `ArtifactRecordTest`, or `RecordUtilTest` only if post-setup evidence shows the target is among the three slowest main Java targets and either takes at least five uncached seconds or accounts for at least 20% of aggregate elapsed time or reported critical-path time.
   - Prefer physical class splits over method-list targets unless method-list targets are clearly lower risk for the remaining bottleneck.
   - If no target still justifies splitting, record the no-split decision and timing evidence.
   - Update `CHANGELOG.md` in this same task if target-structure changes are made.
8. Final validation and planning closeout
   - Run targeted commands recorded in the task board.
   - Run `tools/check.sh`.
   - Verify `CHANGELOG.md` was updated in the same task as any developer-facing validation behavior or test target structure change.
   - Record evidence and commit metadata per task.

## High-Risk Areas

- Direct Maven-layout fixture writes may accidentally omit repository details previously handled by Maven Resolver deployment.
  - Mitigation: keep the change isolated, run resolver-backed record tests, and compare generated path/classifier/POM behavior through existing assertions.
- Removing default `.bazelrc` setup may allow an accidental real Bazel call to use the ambient workspace environment.
  - Mitigation: make real Bazel setup explicit only for tests that require it, and keep fake-backed BazelUtil unit tests proving default behavior.
- HTTP shutdown changes may create port or thread lifecycle instability.
  - Mitigation: run `record_util_tests` uncached after the change and rerun if lifecycle failures appear.
- Check-script info consolidation may make shell parsing brittle.
  - Mitigation: keep parsing minimal and validate `tools/check.sh`.
- Additional target splitting may increase maintenance cost without material speedup.
  - Mitigation: split only after post-setup timing evidence shows the target still matters.

## Required Full Gate

```bash
tools/check.sh
```

## Targeted Validation

```bash
bazel test //src/test/java/org/realityforge/bazel/depgen/record:all_tests --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen:resolver_tests --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen/metadata:record_util_tests --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen/metadata:record_util_tests --test_output=errors --cache_test_results=no --runs_per_test=5
bazel test //src/test/java/org/realityforge/bazel/depgen/util:bazel_util_integration_test --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen:all_tests --test_output=errors --cache_test_results=no
bazel coverage //src/test/java/org/realityforge/bazel/depgen:all_tests --combined_report=lcov --instrumentation_filter='^//src/main/java/org/realityforge/bazel/depgen[/:]' --cache_test_results=no
```

Additional focused commands will be recorded in `20-task-board.yaml` as each task is activated.

## Commit Boundaries

- `SPEED2-01`: Planning, Q-01 resolution, and baseline evidence.
- `SPEED2-02`: Local Maven repository fixture construction optimization, if approved by Q-01.
- `SPEED2-03`: Opt-in Bazel test setup.
- `SPEED2-04`: RecordUtil HTTP shutdown cleanup.
- `SPEED2-05`: BazelUtil integration target minimization, if still justified by timing.
- `SPEED2-06`: Check-script Bazel info consolidation.
- `SPEED2-07`: Post-setup target splits, only if timing still justifies them.
- `SPEED2-08`: Final validation, changelog verification, and task-board closeout.

## Decision Log

- Q-01: Resolved. User selected option 1: prototype direct Maven-layout fixture writes first and keep only with passing targeted tests and timing evidence.
- Q-02: Resolved. User selected option 2: keep the fixture-writer prototype if `record:all_tests` improves by at least 10% uncached, or if the implementation eliminates the per-artifact Maven Resolver deployment session with no test or coverage regression.
- Review Round 1: Accepted findings requiring same-task changelog sequencing, explicit direct fixture-writer contract, three-run median timing protocol, repeated HTTP shutdown validation, a standalone-safe `tools/update_java_deps.sh` interface, and no-op completion criteria for conditional tasks.
- Review Round 2: Accepted findings aligning task-board fixture keep/drop criteria with the 5% slowdown cap, restoring quantitative target-splitting criteria, and specifying `BAZEL_OUTPUT_BASE` as the output-base handoff from `tools/check.sh` to `tools/update_java_deps.sh`.

## Plan Finalization State

This plan is accepted and implemented in `ac3943f42e43ea2d42fcdfe056801fc1cb54a06e`. Final validation passed through `tools/check.sh`, and the attempted post-setup target split was not retained because it regressed the required coverage path.
