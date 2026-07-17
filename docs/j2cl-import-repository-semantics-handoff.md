# J2CL Import repository semantics handoff

## Scope and status

This is the deferred context for `J2clMode.Import` repository and source
requirements. No design has been accepted and no implementation has started.

The issue was discovered while designing the separate modern-J2CL
source-archive fix. It was explicitly excluded from that fix because correcting
Import mode changes binary repository emission, source validation, and
`includeSource` semantics rather than duplicate-JavaScript handling.

The source-archive work separately makes a modern-J2CL hard cut and fully
removes the legacy JS scanner and `__js_sources` machinery. Akasha filtering is
deferred in `docs/j2cl-source-archive-filtering-handoff.md`.

## Current behavior to re-verify

- `J2clMode.Import` is documented in `J2clMode.java` as a binary dependency
  included through `j2cl_import`.
- `ArtifactRecord.writeJ2clLibrary` emits `_j2cl_import` with
  `jar = "@<artifact-repository>//file"`; it does not consume a source archive.
- `ArtifactRecord.emitsBinaryRepository` currently returns true only for Java
  or Plugin nature targets. A J2CL-only Import target therefore appears to
  reference a binary repository that it does not emit.
- `ArtifactRecord.emitsSourceRepositoryRule` currently returns true for every
  emitted J2CL target, without checking Library versus Import mode.
- `ArtifactRecord.validate` currently requires `includeSource` to resolve true
  and requires a sources-classifier artifact for every non-replaced J2CL
  nature, including Import mode.
- `AddCommand` independently rejects every J2CL dependency when
  `includeSource` resolves false, also without checking the J2CL mode.
- A dual Java/J2CL Import artifact does emit its binary repository because of
  the Java nature. A J2CL-only Import artifact does not receive that incidental
  behavior.
- Import mode also rejects J2CL compiler suppressions and any effective
  JSpecify mode other than `Disable`; those are separate mode constraints, not
  causes of the repository inconsistency.

## Test gap

`ArtifactRecordTest.writeJ2clLibrary_modeImport` asserts the isolated
`_j2cl_import` call and its binary label. It does not assert the complete
repository-rule output for the same J2CL-only artifact. Existing tests cover
some Import validation failures but do not demonstrate a successful generated
module or extension whose binary label resolves.

No focused command was run that builds a generated J2CL-only Import target, so
the missing-repository conclusion is based on the currently inspected emission
predicates and should be verified end to end in the resumed investigation.

## Unresolved design tree

- Whether J2CL-only Import always emits the binary artifact repository.
- Whether Import mode permits `includeSource: false` and a missing sources
  classifier, as its binary-only contract implies.
- Whether explicitly requesting `includeSource: true` for J2CL-only Import
  should still emit an otherwise-unused sources repository or be rejected as
  meaningless configuration.
- How source emission behaves for combined Java/J2CL Import artifacts: Java may
  still attach a source JAR independently of the J2CL mode.
- Whether global `includeSource` continues to control Java source attachment
  without imposing a source requirement on J2CL Import.
- How per-nature replacement overlays affect binary and source repository
  requirements when only the J2CL nature is replaced.
- Whether validation logic is centralized around effective J2CL mode or remains
  duplicated between configuration commands and record construction.
- Required coverage for J2CL-only and Java/J2CL Import across module,
  extension, and direct BUILD generation strategies.
- User-visible migration and changelog wording for configurations that
  currently fail without sources or generate an unresolved binary label.

## Why this remains separate

The source-archive duplication fix can remove JS scanning while preserving all
existing Import behavior. Folding this cleanup into that change would make
failures harder to attribute and expand the release contract beyond modern
J2CL source handling. The defects are related only by their use of J2CL
artifact repositories.

## Suggested skills for the resumed session

- `grill-me` for resolving Import mode's binary and source contract one
  decision at a time.
- `structured-delivery-workflow` if the accepted contract becomes a persistent
  implementation plan.
