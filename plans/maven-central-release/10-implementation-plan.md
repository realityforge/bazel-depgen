# Maven Central Release Implementation Plan

## Plan Status

Accepted on 2026-06-30 after iterative plan review.

## Already Completed Packaging Phases

The existing Bazel packaging implementation remains in place:

1. Release package foundation:
   - version build setting;
   - generated `config.properties`;
   - release artifact labels.
2. Artifact builders:
   - classes jar;
   - sources jar;
   - Javadocs jar;
   - curated executable `-all` jar;
   - POM;
   - signed dist directory and upload zip.
3. All-jar integration test:
   - runs `java -jar`;
   - creates a fresh local Maven repository;
   - exercises `init`, `add`, and `generate`.
4. Maven bundle wrapper:
   - `tools/package_maven_central.sh`;
   - GPG defaults compatible with old Buildr flow.

This plan adds the release lifecycle orchestration around those completed pieces.

## Ordered Phase Sequence

### Phase 1: Plan Approval

- Update `plans/maven-central-release` with release lifecycle requirements, decision log, and task board.
- Request explicit user review.
- Do not mark the plan accepted until review feedback is incorporated.

### Phase 2: Java Release Lifecycle Helper

Add a strict-macro Java helper under `tools/release/org/realityforge/bazel/depgen/release` for local structured release
data operations.

Helper responsibilities:

- derive latest previous version from `CHANGELOG.md`;
- derive the next old-Buildr-style version;
- patch `CHANGELOG.md` from `Unreleased` to `v<version>`;
- insert a fresh post-release `Unreleased` section idempotently;
- extract release notes for `v<version>`;
- patch README from previous version to target version;
- fail with clear messages when expected sections or version references are absent;
- optionally parse Central Portal JSON if shell parsing would otherwise become brittle.

Expose the helper as `//tools/release:release_lifecycle` and keep the package-path implementation target private to the
release tool package. Add tests for the helper behavior and wire them into `//tools/release:all_tests`.

### Phase 3: Release Shell Orchestration

Add release scripts:

- `tools/release/check_ready.sh`
  - runs `tools/check.sh`;
  - runs `git diff --check`;
  - scans tracked Java source files for TODO comments matching old `tasks/todo.rake` intent;
  - verifies `gpg`, `curl`, `gh`, `gh auth status`, `GPG_USER`, and `MAVEN_CENTRAL_PASSWORD`.
- `tools/release/next_version.sh`
  - read-only wrapper around the Java helper.
- `tools/release/prepare_release.sh <version> [--dry-run]`
  - validates explicit non-snapshot version;
  - requires a clean git tree unless dry-run;
  - patches changelog and README through the Java helper;
  - creates release preparation commit(s);
  - creates `v<version>` tag.
- `tools/release/upload_maven_central.sh <version>`
  - validates `dist/bazel-depgen-<version>.zip` exists;
  - verifies `HEAD` is exactly `v<version>`;
  - requires an `origin` remote;
  - detects the default branch from `refs/remotes/origin/HEAD` or `gh repo view --json defaultBranchRef`;
  - fetches `origin/<default>` and tags before evaluating reachability;
  - verifies the current branch is the local default branch and is based on the refreshed `origin/<default>`;
  - verifies the tag is reachable from the local default branch;
  - does not require the release commit/tag to already be pushed;
  - uploads the bundle to Central Portal using `curl` and legacy credentials;
  - defaults to `publishingType=AUTOMATIC`;
  - polls until a terminal Central Portal status;
  - fails non-zero on validation or publication failure.
- `tools/release/finalize_release.sh <version>`
  - assumes Maven Central publication succeeded;
  - inserts post-release `Unreleased` idempotently;
  - commits post-release changelog reset when needed;
  - pushes commits and tags;
  - creates or updates GitHub release with notes extracted by the Java helper;
  - marks releases with major version `0` as prerelease;
  - silently best-effort closes a matching `v<version>` milestone.
- `tools/release/perform_release.sh <version>`
  - calls the scripts in order:
    1. `check_ready.sh`;
    2. `prepare_release.sh <version>`;
    3. `tools/package_maven_central.sh <version>`;
    4. `upload_maven_central.sh <version>`;
    5. `finalize_release.sh <version>`.

### Phase 4: Documentation and Old Flow Redirection

- Add `tools/release/README.md` with:
  - end-to-end release flow;
  - split-script recovery flow;
  - credential/tool prerequisites;
  - exact commands;
  - dry-run guidance;
  - Central Portal/GitHub behavior.
- Replace the old Ruby `perform_release` task with an abort that points to the new scripts.
- Keep existing Maven packaging Rake task aborts aligned with the new docs.
- Update `CONTRIBUTING.md` to point to `tools/release/README.md`.
- Update `CHANGELOG.md` under `Unreleased`.

### Phase 5: Validation and Commit Discipline

- Run targeted tests while iterating.
- Run the required full gate before marking implementation complete:

```bash
tools/check.sh
```

- Keep commits aligned with task boundaries:
  - planning commit;
  - Java helper/tests commit;
  - shell orchestration commit;
  - docs/Ruby redirect commit;
  - validation/task-board closeout commit if needed.

## Delivery Approach

