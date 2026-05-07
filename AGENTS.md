# Repository Guidelines

This guide helps contributors work effectively on the Bazel-Depgen codebase.

## User Interaction

When asked to perform a task, ask the user questions one at a time until you have enough context. Feel free to make
reasonable assumptions based on patterns present in the code and ask the user to confirm the assumptions if there are
reasonable alternatives.

## Notes for the Agent

When you learn something non-obvious, add it here if it would make future changes faster or of higher quality.

## Coding Style & Naming Conventions

- Language level: Java 17; compilation uses `-Xlint:all,-processing,-serial` and `-Werror`.
- Nullability: prefer `@Nonnull`/`@Nullable` from `javax.annotation`.
- Public API should have Javadoc; keep package docs in `package-info.java`.

## Commit & Pull Request Guidelines

- Follow `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`.
- Keep commits small and focused; update `CHANGELOG.md` for user-visible changes.
- Any user-facing behavior change must add an `Unreleased` entry in `CHANGELOG.md` in the same change, even if the behavior only affects generated code or developer-facing warnings.
- When updating `CHANGELOG.md`, add the message under the "Unreleased" section. DO NOT add a `Changes in this release:` header as that is added as part of the automation.
- Remove trailing whitespace and keep file endings with a newline.
