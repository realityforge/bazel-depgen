# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.


* Add command line parameter that regenerates depgen cache files. This is useful if repositories have come back
  online or needed to be rebuilt.

* Configure repositories so that url checks against it can not be cached. This will require changes to the way we
  represent repositories.

* repositories should be able to specify whether they are searched by default or not and artifacts should be able
  to specify which artifacts are searched for artifact.

* Add `init` command that initializes `dependencies.bzl` from template that includes all the options and
  documentation for each option.

* Add `add` command that adds a dependency.

* Add `remove` command that removes a dependency.

* Add `update` command that updates the version of a dependency.

* Incorporate features from [bazel_maven_repository](https://github.com/square/bazel_maven_repository) such as:
  - Store jars in the "content addressable" cache, which is machine-wide, and survives even `bazel clean --expunge`
  - `maven_jvm_artifact` which defines `deps` attribute (i.e. Any dependencies needed at compile-time for consumers
    of this target), `runtime_deps` attribute (i.e. Any dependencies needed only at runtime - built into _test and
    _binary deploy jars) and `exports` attribute (i.e. Any targets listed here are treated by the consuming rule as
    if it had declared them).

* Consider functionality like [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps) to determine
  if specific deps are needed.

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass.

* Model output on existing tools such as above and https://github.com/bazelbuild/rules_jvm_external#generated-targets

* Add language types for `kotlin`, `scala`, `java`, `j2cl`. Add some tags to go along with it.

* Support non-jar dependencies and expose them as files/filegroups

* Add ability to add extra runtime or extra compile deps to an artifact

* Add strict mode so that if sources is not present and it has not been marked as not included, the tool will fail.

* Optionally pass output through buildifier.

* Add assertion to workspace or target macro to verify dependencies.yml file has hash that matches hash of file that generated macro

* Add support for managed dependencies which essentially contains a list of artifacts that include version

* Add support for `neverlink` on artifacts.

* Figure out how to populate `licenses` arguments in the future (possibly via scanning poms and heuristics or
  explicit configuration). Alternatively just set if to dummy values as most projects don't have it as a central
  part of the build process.

* Support alternative to `http_file` that supports basic authentication.

* Scan `WORKSPACE` and verify that workspace rule is called from within it? Would need a global option to
  disable this check in case it is called from downstream macro. Probably the best we can do is scan for load
  string. If not present then issue a warning (and also  indicate how the warning can be suppressed)

* Consider default value for global `exportDeps` option: `true` matches maven conventions, `false` matches bazel conventions
