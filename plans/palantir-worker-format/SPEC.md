# Palantir worker formatting spec

## Source and completed design tree

The user explicitly authorizes migration, commits, PR publication, assignment to realityforge,
and auto-merge without bypassing CI. The user waives grill confirmation and delivery entry
approval when repository/reference evidence settles decisions. No unresolved decision remains.

- Enforcement → preserve existing CI → CI runs tools/check.sh → wrapper check builds root worker check.
- Coverage → user settles graph-only Java → root production binary, all_tests, release tools → audit aspect inputs against Java targets.
- Dependency → public rules module v0.1.1 → BCR HTTP 404 → verified immutable archive SRI.
- Editing → preserve write default and check mode → public write tool with existing src/tools roots;
  add thin watch wrapper with the same roots as the requested reference.
- Safety → check never writes sources → negative dirty source check and unchanged source/index proof.
- Cleanup → delete local formatter dependency graph and regeneration branch → regenerate module lock.
- Delivery → same planning reviewer, fresh implementation reviewer → plan/implementation/closeout commits → assigned PR and auto-merge.

Repository evidence: BUILD.bazel all_tests, source/test/release BUILD files, tools/java_format.sh,
tools/update_java_deps.sh, tools/check.sh, .github/workflows/ci.yml, MODULE.bazel and .bazelrc.
Release v0.1.1 archive independently hashed to
sha256-4h/h0cVmPQ/7jkXnvW69rmABbfXOOzoXuBh3gMJDMk8=; its aspect follows deps/runtime_deps/exports/tests,
selects JavaInfo-owned workspace source Java only, and exposes public check/write/watch APIs.

## Problem and required outcome

Formatting currently invokes a locally maintained formatter binary and dependency graph.
Replace its ownership with the shared Bazel rules and enforce formatting through reusable
PalantirJavaFormat worker actions in existing CI.

## Scope and constraints

Only graph-reachable Java requires checking. Preserve write/check command conventions and
existing src/tools write scope. Generated/external inputs are excluded by the shared aspect;
no extra file enumeration or scratch-copy coverage is added. Audit specialized consumers and
preserve them only if actually present. No CI expansion, unrelated upgrades or formatting churn.
No glob() or cross-directory source ownership. Run tools/check.sh. Update Unreleased changelog.

## Requirements and acceptance criteria

- R1 / AC1: Existing CI formatting gate builds a root testonly java_format_check with remediation
  tools/java_format.sh write. Action log proves PalantirJavaFormat worker execution, using
  worker,local strategy and max instances 1. Coverage audit proves intended graph source coverage.
- R2 / AC2: Shared public write/watch tools accept the existing src/tools roots through wrappers;
  check rejects an intentionally dirty reachable source and lists filename/remediation without
  modifying source or staging it. Write repairs that source. Watch observes a modification.
- R3 / AC3: Immutable verified rules module replaces obsolete tools/java-format files, MODULE block
  and update_java_deps branch. Lockfile regenerated. Buildifier, shell syntax and tools/check.sh pass;
  generated dependency outputs remain unchanged. No unrelated Java diffs.
- R4 / AC4: Read-only planning and implementation reviews pass, temporary tree is committed then
  removed in closeout. PR is attached, assigned realityforge, auto-merge requested with allowed
  method and verified state; settings/CI are never bypassed. Precise blockers are reported.

## Significant decisions

| ID | Decision | Rationale | Impact | User verification |
| --- | --- | --- | --- | --- |
| D1 | Graph-only check with explicit binary, test suite and release-tool roots | User clarification; aspect only traverses supported graph edges | Out-of-graph Java excluded | Inspect action-input coverage evidence |
| D2 | Pin v0.1.1 release override and formatter 2.93.0 | BCR lacks module; existing formatter already 2.93.0 | Shared module owns dependencies; transitive rules_java resolves from 9.6.1 to required 9.9.0 | Inspect module and generated lock diff |
| D3 | Preserve wrappers and source roots; add public-tool watch wrapper | Existing convention and requested reference | Developer commands remain simple; checks are read-only | Run write/check/watch wrappers |
| D4 | Keep existing CI invocation | Existing workflow already enforces tools/check.sh | No new workflow needed | Inspect CI logs |
| D5 | Set target Java language version 17 | Write executable uses records; default 11 fails; repository already targets --release 17 | Public write/watch compile on established Java 17 baseline; runtime settings preserved | Run write/watch and full gate |

The first write probe confirmed the shared public executable fails under Bazel's default target
source level 11 (records require >=16). Existing local Java rules already use --release 17,
and CI explicitly supplies JDK 17. Set build --java_language_version=17 to align the target
toolchain with that established baseline; leave runtime selection and tool JDK 25 unchanged.

## Testing decisions

Focused buildifier and format check precede full tools/check.sh. Capture execution JSON and
worker log; compare Java action inputs to configured graph sources. Temporarily dirty one
production, test and tooling source in sequence or together, prove failure/read-only behavior,
repair through writer and confirm pass. Exercise watch on a reachable source and stop it.
Preserve and restore the worktree/index exactly after probes. Inspect final diff and regenerated locks.

## Open questions

None.
