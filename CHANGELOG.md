# Change Log

### Unreleased

* Update the `org.realityforge.guiceyloops` artifact to version `0.113`.
* Update the java version required to `17`.
* Remove several typos in `dependencies.yml`

### [v0.17](https://github.com/realityforge/bazel-depgen/tree/v0.17) (2022-05-24) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.16...v0.17)

Changes in this release:

* Replace the `test_class=...` parameter to java_test macro with `use_testrunner = False` as it is the "correct" approach and it avoids unnecessary work by the Bazel runtime.

### [v0.16](https://github.com/realityforge/bazel-depgen/tree/v0.16) (2022-05-23) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.15...v0.16)

Changes in this release:

* If the `BUILD_WORKSPACE_DIRECTORY` environment variable is present then use the value as the current working directory. This is useful when the command is run via `bazel run ...` as it will use the root repository and thus can modify local source rather than acting on the `execroot` created by bazel which will be removed once the command completes.
* Reimplement the generated `verify_config_sha256` target as a `java_binary` target.
* Reimplement the generated `regenerate_depgen_extension` target as a `java_binary` target.
* Derive the repository cache directory a single time rather than once per artifact by caching the derivation at application startup.

### [v0.15](https://github.com/realityforge/bazel-depgen/tree/v0.15) (2022-05-20) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.14...v0.15)

Changes in this release:

* Use a `_` prefix when importing functions from bazel extension files so that the imported symbol is considered "private" and can not be accessed by downstream consumers.
* Change the way we verify the sha256 for `dependencies.yml` configuration file to be a test. This makes it easier to integrate with a more traditional bazel build flow.
* Eliminate the "data" attribute on library rules as it was only partially successful in triggering verification of sha256 on use. Instead, rely upon the generated test target.

### [v0.14](https://github.com/realityforge/bazel-depgen/tree/v0.14) (2022-05-13) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.13...v0.14)

Changes in this release:

