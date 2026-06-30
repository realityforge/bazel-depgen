# Maven Central Release Requirements

## Plan Status

Accepted on 2026-06-30 after iterative plan review.

## Mission

Finish the Bazel-native Maven Central release migration by adding the release lifecycle orchestration that still exists
only in the old Ruby/Buildr process.

The already implemented packaging layer produces:

- production classes jar;
- production sources jar;
- production Javadocs jar;
- executable `-all` jar with the curated runtime dependency subset;
- POM metadata;
- signed Maven Central bundle at
  `dist/bazel-depgen-<version>/org/realityforge/bazel/depgen/bazel-depgen/<version>/...`;
- upload zip at `dist/bazel-depgen-<version>.zip`.

This follow-up scope adds the release lifecycle around that package:

- release readiness checks;
- version discovery helper;
- changelog and README release preparation;
- release commits and tag creation;
- Central Portal bundle upload and automatic publication;
- post-release changelog reset;
- push, GitHub release creation, and best-effort milestone close;
- operator documentation and old Ruby release task redirection.

## Scope Boundaries

- Do not replace the existing Bazel artifact packaging implementation unless required by orchestration.
- Do not reintroduce Buildr or Ruby as the authoritative release path.
- Do not recreate the old `STAGE`/`LAST_STAGE` resume system.
- Do not add a third-party Maven publishing module.
- Do not add `jq` as a required dependency.
- Do not mock live Central Portal or GitHub APIs in repository tests.
- Central Portal publication is implemented through script orchestration and `curl`, not a Bazel rule.

## Locked Decisions

- Release workflow is split at irreversible boundaries:
  - `tools/release/check_ready.sh`;
  - `tools/release/next_version.sh`;
  - `tools/release/prepare_release.sh`;
  - existing `tools/package_maven_central.sh`;
  - `tools/release/upload_maven_central.sh`;
  - `tools/release/finalize_release.sh`;
  - optional `tools/release/perform_release.sh`.
- Mutating and publishing scripts require an explicit non-snapshot `<version>`.
- `next_version.sh` is read-only and derives the old Buildr-style next version from `CHANGELOG.md`.
- `prepare_release.sh` requires a clean git tree, except `--dry-run` may continue with a warning.
- Release preparation automates changelog/README commits and `v<version>` tag creation, with `--dry-run`.
- Readiness uses repo-native checks instead of `zapwhite`:
  - `tools/check.sh`;
  - `git diff --check`;
  - Java-source TODO scan equivalent to the old `tasks/todo.rake`;
  - external tool and credential checks.
- Readiness requires `gpg`, `curl`, `gh`, successful `gh auth status`, `GPG_USER`, and `MAVEN_CENTRAL_PASSWORD`.
  `GPG_PASS` remains optional.
- Maven Central credentials use the legacy Buildr defaults:
  - username defaults to `realityforge`;
  - password/token comes from `MAVEN_CENTRAL_PASSWORD`.
- GPG remains `GPG_USER` plus optional `GPG_PASS`.
- GitHub release operations use `gh`.
- Central upload defaults to automatic publication and always polls until a terminal status.
- Central upload must refuse to publish unless `HEAD` is exactly `v<version>` and that tag is reachable from the local
  default branch after refreshing `origin/<default>`.
- Central upload requires an `origin` remote and refreshes the default branch/tag refs before evaluating the guard.
- Central upload does not require the release commit/tag to already be pushed; finalization is the first push step after
  Maven Central publication succeeds.
- GitHub finalization runs only after Maven Central publication succeeds.
- Changelog mutation keeps the old two-step model:
  - release preparation replaces `### Unreleased` with `### [v<version>] ...`;
  - finalization reinserts a fresh empty `### Unreleased` section.
