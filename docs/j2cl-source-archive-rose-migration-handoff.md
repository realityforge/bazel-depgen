# Rose migration after the J2CL source-archive fix

## Scope and status

This is the deferred consumer-migration context for Rose after the upstream
bazel-depgen source-archive fix is released. No Rose files have been changed as
part of the upstream investigation, and Rose validation is not a gate performed
by the upstream implementation task.

The intended release boundary is bazel-depgen 0.28. That release will target
J2CL at or after commit
[`805391dbc84aa472af75ee1b0237606d0acd13bb`](https://github.com/google/j2cl/commit/805391dbc84aa472af75ee1b0237606d0acd13bb)
and remove the legacy JavaScript-only repository path. Akasha source filtering
is a separate unresolved design recorded in
`docs/j2cl-source-archive-filtering-handoff.md`.

## Rose evidence to re-verify

- Consumer checkout: `/Users/peter/Code/stocksoftware/rose5`.
- Rose's original evidence is in
  `docs/j2cl-upstream-upgrade-audit.md`, especially “Source jars now carry
  JavaScript directly”. Its attribution of direct archive consumption to J2CL
  commit `49fe6a2d` needs the boundary correction recorded in the filtering
  handoff.
- Rose commit `6028299057a0205bc18bcd3204b200073675459a` introduced the six
  replacement J2CL targets and removed their generated `__js_sources`
  repositories. Its `MODULE.bazel` and `third_party/java/BUILD.bazel` diff is
  the clearest before/after record of the workaround.
- Current Rose configuration should be re-read before editing because its J2CL
  pin and dependency generation may have advanced since that commit.

## Accepted upstream behavior relevant to the migration

- Generated J2CL libraries receive only the original Maven source archive.
- `RecordUtil.readJsAssets`, cached `js_assets`, `ArtifactRecord` JS state,
  `__js_sources` repositories, and the associated conditional `http_archive`
  loads are removed rather than retained behind compatibility behavior.
- Old `_depgen.properties` entries containing `js_assets` are harmless stale
  metadata; no cache rewrite is part of the change.
- The `http_archive` entry in `options.repositoryRuleLoadSymbols` is removed
  because no generator output uses it after the split is deleted.
- bazel-depgen does not attempt to detect the configured J2CL revision. The
  minimum commit and breaking behavior are release documentation instead.
- `J2clMode.Import` repository semantics are outside this change and are
  recorded separately in
  `docs/j2cl-import-repository-semantics-handoff.md`.

## Straightforward Rose migration set

Five of Rose's six replacements exist only to avoid duplicate ordinary
JavaScript and can use the corrected generated J2CL targets:

- Arez core
- Arez Persist core
- BrainCheck core
- React4j core
- Zemeckis core

Their published source JARs each contain ordinary JavaScript that modern J2CL
receives from the original archive. Some also contain valid `.native.js`
companions; preserving the original archive keeps those companions available.
The exact archive entry counts and checksums are recorded in the filtering
handoff and should be re-verified if dependency versions change.

## Akasha remains separate

Akasha's source JAR contains four ordinary `.js` files that Rose needs and 14
`.native.js` patches for native JsTypes that modern J2CL must not receive. Rose
currently uses a checksum-locked `http_archive` with `java_srcs` and an explicit
four-file `js_srcs` allowlist. The upstream core fix does not replace that
filtering behavior, so the Akasha replacement remains until the deferred design
is resolved.

## Migration evidence still absent

No Rose regeneration, BUILD/MODULE edit, Bazel query, build, or test has been
performed for this migration. The expected consumer endpoint is that the five
non-Akasha replacements and obsolete JavaScript-only repository declarations
are gone, generated targets are used directly, Akasha's current override is
unchanged, and Rose's relevant J2CL build/test surface passes.

## Suggested skills for the resumed session

- `structured-delivery-workflow` for a reviewable cross-file consumer migration.
- `grill-me` only if the migration exposes a consumer policy decision not
  already settled by the upstream design.