* Update the `org.realityforge.guiceyloops` artifact to version `0.110`.
* Update the `org.realityforge.gir` artifact to version `0.12`.
* Ensure that the $ is correctly escaped in modern Bazel by adding additional `\` character to the `regenerate_depgen_extension.sh` script.

### [v0.13](https://github.com/realityforge/bazel-depgen/tree/v0.13) (2021-05-25) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.12...v0.13)

Changes in this release:

* Stop adding javascript assets to the generated bazel extension for artifacts that do not have the J2cl nature.
* Add an alias configuration setting to the per-artifact j2cl/java/plugin configuration sections that allow the user to override the name of the alias generated in the bazel extension.

### [v0.12](https://github.com/realityforge/bazel-depgen/tree/v0.12) (2021-04-16) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.11...v0.12)

Changes in this release:

* Fix bug with the generated code when an artifact with the "J2cl" nature contained js assets. The workspace rule `http_archive` was not loaded before it was used.
* Fix bug in the build file snippet when an artifact with the "J2cl" nature contained js assets to use the correct attribute name to record sources in a `filegroup`.
* Remove `java_import` target created for artifacts with the "J2cl" nature when `verifyConfigSha256` is `true`. This target was unused and did not work as expected.
* Avoid adding `*.native.js` files to the fileset added to `j2cl_library` target as the J2cl infrastructure already detects and handles files with this naming pattern.

### [v0.11](https://github.com/realityforge/bazel-depgen/tree/v0.11) (2021-04-15) · [Full Changelog](https://github.com/spritz/spritz/compare/v0.10...v0.11)

Changes in this release:

* Upgrade the `au.com.stocksoftware.idea.codestyle` artifact to version `1.17`.
* Upgrade the `org.realityforge.guiceyloops` artifact to version `0.106`.
* Upgrade the `org.realityforge.gir` artifact to version `0.11`.
* Sort `omit_*` parameters by symbol in macro declarations.
* Avoid adding a workspace rule for an artifact with no classifier unless the artifact has the "Java" or "Plugin" nature. Artifacts that only have the "J2cl" nature use the `sources` classifier and thus the workspace rule sans classifier is unnecessary.
* Update the bazel version tested against to `3.3.0`.
* Artifacts with a "J2cl" nature that contain *.js files will have the files added to the `j2cl_library` target. The exception is files nested within a directory named "public" as it is assumed that these are js files that are shipped as assets in GWT applications.

### [v0.10](https://github.com/realityforge/bazel-depgen/tree/v0.10) (2019-10-11) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.09...v0.10)

* Upgrade the `org.realityforge.javax.annotation` artifact to version `1.0.1`.
* Cleanup the text emitted when a "known" error occurs by removing stack traces and java exception names. The `DepgenException` class and several subclasses have been created to identify "known" error conditions.
* Move the `exportDeps` configuration property into the `java` section in both the per-artifact and global configuration sections. The configuration does not apply to natures other than `Java`.

### [v0.09](https://github.com/realityforge/bazel-depgen/tree/v0.09) (2019-08-07) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.08...v0.09)

* Expand the configuration created via the `init` sub-command to include all configurations properties currently available.
* Add configuration property to repositories to control the checksum policy. By default a repository is expected to have valid checksums and missing or incorrect checksums will generate an error. The policy can be changed to warn on missing/invalid checksums or to ignore checksums altogether.

### [v0.08](https://github.com/realityforge/bazel-depgen/tree/v0.08) (2019-07-28) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.07...v0.08)

* Fix the `WORKSPACE` file generated by the `init` command so that it correctly references the generated Bazel extension.

### [v0.07](https://github.com/realityforge/bazel-depgen/tree/v0.07) (2019-07-28) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.06...v0.07)

* Fix the `init` command so that it does not require that the config file be present before the command is executed.

### [v0.06](https://github.com/realityforge/bazel-depgen/tree/v0.06) (2019-07-27) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.05...v0.06)

* Upgrade the `org.realityforge.guiceyloops` artifact to version `0.102`.
* Upgrade the `au.com.stocksoftware.idea.codestyle` artifact to version `1.14`.
* Add the `includeExternalAnnotations` configuration to the global configuration and to per-artifact configuration. This configuration controls whether the `annotations` classifier artifact is downloaded. The `annotations` classifier artifacts is used by IntelliJ IDEA to store annotations outside the source code. This is particularly useful for IDEA users so annotations can be added to indicate the language of strings, expected patterns of strings, expected flags for strings etc. These annotations can be added without adding additional IDE-specific dependencies to the source code. If the the `annotations` classifier artifact is not found in the remote repository but the `includeExternalAnnotations` configuration is `true` it is not considered an error and is silently ignored.
* Add the `init` subcommand that initializes a `dependencies.yml` and may optionally generate the associated `WORKSPACE` file if not already present and run the `generate` subcommand. The initialization process assumes that the current directory should contain the `WORKSPACE` file and that the `dependencies.yml` is in the standard location (i.e. `thirdparty/dependencies.yml`). Although the tool allows the specification of the configuration file via the standard means. i.e. Specifying `--config-file path/to/deps.yml` before the subcommand. The `init` subcommand will also accept the arguments `--no-create-workspace` to skip creation of the `WORKSPACE` file and `--no-generate`  to skip the execution of the `generate` subcommand after `init` completes.

### [v0.05](https://github.com/realityforge/bazel-depgen/tree/v0.05) (2019-07-04) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.04...v0.05)

* Generate a more useful error message when the `hash --verify-sha256 ...` command fails and the configuration option `verifyConfigSha256` is `true`. The error message describes the action required to rectify the issue. The error message looks like:

> Content SHA256: 0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96 (Expected 1DF387350EB57FD368BBF68EB397334B6241B5BBA816B67CF10BC69BEC541F07)
  Depgen generated extension file 'thirdparty/dependencies.bzl' is out of date with the configuration file 'thirdparty/dependencies.yml.
  Please run command 'bazel run //thirdparty:regenerate_depgen_extension' to update the extension.

### [v0.04](https://github.com/realityforge/bazel-depgen/tree/v0.04) (2019-07-04) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.03...v0.04)

* Fix a bug where targets for replacements were always prefixed with a `:` character which made it possible to use labels with a path or repository. Now the targets defined in config file must resolve without this prefix but can be any arbitrary label.

### [v0.03](https://github.com/realityforge/bazel-depgen/tree/v0.03) (2019-07-01) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.02...v0.03)

* Update the `regenerate_depgen_extension` task so that it does not pass the `--quiet` parameter to the underlying depgen command execution. Passing the `--quiet` flag resulted in some useful messages being omitted from the output when the task was run from the command line which ultimately led to problems identifying the cause of errors.
* Update the `regenerate_depgen_extension` task so that the user can provide arbitrary parameters to the depgen tool by passing them on the commandline. i.e. `bazel run //thirdparty:regenerate_depgen_extension --  --reset-cached-metadata` will pass `--reset-cached-metadata` to the underlying tool.
* Fix bug where the output as a result of the `--help` argument was omitted if the tool was also passed the `--quiet` argument.
* If a depgen artifact is explicitly declared then it **MUST** have the `all` classifier. This is now validated.

### [v0.02](https://github.com/realityforge/bazel-depgen/tree/v0.02) (2019-06-29) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.01...v0.02)

* The `data` attribute is not supported on the `j2cl_library` rule but leaf dependencies had this attribute added when the `verifyConfigSha256` option is `true`. To work around this limitation the output has been adjusted to generate an additional `java_import` rule with the `data` attribute specified and the `j2cl_library` was updated to use the `java_import` in the `srcs` attribute.

### [v0.01](https://github.com/realityforge/bazel-depgen/tree/v0.01) (2019-06-29) · [Full Changelog](https://github.com/realityforge/bazel-depgen/compare/eb92a2dbd83e6f9c990baf0685f4d8781b10e686...v0.01)

 🎉	Initial super-alpha release 🎉.
