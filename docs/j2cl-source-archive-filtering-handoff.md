# J2CL source-archive filtering handoff

## Scope and status

This is the deferred design context for filtering selected entries from Maven
source JARs used by generated J2CL targets, with Akasha as the motivating case.
No filtering design has been accepted and no implementation has started.

The broader source-archive fix targets J2CL at or after upstream commit
[`805391dbc84aa472af75ee1b0237606d0acd13bb`](https://github.com/google/j2cl/commit/805391dbc84aa472af75ee1b0237606d0acd13bb),
removes legacy `__js_sources` splitting, and provides no compatibility mode for
older J2CL. Akasha-specific filtering was deliberately separated and will not
block the core duplication fix. Rose's later migration is captured in
`docs/j2cl-source-archive-rose-migration-handoff.md`; it retains the Akasha
override until this filtering design is resolved.

## Evidence to re-verify

- Rose consumer checkout: `/Users/peter/Code/stocksoftware/rose5`.
- Rose's evidence and workaround rationale are in
  `docs/j2cl-upstream-upgrade-audit.md`, especially “Source jars now carry
  JavaScript directly”.
- Rose commit `6028299057a0205bc18bcd3204b200073675459a` introduced six J2CL
  replacement targets and the Akasha archive override. Current configuration is
  in `MODULE.bazel`, `third_party/java/BUILD.bazel`, and
  `third_party/java/dependencies.yml`.
- The published Akasha source JAR is
  `org.realityforge.akasha:akasha-j2cl:0.33`, SHA-256
  `e07c8d32822e13ed3846c99a2445ad27dda1b887c01fa96a73e613bea7a78540`.
  It contains 1,852 `.java` files, four ordinary `.js` files, and 14
  `.native.js` files. The four-file allowlist is recorded in Rose's
  `MODULE.bazel`.
- The 14 Akasha native files correspond to native JsTypes. Current J2CL rejects
  a `.native.js` companion for a native JsType, so passing the original archive
  unfiltered is invalid. Filename inspection alone does not safely distinguish
  this case from valid native companions such as `ArezConfig.native.js`.
- The Rose workaround uses a checksum-locked `http_archive`, a Java filegroup,
  and the four ordinary-JS entries. Its generated J2CL target consumes those two
  filegroups instead of the original source archive.

## Upstream boundary correction

Rose's audit attributes direct archive consumption to
[`49fe6a2dbd6478fea111776844d11c9c82fa60c5`](https://github.com/google/j2cl/commit/49fe6a2dbd6478fea111776844d11c9c82fa60c5).
That commit moved annotation processing into the transpiler but still supplied
a stripped archive containing only Java. The following commit
[`805391dbc84aa472af75ee1b0237606d0acd13bb`](https://github.com/google/j2cl/commit/805391dbc84aa472af75ee1b0237606d0acd13bb)
moved incompatible-annotation stripping into the Javac frontend and changed
`_j2cl_transpile` to receive the original `jvm_srcs`. That is the exact boundary
at which ordinary and native JavaScript embedded in a source JAR became direct
transpiler inputs.

## Legacy bazel-depgen behavior through v0.27

- `DependencyCollector` scans downloaded source JARs through
  `DepgenMetadata.getJsAssets`.
- `RecordUtil.readJsAssets` selects ordinary `.js` while excluding `/public/`
  and `.native.js`.
- `ArtifactRecord` emits an `__js_sources` `http_archive` for that selection and
  adds both it and the original `__sources` archive to `j2cl_library.srcs`.
- Existing metadata, record, application-record, and generate-command tests
  explicitly assert this legacy split. They do not compile generated output
  with a real current J2CL toolchain.
- Releases `0.24` through `0.27` retain this behavior. The core source-archive
  fix removes it from the next release.

## Constraints exposed by Akasha

- An `http_file` label for a source JAR cannot hide selected archive entries.
  Filtering therefore implies extraction, repacking, or a build action rather
  than merely changing the generated `srcs` list.
- Akasha is J2CL-only in Rose. A generic design also has to account for
  artifacts with both Java and J2CL natures: `java_import.srcjar` still needs a
  source archive while J2CL may need filtered extracted inputs.
- Preserving the historical automatic exclusions for `/public/` and
  `.native.js` is not inherently correct under modern J2CL. Ordinary JS is now
  carried by the archive, valid native companions need to remain visible, and
  Akasha's native companions need explicit exclusion.
- Automatically parsing Java deeply enough to classify native companions would
  duplicate J2CL semantics and was identified as too fragile.
- Rose's workaround uses `glob(["**/*.java"])`; bazel-depgen's repository rules
  prohibit `glob()` in Bazel targets. Whether generated external-repository
  BUILD content may use a glob, or must list/repackage sources another way, is
  unresolved.

## Unresolved design tree

- Whether generic filtering follows in a later release or remains a consumer
  override.
- Whether configuration expresses an allowlist, exclusions, a source-selection
  mode, or separate Java/ordinary-JS/native-JS selections.
- Whether filtering is limited to `.native.js` or is a general source-archive
  entry contract, including the historical `/public/` treatment.
- Whether filtered J2CL-only artifacts replace `__sources` with an
  `http_archive`, and what repository shape is used for dual Java/J2CL
  artifacts.
- Whether source entries are explicitly enumerated, selected by generated
  globs, or repacked into a filtered archive.
- Schema placement and validation under per-artifact `j2cl` configuration,
  including nonexistent paths, overlaps, empty selections, and applicability
  to `J2clMode.Import`.
- Migration shape for Rose's Akasha replacement and the validation needed to
  prove the four ordinary JS files remain present while all 14 native patches
  remain absent.
- Test depth: generated-text coverage only versus a focused real-J2CL
  integration fixture.

## Validation already performed

The six affected Maven Central source JARs were downloaded and inspected by
checksum and entry type. The core fix has generator coverage for extension,
module, direct BUILD, and command-generation paths using ordinary, public, and
native JavaScript entries. Its full `tools/check.sh` gate passed. No Rose
generation, build, test, or file change was part of that validation.

## Suggested skills for the resumed session

- `grill-me` for resolving the design tree one decision at a time.
- `structured-delivery-workflow` if the accepted design is turned into a
  persistent implementation plan.
