load("@rules_java//java/common:java_info.bzl", "JavaInfo")

ReleaseVersionInfo = provider("Release version selected by //tools/release:version.", fields = ["value"])

def _version_impl(ctx):
    return [ReleaseVersionInfo(value = ctx.build_setting_value)]

release_version = rule(
    implementation = _version_impl,
    build_setting = config.string(flag = True),
)

def _release_config_properties_impl(ctx):
    version = ctx.attr.version[ReleaseVersionInfo].value
    out = ctx.actions.declare_file("config.properties")
    ctx.actions.write(
        output = out,
        content = "group=org.realityforge.bazel.depgen\nid=bazel-depgen\nversion=%s\n" % version,
    )
    return [DefaultInfo(files = depset([out]))]

release_config_properties = rule(
    implementation = _release_config_properties_impl,
    attrs = {
        "version": attr.label(default = Label("//tools/release:version")),
    },
)

def _direct_java_class_jars(java_targets):
    jars = []
    for target in java_targets:
        jars.extend(target[JavaInfo].runtime_output_jars)
    return jars

def _direct_java_source_jars(java_targets):
    jars = []
    for target in java_targets:
        jars.extend(target[JavaInfo].source_jars)
    return jars

def _release_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    class_jars = _direct_java_class_jars(ctx.attr.java_targets)
    input_jars = class_jars + ctx.files.jars

    args = ctx.actions.args()
    args.add("merge")
    args.add("--output", out)
    if ctx.attr.main_class:
        args.add("--main-class", ctx.attr.main_class)
    args.add_all(input_jars, before_each = "--input")

    ctx.actions.run(
        executable = ctx.executable._jar_builder,
        arguments = [args],
        inputs = input_jars,
        outputs = [out],
        mnemonic = "ReleaseJar",
        progress_message = "Building release jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_jar = rule(
    implementation = _release_jar_impl,
    attrs = {
        "jars": attr.label_list(allow_files = [".jar"]),
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "main_class": attr.string(),
        "out": attr.string(mandatory = True),
        "_jar_builder": attr.label(
            default = Label("//tools/release:jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _release_source_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    source_jars = _direct_java_source_jars(ctx.attr.java_targets)
    resource_files = ctx.files.resource_files
    if len(resource_files) != len(ctx.attr.resource_entries):
        fail("resource_files and resource_entries must have the same length")

    args = ctx.actions.args()
    args.add("merge")
    args.add("--output", out)
    args.add_all(source_jars, before_each = "--input")
    for index in range(len(resource_files)):
        args.add("--resource")
        args.add("%s=%s" % (resource_files[index].path, ctx.attr.resource_entries[index]))

    ctx.actions.run(
        executable = ctx.executable._jar_builder,
        arguments = [args],
        inputs = source_jars + resource_files,
        outputs = [out],
        mnemonic = "ReleaseSourceJar",
        progress_message = "Building release source jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_source_jar = rule(
    implementation = _release_source_jar_impl,
    attrs = {
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "out": attr.string(mandatory = True),
        "resource_entries": attr.string_list(),
        "resource_files": attr.label_list(allow_files = True),
        "_jar_builder": attr.label(
            default = Label("//tools/release:jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _release_javadoc_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    source_jars = _direct_java_source_jars(ctx.attr.java_targets)
    classpath = _direct_java_class_jars(ctx.attr.java_targets) + ctx.files.classpath

    args = ctx.actions.args()
    args.add("--output", out)
    args.add_all(source_jars, before_each = "--source-jar")
    args.add_all(classpath, before_each = "--classpath")

    ctx.actions.run(
        executable = ctx.executable._javadoc_jar_builder,
        arguments = [args],
        inputs = source_jars + classpath,
        outputs = [out],
        mnemonic = "ReleaseJavadocJar",
        progress_message = "Building release Javadocs jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_javadoc_jar = rule(
    implementation = _release_javadoc_jar_impl,
    attrs = {
        "classpath": attr.label_list(allow_files = [".jar"]),
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "out": attr.string(mandatory = True),
        "_javadoc_jar_builder": attr.label(
            default = Label("//tools/release:javadoc_jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _pom_content(version):
    return """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.realityforge.bazel.depgen</groupId>
  <artifactId>bazel-depgen</artifactId>
  <version>%s</version>
  <packaging>jar</packaging>
  <name>bazel-depgen: Generate Bazel dependency scripts by traversing Maven repositories</name>
  <description>bazel-depgen: Generate Bazel dependency scripts by traversing Maven repositories</description>
  <url>https://github.com/realityforge/bazel-depgen</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:realityforge/bazel-depgen.git</connection>
    <developerConnection>scm:git:git@github.com:realityforge/bazel-depgen.git</developerConnection>
    <url>git@github.com:realityforge/bazel-depgen.git</url>
  </scm>
  <issueManagement>
    <url>https://github.com/realityforge/bazel-depgen/issues</url>
    <system>GitHub Issues</system>
  </issueManagement>
  <developers>
    <developer>
      <id>realityforge</id>
      <name>Peter Donald</name>
    </developer>
  </developers>
  <dependencies>
  </dependencies>
</project>
""" % version

def _release_pom_impl(ctx):
    version = ctx.attr.version[ReleaseVersionInfo].value
    out = ctx.actions.declare_file(ctx.attr.out)
    ctx.actions.write(output = out, content = _pom_content(version))
    return [DefaultInfo(files = depset([out]))]

release_pom = rule(
    implementation = _release_pom_impl,
    attrs = {
        "out": attr.string(mandatory = True),
        "version": attr.label(default = Label("//tools/release:version")),
    },
)

def _release_version_file_impl(ctx):
    version = ctx.attr.version[ReleaseVersionInfo].value
    out = ctx.actions.declare_file(ctx.attr.out)
    ctx.actions.write(output = out, content = version + "\n")
    return [DefaultInfo(files = depset([out]))]

release_version_file = rule(
    implementation = _release_version_file_impl,
    attrs = {
        "out": attr.string(mandatory = True),
        "version": attr.label(default = Label("//tools/release:version")),
    },
)
