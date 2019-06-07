# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Read a global `.depgen.rc` so can use specify settings such as `settings.xml` location and the cache
  directory to some central place (i.e. `~/.depgen/repository`)

* Add `init` command that initializes `dependencies.bzl` from template that includes all the options and
  documentation for each option. Note that the `exportDeps` configuration potentially limits scalability of
  builds as it results in deep dependency trees. Consider also generating initial `WORKSPACE` if a walk through
  parent directories does not locate one.

  `init --no-create-workspace`

* Add `info` command that displays all the parameters passed into tool. This includes the
  - dependency file
  - settings file
  - cache directory
  - reset-cached-metadata flag
  - bazel's repository cache directory

  `init [info key]`

* Add `add` command that adds a dependency.

  `add [coord] --alias foo --nature java --nature plugin --include-optional --include-source --export-deps --generates-api --excludes com.example:base --excludes com.example:base --visibility //blah  --visibility //blee --j2cl-suppress blah`

* Add `remove` command that removes a dependency.

  `remove [coord]`

* Add `update` command that updates the version of a dependency.

  `remove [2-part coord] version`

* Change `hash` command so that it checks an optionally supplied hash matches actual hash. If it does not then
  the command exits with a non-zero exit code. Generate a target that verifies that the actual hash matches the
  hash of the `dependencies.yml` that was used to generate the extension. The target is a dependency of the
  `http_file` (if possible) or the leaf targets so that it is only run if a dependency requires it.

  `hash [sha256?]`

* Consider converting to commandline tool named `bzt`

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass. It may be possible to enhance [dependabot](https://dependabot.com/) to do this now that it
  is owned by Github.

* Add nature types for `kotlin`, `raw` (modelled as `files`/`filegroups`), `scala`, `aar`, `j2cl` etc. that
  control how artifacts are defined. Artifacts may support multiple natures.
  - See [Mabel](https://github.com/menny/mabel) for `aar`, `kotlin`
  - See[bazel_maven_repository](https://github.com/square/bazel_maven_repository) for `aar`, `kotlin`
  - Add `j2cl.mode = Import|Library` configuration so that annotation only libraries can be directly imported.
  - Replacements targets should have a nature associated with them. So each target is replaced only if target matches.
  - `Plugin` should propagate the `Java` nature to all of it's dependencies and runtime dependencies. 

* The `ArtifactRecord.getAlias()` method should accept a `Nature` parameter and should perform the suffixing
  in there rather than scattered through the emit methods.

* Add strict mode so that if sources is not present and it has not been marked as not included, the tool will fail.

* Test `ensureAliasesAreUnique` when collision due to nature collision.

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
