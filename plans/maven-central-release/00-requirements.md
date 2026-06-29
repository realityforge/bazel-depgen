# Maven Central Release Packaging Requirements

## Mission

Create Bazel-native Maven Central release packaging for bazel-depgen.

The release flow must produce:

- Production classes jar.
- Production sources jar.
- Production Javadocs jar.
- Executable `-all` jar containing production classes/resources and the curated dependency subset needed to run the CLI.
- POM metadata matching the existing `0.25` POM except for the version value.
- Signed Maven Central bundle under `dist/bazel-depgen-<version>/org/realityforge/bazel/depgen/bazel-depgen/<version>/...`.
- Upload zip at `dist/bazel-depgen-<version>.zip`.

## Scope Boundaries

- No Maven Central upload API call is implemented.
- The old Ruby release path does not need compatibility.
- Release build logic is repo-local and does not add a third-party publish module.
- The POM keeps an empty dependency list.
- The plain jar is not executable; only the `-all` jar has `Main-Class`.

## Locked Decisions

- Version is supplied by `//tools/release:version` with a `.bazelrc` alias `--release_version`.
- Default version is `0.0.0-SNAPSHOT`; the side-effecting dist packaging rejects snapshots.
- `config.properties`, artifact filenames, POM, and dist layout use the same version setting.
- `//tools/release:maven_artifacts` builds unsigned cacheable artifacts.
- `//tools/release:dist` is the side-effecting local signed bundle assembly.
- The `-all` jar uses explicit external jar file labels, not `JavaInfo.transitive_runtime_jars`.
- The `-all` payload starts from the old Ruby `compile.with`/`MAVEN_RESOLVER` set, removes `javax_annotation`, and adds `jspecify`.
- Generated optional/tooling/test dependencies are excluded.
- Duplicate merge handling:
  - synthesize the manifest;
  - drop dependency signature metadata;
  - skip duplicate directories;
  - merge `META-INF/services/*`;
  - preserve first deterministic legal metadata;
  - fail on duplicate non-identical classes or resources.
- Sources jar includes production `.java` files plus production resources.
- Javadocs use public/protected visibility and fail on command errors, not warnings.
- Dist signing uses `gpg` by default, uses `GPG_USER` and `GPG_PASS` like the old Buildr flow, and accepts optional
  `--gpg-executable` and `--gpg-key-id` overrides.
- Dist checksums are generated for primary artifacts only.
- The dist task cleans only the requested version directory and zip.
- Add `/dist` to `.gitignore`.
- Add a thin `tools/package_maven_central.sh` wrapper and document raw commands in `CONTRIBUTING.md`.
- Disable the old Ruby Maven Central packaging/upload path or redirect it to the new flow.
- Update `CHANGELOG.md` under `Unreleased`.

## Command Surface

Expected commands:

```bash
bazel build //tools/release:maven_artifacts --release_version=1.2.3
bazel test //tools/release:all_tests --release_version=1.2.3
GPG_USER=KEYID bazel run //tools/release:dist --release_version=1.2.3
GPG_USER=KEYID tools/package_maven_central.sh 1.2.3
```

## Quality Gates

- `bazel build //tools/release:maven_artifacts`
- `bazel test //tools/release:all_tests`
- `tools/check.sh`

## Open Questions Register

No open questions remain. The design interview resolved all implementation decisions before coding.