- Changelog and README structured mutations are implemented by Java tooling under `tools/release`.
- README Maven dependency version is updated during release preparation and fails if the previous version is absent.
- `finalize_release.sh` pushes commits/tags before creating the GitHub release.
- GitHub release notes come from the `CHANGELOG.md` section for `v<version>`.
- GitHub releases are marked prerelease when the major version is `0`, matching the old Buildr behavior.
- A matching GitHub milestone titled `v<version>` is closed best-effort and silently ignored on absence/failure.
- Finalization should be rerunnable:
  - do not duplicate `Unreleased`;
  - tolerate already-pushed commits/tags;
  - update or skip an existing GitHub release instead of failing.
- The old Ruby `perform_release` task is replaced with an abort pointing to the new scripts.
- Add `tools/release/README.md`; keep a short pointer in `CONTRIBUTING.md`; every script has concise `usage()`.
- Update `CHANGELOG.md` under `Unreleased` for the new release orchestration.

## Command Surface

Expected local commands after implementation:

```bash
tools/release/next_version.sh
tools/release/check_ready.sh
tools/release/prepare_release.sh 1.2.3
GPG_USER=KEYID tools/package_maven_central.sh 1.2.3
MAVEN_CENTRAL_PASSWORD=TOKEN tools/release/upload_maven_central.sh 1.2.3
tools/release/finalize_release.sh 1.2.3
tools/release/perform_release.sh 1.2.3
```

Dry-run command:

```bash
tools/release/prepare_release.sh 1.2.3 --dry-run
```

## Behavior Expectations

- `perform_release.sh <version>` runs:
  1. readiness checks;
  2. release preparation;
  3. package validation via `tools/package_maven_central.sh <version>`;
  4. Central Portal automatic upload/publication;
  5. finalization.
- Readiness runs full `tools/check.sh` once before release preparation.
- After release-prep commits and tagging, the packaging wrapper is the release validation before upload.
- Central upload uses Sonatype Central Portal Publisher API upload/status endpoints and fails non-zero on validation or
  publication failure.
- Release date defaults to the current local date in `YYYY-MM-DD`; `RELEASE_DATE` may override it to preserve old Buildr
  behavior.
- The Java helper owns structured local mutations:
  - derive previous/latest version from `CHANGELOG.md`;
  - derive next version;
  - patch release changelog;
  - insert post-release `Unreleased`;
  - extract release notes;
  - patch README version;
  - optionally parse Central Portal JSON if shell parsing becomes brittle.

## Central Portal API Contract

Implementation must use the current Sonatype Central Portal Publisher API contract:

- Base URL: `https://central.sonatype.com`.
- Upload endpoint:
  `POST /api/v1/publisher/upload?name=bazel-depgen-<version>&publishingType=AUTOMATIC`.
- Upload body: multipart form field `bundle` containing `dist/bazel-depgen-<version>.zip`.
- Authentication: bearer token value is the base64 encoding of `realityforge:<MAVEN_CENTRAL_PASSWORD>`, preserving the
  old Buildr username/password source.
- Upload response: deployment id as text; the script must print it.
- Status endpoint: `POST /api/v1/publisher/status?id=<deployment-id>`.
- Polling states:
  - continue: `PENDING`, `VALIDATING`, `VALIDATED`, `PUBLISHING`;
  - success: `PUBLISHED`;
  - failure: `FAILED`.
- `VALIDATED` is not success for the default automatic flow; keep polling until `PUBLISHED`, `FAILED`, or timeout.
- Polling timeout/backoff: poll every 15 seconds for up to 30 minutes.
- Failure output must include the deployment id, last status, and the response body or extracted validation errors.

The API details were verified against Sonatype's Publisher API documentation during plan review on 2026-06-30.

## Release Tag Guard Contract

`upload_maven_central.sh <version>` must perform this guard before calling Central Portal:

1. Require an `origin` remote.
2. Detect the default branch from `refs/remotes/origin/HEAD`; if absent, query `gh repo view --json defaultBranchRef`.
3. Refresh the default branch and tags:

   ```bash
   git fetch origin "+refs/heads/<default>:refs/remotes/origin/<default>" --tags
   ```

4. Require the current local branch to be `<default>`.
5. Require `refs/remotes/origin/<default>` to be an ancestor of `HEAD`, so the release is based on the latest fetched
   default branch.
