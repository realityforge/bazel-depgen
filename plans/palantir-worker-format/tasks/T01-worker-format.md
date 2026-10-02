# T01 — Migrate worker formatting

- Status: pending
- Blocked by: None
- Spec coverage: R1–R4, AC1–AC4

## Delivers

Shared pinned formatter ownership and graph-check worker execution through existing CI,
with preserved write command and public watch wrapper, dependency cleanup and reviewed PR.

## Acceptance criteria

- [ ] Root graph check, worker execution and complete intended input coverage (AC1).
- [ ] Read-only negative checks; writer repairs; watcher handles modification (AC2).
- [ ] Remove obsolete plumbing; regenerated lock; Buildifier, syntax, tools/check.sh and diff pass (AC3).
- [ ] Required reviews and plan history complete; tree removed; assigned auto-merge PR verified (AC4).

## Validation

- bazel build //:java_format_check with execution log and graph source audit.
- Intentional dirty production/test/tool source check, source/index hash comparison, writer repair, watch event.
- bash -n wrappers; bazel run //:buildifier_check; tools/check.sh; git diff --check and final diff review.
- Same planning reviewer; fresh implementation reviewer; git history and GitHub PR API state.

## Evidence

pending. AC4 lifecycle evidence is completed during review/closeout/publication, after task implementation checks.
