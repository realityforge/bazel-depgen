# Change Log

### Unreleased

* Upgrade the `au.com.stocksoftware.idea.codestyle` artifact to version `1.14`.

### [v0.05](https://github.com/realityforge/bazel-depgen/tree/v0.05) (2019-07-04)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.04...v0.05)

* Generate a more useful error message when the `hash --verify-sha256 ...` command fails and the configuration option `verifyConfigSha256` is `true`. The error message describes the action required to rectify the issue. The error message looks like:

> Content SHA256: 0A8DBED4B09238126BA5E065EB4E392A1B631FA1A20FCA9AE1DF5AA364F59C96 (Expected 1DF387350EB57FD368BBF68EB397334B6241B5BBA816B67CF10BC69BEC541F07)
  Depgen generated extension file 'thirdparty/dependencies.bzl' is out of date with the configuration file 'thirdparty/dependencies.yml.
  Please run command 'bazel run //thirdparty:regenerate_depgen_extension' to update the extension.

### [v0.04](https://github.com/realityforge/bazel-depgen/tree/v0.04) (2019-07-04)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.03...v0.04)

* Fix a bug where targets for replacements were always prefixed with a `:` character which made it possible to use labels with a path or repository. Now the targets defined in config file must resolve without this prefix but can be any arbitrary label.

### [v0.03](https://github.com/realityforge/bazel-depgen/tree/v0.03) (2019-07-01)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.02...v0.03)

* Update the `regenerate_depgen_extension` task so that it does not pass the `--quiet` parameter to the underlying depgen command execution. Passing the `--quiet` flag resulted in some useful messages being omitted from the output when the task was run from the command line which ultimately led to problems identifying the cause of errors.
* Update the `regenerate_depgen_extension` task so that the user can provide arbitrary parameters to the depgen tool by passing them on the commandline. i.e. `bazel run //thirdparty:regenerate_depgen_extension --  --reset-cached-metadata` will pass `--reset-cached-metadata` to the underlying tool.
* Fix bug where the output as a result of the `--help` argument was omitted if the tool was also passed the `--quiet` argument.
* If a depgen artifact is explicitly declared then it **MUST** have the `all` classifier. This is now validated.

### [v0.02](https://github.com/realityforge/bazel-depgen/tree/v0.02) (2019-06-29)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.01...v0.02)

* The `data` attribute is not supported on the `j2cl_library` rule but leaf dependencies had this attribute added when the `verifyConfigSha256` option is `true`. To work around this limitation the output has been adjusted to generate an additional `java_import` rule with the `data` attribute specified and the `j2cl_library` was updated to use the `java_import` in the `srcs` attribute.

### [v0.01](https://github.com/realityforge/bazel-depgen/tree/v0.01) (2019-06-29)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/eb92a2dbd83e6f9c990baf0685f4d8781b10e686...v0.01)

 ‎🎉	Initial super-alpha release ‎🎉.
