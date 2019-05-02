# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Incorporate features from [bazel_maven_repository](https://github.com/square/bazel_maven_repository) such as:
  - Store jars in the "content addressable" cache, which is machine-wide, and survives even `bazel clean --expunge`
  - `maven_jvm_artifact` which defines `deps` attribute (i.e. Any dependencies needed at compile-time for consumers
    of this target), `runtime_deps` attribute (i.e. Any dependencies needed only at runtime - built into _test and
    _binary deploy jars) and `exports` attribute (i.e. Any targets listed here are treated by the consuming rule as
    if it had declared them).

* Consider functionality like [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps) to determine
  if specific deps are needed.

* Enhance options in `dependencies.yml` so that it determines the output format of the tool. This includes things like
  prefixes for names, whether to support `omit_*` config.

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass.

* Model output on existing tools such as above and https://github.com/bazelbuild/rules_jvm_external#generated-targets

* Add language types for `kotlin`, `scala`, `java`, `j2cl`. Add some tags to go along with it.

* Support non-jar dependencies and expose them as files/filegroups

* Add support for global excludes

* Add ability to add extra runtime or extra compile deps to an artifact

* Add ability to specify that for an artifact, optional dependencies are included

* Add ability to download source artifacts as an `Option`. Add per-artifact rule that can override global option.

* Add ability to add in labels that will be used as an alias of a dependency. This is similar to replacements
  except that it is done at the time that the `setup_workspace()` based on parameters passed and while replacements
  always occur.