6. Require `git describe --exact-match --tags HEAD` to equal `v<version>`.
7. Require `git merge-base --is-ancestor "v<version>" "refs/heads/<default>"`.

If any step fails, upload must fail before contacting Central Portal.

## Changelog Format Contract

Release preparation transforms the current top section exactly from:

```markdown
# Change Log

### Unreleased

* Change entry.

### [v0.25](https://github.com/realityforge/bazel-depgen/tree/v0.25) (2026-06-26) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.24...v0.25)
```

to:

```markdown
# Change Log

### [v0.26](https://github.com/realityforge/bazel-depgen/tree/v0.26) (2026-06-30) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.25...v0.26)

Changes in this release:

* Change entry.

### [v0.25](https://github.com/realityforge/bazel-depgen/tree/v0.25) (2026-06-26) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.24...v0.25)
```

Rules:

- `RELEASE_DATE` may override the date; otherwise use the current local date in `YYYY-MM-DD`.
- The compare base is the previous release heading version, or the first git commit for the first release.
- Release preparation must fail if `### Unreleased` is missing, duplicated, or empty after trimming whitespace.
- Release preparation must fail if `### [v<version>]` already exists.
- Release notes extraction starts after the release heading blank line and ends before the next `### [v`.
- Release notes include the `Changes in this release:` line.
- Finalization inserts:

  ```markdown
  # Change Log

  ### Unreleased

  ```

  before the `v<version>` heading only when no top-level `### Unreleased` section already exists there.

## Quality Gates

Required full gate before implementation tasks are marked complete:

```bash
tools/check.sh
```

Targeted gates during implementation:

```bash
bazel test //tools/release:all_tests --release_version=1.2.3
bazel run //tools/release:release_lifecycle -- --help
bash -n tools/release/*.sh tools/package_maven_central.sh
tools/release/prepare_release.sh 1.2.3 --dry-run
```

Networked release commands are not run as repository tests:

```bash
tools/release/upload_maven_central.sh 1.2.3
tools/release/finalize_release.sh 1.2.3
```

## Known Intentional Divergences

- Old OSSRH staging/promotion API is not ported; new upload uses the current Central Portal bundle API.
- Old `zapwhite` is not retained; native repo checks replace it.
- Old `STAGE`/`LAST_STAGE` resume semantics are replaced by separate scripts.
- GitHub uses `gh` instead of Ruby Octokit/netrc.
- Central upload defaults to automatic publication rather than manual Portal promotion.

## Open Questions Register

All release-orchestration questions are resolved. Earlier packaging questions `Q01`-`Q41` remain covered by the
existing completed packaging plan; this follow-up uses `Q42` onward.

### Q42

