# TODO

This document is essentially a list of shorthand notes describing work yet to be completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Add `upgrade` command that updates depgen dependency.

* Optionally pass output through [buildifier](https://github.com/bazelbuild/buildtools/tree/master/buildifier).

* Consider adding `buildifier` back into the build project. It can either verify that the output bazel files do not
  generate any warnings by running `buildifier --lint=warn` on any generated file as part of our build process _or_
  we could run `buildifier --lint=fix` when we output files for a slightly improved forward compatibility?

* Separate `dependenciez.bzl` so that workspace macro goes in `deps.bzl` and target macro into `rules.bzl`?

* Currently, even if an artifact is restricted to a repository it is looked up in all repositories. Figure out a
  way to restrict it to a specific repository.

* If the j2cl artifact already has -j2cl suffix ... then don't add another?

* order omits based on omit key rather than underlying artifact key

* Multiple artifact classifiers from a single coordinate
  - can I get the `test-sources` classifier when also getting the `jar` classifier?

* Add parameter like `--reset-cached-metadata` that only resets failed lookups. Useful when mirrors take a while to
  propagate and repositories are add-only.

* Fix error when unable to resolve to include dependency trace. i.e. We can get a trace like:

```
INFO: Build completed successfully, 1 total action
Downloading: https://repo.maven.apache.org/maven2/colt/colt/1.2.0/colt-1.2.0-sources.jar
Downloading: https://repo.maven.apache.org/maven2/tapestry/tapestry/4.0.2/tapestry-4.0.2-sources.jar
Unable to locate source for artifact 'colt:colt:jar:1.2.0'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.
```

Where it is unclear why colt is included. Maybe emitting the dependency graph or at least the path to root dependency would give a better explanation.

* Add command that checks/validates/cleans cache.

* Add `init` command that emits a dependency.yml from template.

* Add `add` command that adds a dependency.

  `add [coord] --alias foo --nature java --nature plugin --include-optional --include-source --export-deps --generates-api --excludes com.example:base --excludes com.example:base --visibility //blah  --visibility //blee --j2cl-suppress blah`

* Add `remove` command that removes a dependency.

  `remove [coord]`

* Add `update` command that updates the version of a dependency.

  `remove [2-part coord] version`

* Refactor tests so that by default they don't call out to bazel except when needed. This should speed
  up tests.

* Consider converting to commandline tool named `bzt`

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass. It may be possible to enhance [dependabot](https://dependabot.com/) to do this now that it
  is owned by Github. I can probably hack it together via: https://github.com/dependabot/dependabot-core

* Add nature types for `kotlin`, `raw` (modelled as `files`/`filegroups`), `scala`, `aar`, `j2cl` etc. that
  control how artifacts are defined. Artifacts may support multiple natures.
  - See [Mabel](https://github.com/menny/mabel) for `aar`, `kotlin`
  - See[bazel_maven_repository](https://github.com/square/bazel_maven_repository) for `aar`, `kotlin`

* Add support for `neverlink` on artifacts.

* `http_file` now supports basic authentication via `.netrc`. See if we can figure out a way to align the two
  `settings.xml` and `.netrc`. Perhaps by moving to `.netrc` exclusively. Then we could also remove names from
  repositories and cache via url.

* Gazelle for java already does some of scanning. Use or copy? https://github.com/bazel-contrib/rules_jvm/tree/main/java/gazelle
* Optionally run something like [jvm-classpath-validator](https://github.com/or-shachar/jvm-classpath-validator)
  on produced libraries to verify that the classpaths do not have collisions. A more advanced tool may even go
  further and generate a list of all java classes in each artifact. Other tools could pick detect when compiles
  fail due to missing classes and suggest which libraries to add to which modules. For this it may be required to
  write this tool from scratch.
- https://github.com/classgraph/classgraph to scan java to generate java build files?
- Most likely we will want to support a tool like [BUILD_file_generator](https://github.com/bazelbuild/BUILD_file_generator)
  or [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps), both of which scan java files and
  automagically creates `BUILD` files (somehow?) so that there is fine grain dependencies without the heartache.
  Also see [ThirdPartyDepsAnalyzer](https://github.com/google/startup-os/blob/b10384644056cc9ac44388a76dbd0a4a8350e76d/tools/build_file_generator/ThirdPartyDepsAnalyzer.java) and friends. (See [migrating-gjf-to-bazel.md](https://github.com/cgrushko/text/blob/master/migrating-gjf-to-bazel.md))

Places to look for bazel inspiration:

* https://github.com/sgammon/elide
* https://github.com/vaticle/dependencies/tree/master/tool/unuseddeps
* https://github.com/vaticle/dependencies/tree/master/tool/checkstyle
* https://github.com/envoyproxy/envoy/blob/master/.bazelrc
