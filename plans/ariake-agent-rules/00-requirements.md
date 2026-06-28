# Requirements

## Mission

Adopt Ariake's repository agent rules in Bazel-Depgen and make the current repository comply with them.

## Scope

- Update `AGENTS.md` with the Ariake rules.
- Remove recursive `glob()` usage from Bazel targets.
- Ensure each Java source package directory that owns source files also owns its own `BUILD.bazel`.
- Keep Bazel-Depgen on Bazel 9.1.1 and Java 17.

## Quality Gates

- `rg -n "glob\\(" -g 'BUILD.bazel' -g '*.bzl' -g 'MODULE.bazel'` reports no source target glob usage.
- `tools/check.sh` passes.

## Open Questions Register

- id: Q-01
  status: resolved
  question: Should the Ariake rules be copied literally or adapted to Bazel-Depgen's existing AGENTS structure?
  context: Bazel-Depgen already has commit and changelog guidance in `AGENTS.md`.
  options: Copy the Ariake title verbatim, or add the Ariake rules under this repo's existing guide.
  tradeoffs: A verbatim copy would drop existing repo-specific release guidance. Integrating the rules preserves current guidance while adopting the new constraints.
  recommended_default: Integrate the Ariake rules into the existing guide.
  user_decision: Integrate into the existing guide.
  artifacts_updated: `00-requirements.md`, `10-implementation-plan.md`
