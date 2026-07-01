# Test Speed Implementation Plan

Status: accepted

## Delivery Approach

Use small, measurable changes that preserve coverage while separating fast unit behavior from explicit Bazel integration coverage. Start with the current critical path, then remove duplicate full-gate work, then split any remaining large TestNG targets where the evidence still supports it.

## Phase Sequence

1. Baseline and target map
   - Record current target list and timing evidence for the split suite.
   - Confirm which targets are executed by normal `bazel test //...` and by the coverage command in `tools/check.sh`.
   - Capture the expected coverage thresholds before implementation changes.
2. Reduce real Bazel shellouts in tests
   - Introduce a minimal seam around Bazel info execution, using the existing code style.
   - Move parsing, fallback, and failure behavior into unit tests that use fakes.
   - Move fast fake-backed coverage into `BazelUtilTest.java` under `util:unit_tests`.
   - Move real Bazel process coverage into `BazelUtilIntegrationTest.java` under `util:bazel_util_integration_test`.
   - Keep `util:all_tests` wired to both `unit_tests` and `bazel_util_integration_test`.
   - Keep the integration target to at most two real Bazel process starts: one combined workspace info lookup and one default repository-cache lookup only if still needed for real-path coverage.
   - This phase may introduce the combined Bazel info reader in `BazelUtil`; `Main` wiring remains in phase 3.
   - Update `CHANGELOG.md` in this same task if the target membership change is developer-facing.
   - Avoid broad abstraction; only isolate the command boundary needed to stop default tests from shelling out repeatedly.
3. Batch production Bazel info lookups
   - Wire `Main` to the combined Bazel info reader when both defaults are needed.
   - Keep single-value behavior available only where it is directly used.
   - Preserve the current `Main` option-processing order: cache-dir failure still stops processing before repository-cache derivation.
   - Extend tests to prove both-missing, one-explicit, output-base failure, missing repository-cache output, and fallback behavior.
4. Optimize `tools/check.sh`
   - Record `bazel query 'tests(//...)'`, `bazel query 'tests(//src/test/java/org/realityforge/bazel/depgen:all_tests)'`, and the `except` query that identifies non-overlapping tests.
   - Adjust normal test execution from that exact target diff so the main Java suite is not run twice when coverage already runs it.
   - Keep non-covered tests and all non-test gates in the script.
   - Update `CHANGELOG.md` in this same task if the resulting check behavior is developer-facing.
   - Re-run the full script and record before/after behavior.
5. Evaluate and split remaining large TestNG targets if still useful
   - Split `record:all_tests` first if it still meets the measurable split threshold after earlier changes.
   - Evaluate `cli_tests`, `model:all_tests`, and `metadata:all_tests` after the BazelUtil work.
   - Prefer class-level splits; use TestNG groups inside one class only when method clusters are semantically clear and method counts are unchanged.
   - Keep direct `srcs` on each test target and preserve aggregate `all_tests` targets.
   - Update `CHANGELOG.md` in this same task if target-structure changes are developer-facing.
6. Final validation and cleanup
   - Run targeted tests for each changed area.
   - Run `tools/check.sh`.
   - Verify any required `CHANGELOG.md` entries were already added in the same task as the developer-facing change.
   - Commit each validated implementation slice with task-board evidence.

## High-Risk Areas

- Accidentally reducing real integration coverage.
  - Mitigation: keep an explicit Bazel integration target and require it in targeted gates.
- Changing fallback behavior for repository cache resolution.
  - Mitigation: unit tests cover command success, missing key, command failure, and fallback paths.
- Parsing multi-key `bazel info` output incorrectly.
  - Mitigation: use structured line parsing keyed by Bazel info field names and add representative tests.
- Making `tools/check.sh` faster by losing validation.
  - Mitigation: prove target overlap with `bazel query` or command evidence before editing, and preserve all non-overlapping checks.
- Over-splitting targets.
  - Mitigation: split only targets that meet the recorded timing threshold and avoid new shared test libraries that force broad invalidation.
- Coverage regression from test-target reshaping.
  - Mitigation: run the existing coverage gate through `tools/check.sh`.

## Required Full Gate

```bash
tools/check.sh
```

## Targeted Validation

```bash
bazel test //src/test/java/org/realityforge/bazel/depgen/util:bazel_util_integration_test --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen:all_tests --test_output=errors --cache_test_results=no
```

Additional focused commands will be recorded in `20-task-board.yaml` as each task is activated.

## Commit Boundaries

- `SPEED-01`: Baseline evidence and planning updates only, no production behavior change.
- `SPEED-02`: Bazel info provider extraction, fast unit coverage, reduced integration shellouts, and same-task changelog entry if required.
- `SPEED-03`: Batched production Bazel info lookup.
- `SPEED-04`: `tools/check.sh` duplicate-suite optimization and same-task changelog entry if required.
- `SPEED-05`: Additional high-density TestNG target splits and same-task changelog entry if required, or a recorded no-split decision if timing evidence no longer justifies them.

## Decision Log

- Q-01: Resolved. Prioritize reducing repeated real Bazel invocations before further target splitting because the current critical path is `BazelUtilTest` shellout time.
- Q-02: Resolved. `tools/check.sh` remains the required full gate because repository instructions make it mandatory before claiming implementation complete.
- Review Round 1: Accepted findings requiring same-task changelog sequencing for check-script changes, explicit BazelUtil unit/integration target layout, batched Bazel info behavior cases, exact check-script query diff evidence, and measurable target-splitting criteria.
- Review Round 2: Accepted findings clarifying that `BazelUtil` may introduce the combined reader before `Main` uses it, removing subjective split wording, and requiring same-task changelog handling for developer-facing test-target-structure changes.
- Review Round 3: Accepted finding requiring same-task changelog handling for SPEED-02 target membership changes.

## Plan Finalization State

This plan is completed. User approval was recorded after iterative plan review completed, the final review finding was incorporated, implementation landed in `fbe3d092c1b22a4636cf3b9ccfeb7772385ac6a9`, and `tools/check.sh` passed.
