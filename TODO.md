# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Add validate phase against `ArtifactConfig` that will:
  - verify coord to ensure that is in correct format
  - verify coords is not present with other spec components and set other spec components when parsing coord.
  - verify excludes to ensure that they contain at most 1-2 spec components (i.e. group + optional id) and
    create inner model object to represent exclude

* Incorporate features from [bazel_maven_repository](https://github.com/square/bazel_maven_repository) such as:
  - Store jars in the "content addressable" cache, which is machine-wide, and survives even `bazel clean --expunge`
  - `maven_jvm_artifact` which defines `deps` attribute (i.e. Any dependencies needed at compile-time for consumers
    of this target), `runtime_deps` attribute (i.e. Any dependencies needed only at runtime - built into _test and
    _binary deploy jars) and `exports` attribute (i.e. Any targets listed here are treated by the consuming rule as
    if it had declared them).

* Consider functionality like [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps) to determine
  if specific deps are needed.

* Enhance options in `dependencies.yml` so that it determines the output format of the tool. This includes things like
  name of function to add symbols, prefixes for names, whether to support `omit_*` config.
