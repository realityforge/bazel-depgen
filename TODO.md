# TODO

This document is essentially a list of shorthand notes describing work yet to be completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Add `upgrade` command that updates depgen dependency.

* Upgrade to Bazel 3.1 - see https://mail.google.com/mail/u/0/#inbox/FMfcgxwHMsVGfMbZWPsxdNxlwjxVQZdK

* Optionally run something like [jvm-classpath-validator](https://github.com/or-shachar/jvm-classpath-validator)
  on produced libraries to verify that the classpaths do not have collisions. A more advanced tool may even go
  further and generate a list of all java classes in each artifact. Other tools could pick detect when compiles
  fail due to missing classes and suggest which libraries to add to which modules. For this it may be required to
  write this tool from scratch.

* Also run unused deps tool?

* Re-add licensing back into plugin. See https://docs.google.com/document/u/0/d/1uwBuhAoBNrw8tmFs-NxlssI6VRolidGYdYqagLqHWt8/mobilebasic

* Consider adding `buildifier` back into the build project. It can either verify that the output bazel files do not
  generate any warnings by running `buildifier --lint=warn` on any generated file as part of our build process _or_
  we could run `buildifier --lint=fix` when we output files for a slightly improved forward compatibility?

* Consider adding support for workspace rules for java etc via

```
   if not omit_rules_java:
        http_archive(
            name = "rules_java",
            strip_prefix = "rules_java-0.1.1",
            sha256 = "6b753b0c02b7fc1902e39f4f330c5958a42f31ea7832c11c11e5c11306292c27",
            urls = ["https://github.com/bazelbuild/rules_java/archive/0.1.1.tar.gz"],
        )
```

* Start to separate `dependenciez.bzl` according to latest conventions. i.e. put workspace rules in `repositories.bzl`
  in dependencies directory. Or maybe not as we probably want to be able to explicitly add customized rules. Maybe we
  generate it there but add configuration to put it elsewhere? After all we are not generating rules but just deps

  See https://docs.bazel.build/versions/master/skylark/deploying.html#dependencies

* Currently even if an artifact is restricted to a repository it is looked up in all repositories. Figure out a
  way to restrict it to a specific repository.

* Shutdown bazel servers after test

* Read a global `.depgen.rc` so can use specify settings such as `settings.xml` location and the cache
  directory to some central place (i.e. `~/.depgen/repository`)

* See if we can use aspects rather than data attributes to check generated is uptodate?

* Consider trying to get `j2cl_library` to support `data` attribute and remove cruft from generated infrastructure.

* Add parameter like `--reset-cached-metadata` that only resets failed lookups. Useful when mirrors take a while to
  propagate and repositories are add-only.

* Add command that checks/validates/cleans cache.

* Add `add` command that adds a dependency.

  `add [coord] --alias foo --nature java --nature plugin --include-optional --include-source --export-deps --generates-api --excludes com.example:base --excludes com.example:base --visibility //blah  --visibility //blee --j2cl-suppress blah`

* Add `remove` command that removes a dependency.

  `remove [coord]`

* Add `update` command that updates the version of a dependency.

  `remove [2-part coord] version`

* Refactor tests so that by default they don't call out to bazel except when needed. This should speed
  up tests.

* When an artifact has the `J2cl` nature we should consider whether we analyze the jars and extract any of the
  `*.native.js`, `*.extern.js` or `*.js` files and add them to the `j2cl_library` macro manually.

* Consider converting to commandline tool named `bzt`

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass. It may be possible to enhance [dependabot](https://dependabot.com/) to do this now that it
  is owned by Github. I can probably hack it together via: https://github.com/dependabot/dependabot-core

* Add nature types for `kotlin`, `raw` (modelled as `files`/`filegroups`), `scala`, `aar`, `j2cl` etc. that
  control how artifacts are defined. Artifacts may support multiple natures.
  - See [Mabel](https://github.com/menny/mabel) for `aar`, `kotlin`
  - See[bazel_maven_repository](https://github.com/square/bazel_maven_repository) for `aar`, `kotlin`

* Optionally pass output through [buildifier](https://github.com/bazelbuild/buildtools/tree/master/buildifier).

* Add support for managed dependencies which essentially contains a list of artifacts that include version

* Add support for `neverlink` on artifacts.

* `http_file` now supports basic authentication via `.netrc`. See if we can figure out a way to align the two
  `settings.xml` and `.netrc`. Perhaps by moving to `.netrc` exclusively. Then we could also remove names from
  repositories and cache via url.

* Add in the ability to do the outputs from bazel-deps as an external repo
    - see https://github.com/johnynek/bazel-deps/commit/48fdf7f8bcf3aadfa07f9f7e6f0c9f4247cb0f58

* Look to [bazel-tools](https://github.com/spotify/bazel-tools) and [awesome-bazel](https://github.com/jin/awesome-bazel)
  to see if there is other tools that can be incoporated.
  - Most likely we will want to support a tool like [BUILD_file_generator](https://github.com/bazelbuild/BUILD_file_generator)
    or [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps), both of which scan java files and
    automagically creates `BUILD` files (somehow?) so that there is fine grain dependencies without the heartache.
  - Another option is [exodus](https://github.com/wix/exodus)

https://github.com/thundergolfer/bazel-linting-system

* Also look to tools like [bazel-java-builder-template](https://github.com/salesforce/bazel-java-builder-template)
  that demonstrate how to build code generation tools.

* Look at [pomgen](https://github.com/salesforce/pomgen) - groups packages into an artifact (i.e. a
   maven-esque artifact) and groups artifacts into libraries (i.e. multiple artifacts that share a
   version and probably groupId and are released together)

* Look at training materials in https://github.com/OasisDigital/bazelcon-2019

Figure out how to use

https://docs.bazel.build/versions/master/skylark/lib/globals.html#workspace and
managed_directories for exposing generated code so IDE can see it

* [Cirrus CI](https://cirrus-ci.org/features/) - A CI that has goodish support for bazel caches and free for OS

Other places to look for bazel inspiration

* https://github.com/batfish/batfish/tree/master/skylark

* https://github.com/sgammon/GUST

* Look at configuration in https://github.com/envoyproxy/envoy/blob/master/.bazelrc

## Bazel Summary

* The basic build unit is a target
* Targets are instances of rules

Always use `--unused-dependency-checker=ERROR` `--srtict-java-deps=ERROR`

And from https://github.com/spotify/bazel-tools

depfuzz - A tool for removing unused dependencies with a fuzzing strategy.
expand-macros - A tool for expanding Bazel macros into the rules that they generate.
format - A tool for formatting all files in the repository according to common style guides.
unused - A tool for showing source files that are not used in the build.
sync-deps - A tool for synchronizing third-party dependencies.
sync-repos - A tool for synchronizing third-party repositories.
