# Implementation Plan

## Phases

1. Add Ariake's agent rules to `AGENTS.md`.
2. Replace coarse recursive Java Bazel targets with package-owned `BUILD.bazel` files and explicit source lists.
3. Run buildifier, Java formatting, and the full `tools/check.sh` gate.

## Decisions

- Keep existing Bazel 9.1.1 and Java 17 setup unchanged.
- Preserve the application dependency graph; any extra targets should be structural Bazel target changes only.
- Break package-level Bazel cycles by splitting root-package shared classes into local targets rather than listing child directory files from a parent target.

## Decision Log

- Q-01: Ariake rules are integrated into this repo's existing `AGENTS.md` so current changelog guidance remains available.

## Required Full Gate

```bash
tools/check.sh
```
