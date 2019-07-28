# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Init command
  - fill out template

* `exportDeps` - should that be below a java config section both globally and at the artifact level?

* Currently even if an artifact is restricted to a repository it is looked up in all repositories. Figure out a
  way to restrict it to a specific repository. 

* Shutdown bazel servers after test

* Read a global `.depgen.rc` so can use specify settings such as `settings.xml` location and the cache
  directory to some central place (i.e. `~/.depgen/repository`)

* Consider trying to get `j2cl_library` to support `data` attribute and remove cruft from generated infrastructure.

* Add command that checks/validates/cleans cache.

* Annotate repositories with configuration to indicate that they may not have valid checksums:. Avoid messages like

```
 Downloading: .../org/realityforge/bazel/depgen/bazel-depgen/X/bazel-depgen-X-sources.jar
 Jun 28, 2019 11:20:50 PM org.eclipse.aether.internal.impl.WarnChecksumPolicy onTransferChecksumFailure
 WARNING: Could not validate integrity of download from .../org/realityforge/bazel/depgen/bazel-depgen/X/bazel-depgen-X-sources.jar: Checksum validation failed, no checksums available
 Transfer Corrupted: org/realityforge/bazel/depgen/bazel-depgen/X/bazel-depgen-X-sources.jar due to org.eclipse.aether.transfer.ChecksumFailureException: Checksum validation failed, no checksums available
```
  Valid behaviour should be to fail if this error occurs. We should also add configuration to repositories to
  indicate a repository does not support checksums.

* Cleanup error output like following. Stop duplicating message and remove stacktrace as it is a "known" error and
  remove the exception name. This may involve replacing lots of errors in codebase with new exception type such as
  `DepgenException` or `DepgenValidationException` or `DepgenConfigurationException`

```
java.lang.IllegalStateException: Artifact 'org.realityforge.com.google.jsinterop:base:jar:1.0.0-b2-e6d791f' declared target for nature 'J2cl' but artifact does not have specified nature.
java.lang.IllegalStateException: Artifact 'org.realityforge.com.google.jsinterop:base:jar:1.0.0-b2-e6d791f' declared target for nature 'J2cl' but artifact does not have specified nature.
        at org.realityforge.bazel.depgen.record.ArtifactRecord.validate(ArtifactRecord.java:214)
        at java.util.ArrayList.forEach(ArrayList.java:1257)
        at org.realityforge.bazel.depgen.record.ApplicationRecord.build(ApplicationRecord.java:51)
        at org.realityforge.bazel.depgen.Main.loadRecord(Main.java:194)
        at org.realityforge.bazel.depgen.CommandContextImpl.loadRecord(CommandContextImpl.java:38)
        at org.realityforge.bazel.depgen.GenerateCommand.run(GenerateCommand.java:20)
        at org.realityforge.bazel.depgen.Main.run(Main.java:130)
        at org.realityforge.bazel.depgen.Main.main(Main.java:118)
```

* Add `add` command that adds a dependency.

  `add [coord] --alias foo --nature java --nature plugin --include-optional --include-source --export-deps --generates-api --excludes com.example:base --excludes com.example:base --visibility //blah  --visibility //blee --j2cl-suppress blah`

* Add `remove` command that removes a dependency.

  `remove [coord]`

* Add `update` command that updates the version of a dependency.

  `remove [2-part coord] version`

* Add `upgrade` command that updates depgen dependency.

* Refactor tests so that by default they don't call out to bazel except when needed. This should speed
  up tests.

* When an artifact has the `J2cl` nature we should consider whether we analyze the jars and extract any of the
  `*.native.js`, `*.extern.js` or `*.js` files and add them to the `j2cl_library` macro manually.

* Consider converting to commandline tool named `bzt`

* Consider adding a Github Action that bumps dependencies and runs tests as appropriate. It could generate a PR if
  all the tests pass. It may be possible to enhance [dependabot](https://dependabot.com/) to do this now that it
  is owned by Github.

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

* Look to [bazel-tools](https://github.com/spotify/bazel-tools) and [awesome-bazel](https://github.com/jin/awesome-bazel)
  to see if there is other tools that can be incoporated.
  - Most likely we will want to support a tool like [BUILD_file_generator](https://github.com/bazelbuild/BUILD_file_generator)
    or [tools_jvm_autodeps](https://github.com/cgrushko/tools_jvm_autodeps), both of which scan java files and
    automagically creates `BUILD` files (somehow?) so that there is fine grain dependencies without the heartache.
  - Another option is [exodus](https://github.com/wix/exodus)
