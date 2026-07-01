# Test Speed Requirements

Status: accepted

## Mission

Reduce local and CI test latency while preserving the current test coverage, coverage thresholds, and meaningful integration coverage for Bazel-dependent behavior.

## Scope Boundaries

- In scope:
  - Keep the existing TestNG coverage while reducing avoidable real Bazel invocations in tests.
  - Preserve dedicated integration coverage for code that shells out to Bazel.
  - Batch Bazel info lookups in production code where both values are needed.
  - Remove avoidable duplicate execution from `tools/check.sh` where coverage already exercises the same test suite.
  - Split remaining large TestNG groups when it improves parallelism without reintroducing shared compilation bottlenecks.
  - Update `CHANGELOG.md` if implementation changes developer-facing validation behavior or command behavior.
- Out of scope:
  - Lowering coverage thresholds.
  - Skipping currently covered behavior.
  - Replacing TestNG or Bazel.
  - Introducing Bazel `glob()` usage.
  - Moving source ownership across directory-local `BUILD.bazel` files.

## Locked Decisions

- `tools/check.sh` remains the required full gate before implementation work is claimed complete.
- Tests should not call real Bazel by default when a fake or injected provider can cover the same logic.
- Real Bazel coverage is kept in explicit integration targets.
- The current split TestNG target structure is kept and refined, not reverted to a monolithic target.
- Test source files stay directly attached to the owning test targets unless a shared support library is genuinely shared support code.
- New or changed Bazel targets must list sources explicitly.
- Any `CHANGELOG.md` entry required by a developer-facing check-script or test-target-structure change is made in the same task as that change.

## Command Surface

No public CLI command syntax is expected to change.

Validation command expectations:

```bash
tools/check.sh
```

Targeted commands expected during implementation:

```bash
bazel test //src/test/java/org/realityforge/bazel/depgen/util:bazel_util_integration_test --test_output=errors --cache_test_results=no
bazel test //src/test/java/org/realityforge/bazel/depgen:all_tests --test_output=errors --cache_test_results=no
```

The exact targeted commands may be updated in the task board if target names change during the approved implementation.

## Behavior Expectations

- `BazelUtil` behavior stays equivalent:
  - `output_base` defaults still come from `bazel info output_base`.
  - `repository_cache` defaults still come from `bazel info repository_cache`.
  - Repository cache fallback behavior remains covered.
  - Bazel command failure handling remains covered.
- Unit tests for parsing, fallback selection, and error behavior use fake providers or command runners.
- Integration tests prove the real Bazel command path still works.
- `BazelUtil` test layout is explicit:
  - `BazelUtilTest.java` becomes the fast fake-backed test class and is included by `//src/test/java/org/realityforge/bazel/depgen/util:unit_tests`.
  - `BazelUtilIntegrationTest.java` owns real Bazel process coverage and is included by `//src/test/java/org/realityforge/bazel/depgen/util:bazel_util_integration_test`.
  - `//src/test/java/org/realityforge/bazel/depgen/util:all_tests` includes both targets.
  - `unit_tests` performs zero real Bazel process starts.
  - `bazel_util_integration_test` performs at most two real Bazel process starts: one combined workspace info lookup and, only if still needed for real-path coverage, one default repository-cache lookup.
  - The combined Bazel info reader may be introduced while restructuring `BazelUtil` tests, but `Main` is not wired to use it until the production batching task.
- Batched Bazel info lookup behavior is defined:
  - If both cache directory and repository cache directory are explicit, no Bazel info lookup runs.
  - If only cache directory is missing, only `output_base` is required; failure still fails option processing as it does today.
  - If only repository cache directory is missing, only `repository_cache` is required; failure still falls back to the default repository cache path.
  - If both defaults are needed, one combined `bazel info output_base repository_cache` lookup is used.
  - If the combined lookup fails while the cache directory is required, option processing fails before setting repository cache, preserving the current ordering.
  - If combined output has `output_base` but no `repository_cache`, cache directory is set and repository cache falls back through the existing default-cache path.
  - Main-level tests cover both-missing, one-explicit, output-base failure, missing repository-cache output, and fallback preservation.
- `tools/check.sh` still validates build, tests, coverage, formatting, dependency sync, and buildifier checks.
- Any check-script optimization must be backed by evidence that the removed normal test invocation is still covered by another gate.
- Check-script overlap evidence must include the exact target diff from:
  - `bazel query 'tests(//...)'`
  - `bazel query 'tests(//src/test/java/org/realityforge/bazel/depgen:all_tests)'`
  - `bazel query 'tests(//...) except tests(//src/test/java/org/realityforge/bazel/depgen:all_tests)'`
