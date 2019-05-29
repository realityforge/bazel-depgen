# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Generate an error if `PluginAndLibrary` nature is used when combined with `J2cl` language.

* Add command line parameter that regenerates depgen cache files. This is useful if repositories have come back
  online or needed to be rebuilt.

* Configure repositories so that url checks against it can not be cached. This will require changes to the way we
  represent repositories.

* repositories should be able to specify whether they are searched by default or not and artifacts should be able
  to specify which artifacts are searched for artifact.

* Consider removing support for non-coord based configuration options for artifacts. This would make the
  modification of dependency files much easier.

* Add `init` command that initializes `dependencies.bzl` from template that includes all the options and
  documentation for each option. Note that the `exportDeps` configuration potentially limits scalability of
  builds as it results in deep dependency trees.

* Add `add` command that adds a dependency.

* Add `remove` command that removes a dependency.

* Add `update` command that updates the version of a dependency.

* Add `hash` command that checks that configuration matches a particular hash and/or generates and reports
  a hash. To get this working we will most likely need to be able to get the `*Config` objects remove all
  non-null values if they don't appear in the config and then generate the `YAML` and/or 'json' representation
  of the config and hash the config contents. Add assertion to workspace or target macro to verify
  `dependencies.yml` file has hash that matches hash of file that generated macro. This task should only be run
  if one of the dependencies is actually referenced. This would require adding it as a dependency of all root
  level targets?. This would allow tasks (i.e. `regenerate`) to run that did not reference dependencies.

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