- status: resolved
- question: Should release orchestration be split at irreversible boundaries?
- context: Buildr used one `perform_release`; Central/GitHub steps are irreversible or externally visible.
- options: one monolithic script; split scripts with an optional wrapper.
- tradeoffs: monolithic is convenient but harder to recover; split scripts create clear resume points.
- recommended_default: split scripts plus optional happy-path wrapper.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`

### Q43

- status: resolved
- question: What should be the version authority?
- context: old Buildr derived a candidate from changelog unless `PRODUCT_VERSION` was supplied; Bazel packaging requires
  `--release_version`.
- options: implicit changelog-derived mutation; explicit version; explicit version plus read-only helper.
- tradeoffs: implicit is convenient but can surprise; explicit is deliberate; helper preserves convenience.
- recommended_default: explicit version for mutating/publishing scripts and read-only `next_version.sh`.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q44

- status: resolved
- question: Should release preparation create commits and tags automatically?
- context: Buildr committed changelog/README changes and created the release tag.
- options: print commands only; automate commits/tags; automate with dry-run.
- tradeoffs: manual commands reduce script power but increase drift; automation preserves release consistency.
- recommended_default: automate commits and tags with `--dry-run`.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q45

- status: resolved
- question: Should `prepare_release.sh` require a clean git tree?
- context: release commits/tags must represent exact release content.
- options: require clean tree; allow dirty tree; warn only.
- tradeoffs: clean tree prevents accidental content; dirty tree is flexible but ambiguous.
- recommended_default: require clean tree, with dry-run warning only.
- user_decision: accepted recommendation; also noted another thread may change the worktree, so re-check before edits.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q46

- status: resolved
- question: Should readiness port `zapwhite` literally?
- context: current repo already has `tools/check.sh`; old Buildr used `bundle exec zapwhite`.
- options: keep zapwhite; replace with repo-native checks.
- tradeoffs: keeping zapwhite preserves a Ruby dependency; native checks fit the Bazel release path.
- recommended_default: native checks plus Java TODO scan and `git diff --check`.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q47

- status: resolved
- question: Should Central Portal publishing be automated?
- context: old Buildr published automatically through OSSRH; current Sonatype flow accepts upload bundles through Portal
  APIs.
- options: manual upload only; API upload default manual; API upload default automatic.
- tradeoffs: manual is safer but incomplete; automatic matches old release automation and user preference.
- recommended_default: API upload with automatic publication by default.
- user_decision: accepted with default automatic publication.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q48

- status: resolved
- question: How should release credentials be read?
- context: old Buildr hardcoded Maven username `realityforge`, read `MAVEN_CENTRAL_PASSWORD`, used `GPG_USER`/`GPG_PASS`,
  and used Octokit/netrc for GitHub.
- options: new env names; legacy defaults; mixed scheme.
- tradeoffs: new names are clearer but break existing setup; legacy keeps operator compatibility.
- recommended_default: legacy Central and GPG defaults, GitHub via `gh`.
- user_decision: use legacy Central tooling, keep GPG as-is, use `gh`.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q49

- status: resolved
- question: Should upload polling wait for a terminal Central Portal state?
- context: automatic publication may validate asynchronously.
- options: no wait; optional wait; always wait.
- tradeoffs: no wait hides failures; always wait gives deterministic release outcome.
- recommended_default: always poll and do not support `--no-wait`.
- user_decision: accepted recommendation but no `--no-wait`.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q50

- status: resolved
- question: Should upload keep the old publish-if-tagged guard?
- context: Buildr published only when `HEAD` was exactly on a release tag merged to a candidate branch.
- options: no guard; exact tag only; exact tag plus default-branch reachability.
- tradeoffs: stricter guard prevents accidental publication from local work.
- recommended_default: exact `v<version>` tag reachable from detected default branch.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q51

- status: resolved
- question: Should GitHub finalization happen only after Maven Central succeeds?
- context: a GitHub release can point users at a Maven version that failed publication.
- options: independent finalization; finalization after successful upload.
- tradeoffs: sequencing after upload avoids false public release notes.
- recommended_default: finalize only after Central automatic publication succeeds.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q52

- status: resolved
- question: How should GitHub release notes be extracted?
- context: Buildr extracted the `CHANGELOG.md` section for the release version.
- options: shell/Python helper; Java helper; manual notes.
- tradeoffs: Java matches existing release tooling pattern and is testable in Bazel.
- recommended_default: Java helper extracts notes and `gh release create` consumes the notes file.
- user_decision: prefer Java tooling.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q53

- status: resolved
- question: Should changelog mutation keep the old two-step model?
- context: Buildr moved `Unreleased` into a release section, then later inserted a new empty `Unreleased`.
- options: one-step changelog rewrite; old two-step model.
- tradeoffs: old model matches existing release history and lets GitHub notes come from the release section.
- recommended_default: two-step Java helper.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q54

- status: resolved
- question: Should release preparation update README Maven dependency version?
- context: Buildr patched README version references; current README hardcodes the latest release.
- options: skip README; update and fail if previous version absent.
- tradeoffs: failing prevents stale README snippets.
- recommended_default: update README and fail if the previous version cannot be found.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q55

- status: resolved
- question: Should finalization push before creating the GitHub release?
- context: Buildr pushed commits/tags before creating the GitHub release.
- options: create release first; push first.
- tradeoffs: pushing first ensures GitHub can see the tag and changelog commits.
- recommended_default: push commits/tags, then create or update the GitHub release.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q56

- status: resolved
- question: Should finalization close a matching GitHub milestone?
- context: Buildr closed an open milestone titled exactly `v<version>`.
- options: do not close; fail if close fails; best-effort silent close.
- tradeoffs: milestone close is useful but not release-critical.
- recommended_default: best-effort close, silently ignore absence/failure.
- user_decision: accepted recommendation with no warning.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q57

- status: resolved
- question: How many full checks should `perform_release.sh` run?
- context: old Buildr built before docs/version prep; full checks are expensive.
- options: full check twice; full readiness once plus package validation; package only.
- tradeoffs: full check twice is slow; package validation after prep proves tagged release artifacts.
- recommended_default: readiness once, then package validation after prep.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q58

- status: resolved
- question: How should finalization recover after Maven publication?
- context: Maven publication is effectively irreversible, but GitHub/post-release steps can fail.
- options: continue best-effort through all steps; fail fast with rerunnable finalization.
- tradeoffs: rerunnable finalization gives controlled recovery without hiding failures.
- recommended_default: fail fast but make finalization idempotent enough to rerun.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q59

- status: resolved
- question: Should old `STAGE`/`LAST_STAGE` resume semantics be retained?
- context: split scripts naturally define resume points.
- options: recreate stage env vars; rely on split scripts.
- tradeoffs: stage env vars are custom state; split scripts are simpler and visible.
- recommended_default: do not recreate stage env vars.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q60

- status: resolved
- question: What should happen to the old Ruby release task?
- context: Maven packaging Rake tasks already abort, but `tasks/release.rake` still defines old `perform_release`.
- options: keep both; delete task; replace with abort pointer.
- tradeoffs: two authoritative flows are unsafe; abort preserves discoverability.
- recommended_default: replace old task with abort message pointing to new scripts.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q61

- status: resolved
- question: Where should release documentation live?
- context: release flow is larger than the current `CONTRIBUTING.md` Maven bundle snippet.
- options: only script usage; only `CONTRIBUTING.md`; dedicated release README plus pointer.
- tradeoffs: dedicated doc keeps operator flow clear without bloating contributor docs.
- recommended_default: add `tools/release/README.md`, short `CONTRIBUTING.md` pointer, concise script usage.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q62

- status: resolved
- question: Should readiness check external release tools and credentials?
- context: signing, Central upload, and GitHub release otherwise fail late.
- options: late failure; up-front checks.
- tradeoffs: up-front checks make release prerequisites explicit.
- recommended_default: check `gpg`, `curl`, `gh`, `gh auth status`, `MAVEN_CENTRAL_PASSWORD`, and `GPG_USER`.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q63

- status: resolved
- question: What tests should cover the Java helper?
- context: structured file mutations are easy to regress; live APIs should not be mocked in repo tests.
- options: no helper tests; focused helper tests; live API tests.
- tradeoffs: focused tests give confidence without network brittleness.
- recommended_default: focused Java helper tests plus shell syntax/dry-run checks.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q64

- status: resolved
- question: Should Central API upload/polling be shell or Java?
- context: repo release tools use Java for structured local work and shell for orchestration.
- options: all shell; all Java; shell `curl` plus Java for structured parsing/local mutation.
- tradeoffs: all Java overbuilds API orchestration; all shell can make JSON parsing brittle.
- recommended_default: shell `curl`, Java only where structure matters.
- user_decision: accepted recommendation.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`

### Q65

- status: resolved
- question: Should the release orchestration work use the structured delivery workflow and planning artifacts?
- context: the work spans git, Central Portal, GitHub, docs, scripts, Java helpers, and tests.
- options: implement directly; update plan artifacts and use approval gate.
- tradeoffs: planning adds overhead but makes the accepted decisions auditable.
- recommended_default: use structured-delivery workflow and update `plans/maven-central-release`.
- user_decision: requested `structured-delivery-workflow`.
- artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`, `20-task-board.yaml`