- Split test targets must remain discoverable through existing aggregate targets.
- A remaining TestNG target is split only when post-optimization evidence shows it is still among the three slowest non-integration targets and either takes at least five uncached seconds or accounts for at least 20% of the aggregate wall time.
- Class-level splits are preferred. Splitting one Java test class by TestNG groups is allowed only when the method groups have clear semantic boundaries and before/after TestNG method counts match.

## Current Evidence

- The split TestNG suite currently expands to 11 runnable targets and preserves 515 TestNG methods.
- The largest remaining TestNG targets by method count are:
  - `record:all_tests`: 160 methods.
  - `cli_tests`: 74 methods.
  - `model:all_tests`: 75 methods.
  - `metadata:all_tests`: 48 methods.
- Recent uncached split-suite timings put `//src/test/java/org/realityforge/bazel/depgen/util:bazel_util_integration_test` on the critical path at roughly 15-18 seconds.
- `BazelUtilTest` covers six test methods, five of which exercise real Bazel shellouts.
- `AbstractTest` creates a fresh `.bazelrc` with a fresh `startup --output_user_root` per test method, preventing Bazel server reuse across `BazelUtilTest` methods.
- `bazel info output_base repository_cache` returns both required values in a single Bazel process in this workspace.
- `tools/check.sh` currently runs the main suite under both normal test configuration and coverage configuration.

## Quality Gates

- Required full gate:
  - `tools/check.sh`
- Focused gates:
  - BazelUtil unit tests after fake-provider extraction.
  - BazelUtil integration target after real Bazel coverage is reduced.
  - Main TestNG aggregate after target-split changes.
  - The optimized `tools/check.sh` path after check-script edits.
- Coverage gate:
  - Existing line and branch coverage checks from `tools/check.sh` must continue to pass.

## Known Intentional Divergences

- Some behavior currently covered by repeated real Bazel shellouts will move to unit tests with fakes. This is intentional only when a smaller integration test still proves the real Bazel path.
- `tools/check.sh` may stop running the main Java suite twice if the coverage run already executes that same suite and coverage thresholds still pass.
- Additional split targets may increase target count to improve parallelism.

## Open Questions Register

### Q-01

- status: resolved
- question: Should the first implementation priority be reducing real Bazel invocations or further splitting TestNG targets?
- context: Existing timing evidence shows the BazelUtil integration target dominates the split-suite critical path, while further target splitting can improve parallelism but does not remove the slowest shellout cost.
- options:
  - Prioritize reducing real Bazel invocations first.
  - Prioritize further TestNG target splitting first.
  - Do both in one broad change.
- tradeoffs:
  - Bazel shellout reduction first: highest measured payoff and cleaner test layering.
  - Target splitting first: lower risk but smaller impact while `BazelUtilTest` remains the critical path.
  - Combined change: fewer commits but harder to validate causality.
- recommended_default: Prioritize reducing real Bazel invocations first.
- user_decision: Inferred from the user's original focus on `bazel info output_base` and `bazel info repository_cache`, plus current timing evidence. No grill-me question was required.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q-02

- status: resolved
- question: Should `tools/check.sh` remain the required full gate?
- context: Repository instructions explicitly require `tools/check.sh` before claiming implementation work complete.
- options:
  - Keep `tools/check.sh` as the full gate.
  - Replace it with narrower Bazel commands.
- tradeoffs:
  - Keeping `tools/check.sh`: aligns with repository policy and preserves formatting, dependency, build, test, and coverage checks.
  - Replacing it: faster locally but violates the repository instruction and loses gate coverage.
- recommended_default: Keep `tools/check.sh`.
- user_decision: Locked by repository instructions.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

## Review History

- Draft created from measured local evidence gathered during the prior test-splitting work.
- Iterative plan review round 1 accepted five findings: changelog sequencing, BazelUtil test layout, batched info semantics, check-script target-diff evidence, and measurable split criteria.
- Iterative plan review round 2 accepted three findings: split ownership for the combined Bazel info reader, removal of subjective split wording, and same-task changelog handling for test-target-structure changes.
- Iterative plan review round 3 accepted one finding: same-task changelog handling for SPEED-02 target membership changes.
- User approved implementation with "implement"; plan marked accepted.
- Implementation completed in `fbe3d092c1b22a4636cf3b9ccfeb7772385ac6a9` and passed `tools/check.sh`.
