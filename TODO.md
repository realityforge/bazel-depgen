# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Generate an error if `PluginAndLibrary` nature is used when combined with `J2cl` language.

* Add command line parameter that regenerates depgen cache files. This is useful if repositories have come back
  online or needed to be rebuilt.

* Configure repositories so that url checks against it can not be cached.

* repositories should be able to specify whether they are searched by default or not and artifacts should be able
  to specify which artifacts are searched for artifact.

* Add `init` command that initializes `dependencies.bzl` from template that includes all the options and
  documentation for each option. Note that the `exportDeps` configuration potentially limits scalability of
  builds as it results in deep dependency trees. Consider also generating initial `WORKSPACE` if a walk through
  parent directories does not locate one.

* Add `add` command that adds a dependency.

* Add `remove` command that removes a dependency.

* Add `update` command that updates the version of a dependency.

* Change `hash` command so that it checks an optionally supplied hash matches actual hash. If it does not then
  the command exits with a non-zero exit code. Generate a target that verifies that the actual hash matches the
  hash of the `dependencies.yml` that was used to generate the extension. The target is a dependency of the
  `http_file` (if possible) or the leaf targets so that it is only run if a dependency requires it.

* Consider converting to commandline tool named `bzt`

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass. It may be possible to enhance [dependabot](https://dependabot.com/) to do this now that it
  is owned by Github.

* Add language types for `kotlin`, `scala`, `java`, `aar`, `j2cl` that control how artifacts defined. Some
  artifacts may support multiple languages. Support non-jar dependencies and expose them as files/filegroups
  - See [Mabel](https://github.com/menny/mabel) for `aar`, `kotlin`
  - See[bazel_maven_repository](https://github.com/square/bazel_maven_repository) for `aar`, `kotlin`
  - Configuration per language can probably be achieved by having a global default language plus a per-artifact
    list of languages. Non-default languages generate artifacts with suffixes such as `-j2cl`. Configuration for
    per-artifact level happens as object below artifact or below config (i.e. `j2cl: {suppress: 'debuggerStatement'}`)

* Add strict mode so that if sources is not present and it has not been marked as not included, the tool will fail.

* Optionally pass output through [buildifier](https://github.com/bazelbuild/buildtools/tree/master/buildifier).

* Add support for managed dependencies which essentially contains a list of artifacts that include version

* Add support for `neverlink` on artifacts.

* Support alternative to `http_file` that supports basic authentication.

* Add Bazel target/task that will regenerate dependencies. [Mabel](https://github.com/menny/mabel) has a set
  of `//resolver/..` tasks that may act as inspiration. Ultimately they will just invoke the CLI tool.

* Look to [bazel-tools](https://github.com/spotify/bazel-tools) and [awesome-bazel](https://github.com/jin/awesome-bazel)
  to see if there is other tools that can be incoporated.
  - Most likely we will want to support a tool like [BUILD_file_generator](https://github.com/bazelbuild/BUILD_file_generator)
    or [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps), both of which scan java files and
    automagically creates `BUILD` files (somehow?) so that there is fine grain dependencies without the heartache.
  - Another option is [exodus](https://wix-incubator.github.io/exodus)
