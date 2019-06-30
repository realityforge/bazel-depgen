# Change Log

### Unreleased

* Update the `regenerate_depgen_extension` task so that it does not pass the `--quiet` parameter to the underlying depgen command execution. Passing the `--quiet` flag resulted in some useful messages being omitted from the output when the task was run from the command line which ultimately led to problems identifying the cause of errors.

### [v0.02](https://github.com/realityforge/bazel-depgen/tree/v0.02) (2019-06-29)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/v0.01...v0.02)

* The `data` attribute is not supported on the `j2cl_library` rule but leaf dependencies had this attribute added when the `verifyConfigSha256` option is `true`. To work around this limitation the output has been adjusted to generate an additional `java_import` rule with the `data` attribute specified and the `j2cl_library` was updated to use the `java_import` in the `srcs` attribute.

### [v0.01](https://github.com/realityforge/bazel-depgen/tree/v0.01) (2019-06-29)
[Full Changelog](https://github.com/realityforge/bazel-depgen/compare/eb92a2dbd83e6f9c990baf0685f4d8781b10e686...v0.01)

 ‎🎉	Initial super-alpha release ‎🎉.