- Prefer minimal local scripts with explicit usage text.
- Keep side-effecting publish steps out of Bazel rules.
- Keep Java helpers package-aligned and compiled through repository strict Java macros.
- Avoid new runtime dependencies for release scripts.
- Preserve old credential defaults where the user requested them.
- Treat `dist/` contents as generated release output, not source-controlled state.

## High-Risk Areas and Mitigations

- Central Portal API details may change.
  - Mitigation: implement the locked API contract from requirements; re-check official docs at implementation time only to
    confirm no breaking change has occurred; keep the implementation isolated in `upload_maven_central.sh`.
- Git publishing and GitHub release steps are externally visible.
  - Mitigation: require a local default-branch release tag guard before Central upload; finalization runs only after
    Central success; use idempotent behavior for reruns.
- Changelog parsing can corrupt release notes.
  - Mitigation: implement parsing in Java with focused tests and fail on missing/ambiguous sections.
- README version update can silently miss stale examples.
  - Mitigation: fail when the previous version is absent.
- Release scripts may capture unrelated local changes.
  - Mitigation: `prepare_release.sh` requires a clean git tree; re-check worktree before implementation/commits because a
    separate thread may modify this workspace.
- Networked commands cannot be tested safely in normal repo tests.
  - Mitigation: syntax-check scripts and test dry-run/local helpers; do not mock live Central/GitHub APIs.

## Required Full Gate

```bash
tools/check.sh
```

## Targeted Validation

```bash
bazel test //tools/release:all_tests --release_version=1.2.3
bazel run //tools/release:release_lifecycle -- --help
bash -n tools/release/*.sh tools/package_maven_central.sh
tools/release/prepare_release.sh 1.2.3 --dry-run
```

For upload/finalization scripts, validation is limited to syntax, local guard behavior, and dry-run/local helper behavior.
Live Central Portal and GitHub operations are release-time operations, not automated tests.

## Central Portal API Details

`upload_maven_central.sh` must use:

- `POST https://central.sonatype.com/api/v1/publisher/upload?name=bazel-depgen-<version>&publishingType=AUTOMATIC`
  with multipart field `bundle=@dist/bazel-depgen-<version>.zip`;
- `Authorization: Bearer <base64("realityforge:" + MAVEN_CENTRAL_PASSWORD)>`;
- response text as the deployment id;
- `POST https://central.sonatype.com/api/v1/publisher/status?id=<deployment-id>` for polling.

Polling behavior:

- continue: `PENDING`, `VALIDATING`, `VALIDATED`, `PUBLISHING`;
- success: `PUBLISHED`;
- failure: `FAILED`;
- timeout: 30 minutes at 15-second intervals.

Failure output includes deployment id, last status, and response body or extracted validation errors.

## Changelog Mutation Details

Preparation heading template:

```markdown
### [v<version>](https://github.com/realityforge/bazel-depgen/tree/v<version>) (<release-date>) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v<previous>...v<version>)

Changes in this release:
```

Finalization inserts a fresh empty `### Unreleased` section above the just-released heading if and only if it is absent.
The release-note body for GitHub starts after the release heading and ends before the next `### [v`; it includes
`Changes in this release:`.

## Decision Log

- Q01-Q41: completed packaging decisions from the existing Maven Central packaging plan remain unchanged.
- Q42: split release orchestration into boundary scripts plus optional `perform_release.sh`.
- Q43: require explicit version for mutating/publishing scripts; add read-only `next_version.sh`.
- Q44: automate release preparation commits and tag creation with `--dry-run`.
- Q45: require clean git tree for release preparation; dry-run only warns.
- Q46: replace old `zapwhite` readiness with repo-native checks and Java TODO scan.
- Q47: automate Central Portal upload and default to automatic publication.
- Q48: use legacy Central credentials, keep GPG env defaults, use `gh` for GitHub.
- Q49: always poll Central Portal to terminal status; no `--no-wait`.
- Q50: preserve old publish guard with exact `v<version>` tag and default-branch reachability.
- Q51: run GitHub finalization only after Maven Central publication succeeds.
- Q52: use Java helper for GitHub release-note extraction.
- Q53: keep two-step changelog mutation.
- Q54: update README Maven version during preparation and fail when previous version is absent.
- Q55: push commits/tags before creating GitHub release.
- Q56: silently best-effort close matching GitHub milestone.
- Q57: run full readiness once, then package validation after release prep.
- Q58: make finalization rerunnable/idempotent enough for recovery.
- Q59: do not recreate `STAGE`/`LAST_STAGE`.
- Q60: replace old Ruby release task with abort pointing to new scripts.
- Q61: document workflow in `tools/release/README.md` and keep script usage concise.
- Q62: readiness checks external tools, `gh auth status`, `MAVEN_CENTRAL_PASSWORD`, and `GPG_USER`.
- Q63: add focused Java helper tests; avoid live API mocks.
- Q64: keep HTTP orchestration in shell/`curl`, Java for structured local mutation/parsing.
- Q65: use the structured-delivery workflow and plan approval gate before implementation.
- Review Round 1: lock Central Portal API/status contract, default-branch guard algorithm, changelog format details, and
  explicit `//tools/release:release_lifecycle` helper target.
- Review Round 2: adjust upload guard to preserve the accepted old-flow sequencing where release commits/tags are pushed
  only during finalization after Maven Central publication succeeds.
