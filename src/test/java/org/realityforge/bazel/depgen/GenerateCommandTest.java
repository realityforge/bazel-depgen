package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.testng.annotations.Test;

public class GenerateCommandTest extends AbstractTest {
    @Test
    public void generate() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        final String url = dir.toUri().toString();

        writeWorkspace();
        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");
        final ApplicationModel model = loadApplicationModel();

        final var handler = new TestHandler();
        final var command = new GenerateCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment()));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(handler.toString(), "");

        assertFalse(Files.exists(FileUtil.getCurrentDirectory().resolve("BUILD.bazel")));
        assertEquals(loadAsString(FileUtil.getCurrentDirectory().resolve("thirdparty/BUILD.bazel")), """
            # File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen\
             version 1
            # Contents can be edited and will not be overridden.
            package(default_visibility = ["//visibility:public"])

            load("//thirdparty:dependencies.bzl", "generate_targets")

            generate_targets()

            exports_files(["dependencies.yml"])
            """);
        assertEquals(
                loadAsString(
                        FileUtil.getCurrentDirectory().resolve("thirdparty/dependencies.bzl"),
                        model.getConfigSha256(),
                        url),
                """
                # DO NOT EDIT: File is auto-generated from dependencies.yml by\
                 https://github.com/realityforge/bazel-depgen version 1

                ""\"
                    Macro rules to load dependencies.

                    Invoke 'generate_workspace_rules' from a WORKSPACE file.
                    Invoke 'generate_targets' from a BUILD.bazel file.
                ""\"
                # Dependency Graph Generated from the input data
                # \\- com.example:myapp:jar:1.0 [compile]

                load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
                load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
                 "java_import", _java_test = "java_test")

                # SHA256 of the configuration content that generated this file
                _CONFIG_SHA256 = "MYSHA"

                def generate_workspace_rules():
                    ""\"
                        Repository rules macro to load dependencies.

                        Must be run from a WORKSPACE file.
                    ""\"

                    _http_file(
                        name = "com_example__myapp__1_0",
                        downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                    )

                    _http_file(
                        name = "com_example__myapp__1_0__sources",
                        downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                    )

                    _http_file(
                        name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                        downloaded_file_path =\
                 "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls =\
                 ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                    )

                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--verbose",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--verbose",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                    )

                    _java_import(
                        name = "org_realityforge_bazel_depgen__bazel_depgen",
                        jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                        tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                    )
                """);
    }

    @Test
    public void generate_buildFilesExist() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeWorkspace();
        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path configPackage = FileUtil.getCurrentDirectory().resolve("BUILD.bazel");
        final Path extensionPackage = FileUtil.getCurrentDirectory().resolve("thirdparty/BUILD.bazel");

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");
        FileUtil.write(configPackage, "");
        FileUtil.write(extensionPackage, "");

        final ApplicationModel model = loadApplicationModel();

        final var handler = new TestHandler();
        final var command = new GenerateCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment()));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(handler.toString(), "");

        // File contents not changed
        assertEquals(loadAsString(configPackage), "");
        // File contents not changed
        assertEquals(loadAsString(extensionPackage), "");

        assertEquals(
                loadAsString(
                        FileUtil.getCurrentDirectory().resolve("thirdparty/dependencies.bzl"),
                        model.getConfigSha256(),
                        dir.toUri().toString()),
                """
                # DO NOT EDIT: File is auto-generated from dependencies.yml by\
                 https://github.com/realityforge/bazel-depgen version 1

                ""\"
                    Macro rules to load dependencies.

                    Invoke 'generate_workspace_rules' from a WORKSPACE file.
                    Invoke 'generate_targets' from a BUILD.bazel file.
                ""\"
                # Dependency Graph Generated from the input data
                # \\- com.example:myapp:jar:1.0 [compile]

                load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
                load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
                 "java_import", _java_test = "java_test")

                # SHA256 of the configuration content that generated this file
                _CONFIG_SHA256 = "MYSHA"

                def generate_workspace_rules():
                    ""\"
                        Repository rules macro to load dependencies.

                        Must be run from a WORKSPACE file.
                    ""\"

                    _http_file(
                        name = "com_example__myapp__1_0",
                        downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                    )

                    _http_file(
                        name = "com_example__myapp__1_0__sources",
                        downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                    )

                    _http_file(
                        name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                        downloaded_file_path =\
                 "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls =\
                 ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                    )

                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--verbose",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--verbose",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                    )

                    _java_import(
                        name = "org_realityforge_bazel_depgen__bazel_depgen",
                        jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                        tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                    )
                """);
    }

    @Test
    public void generate_directoryIsAFile() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeWorkspace();
        writeConfigFile(dir, """
            options:
              extensionFile: somedir/dependencies.bzl
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        FileUtil.write(FileUtil.getCurrentDirectory().resolve("thirdparty").resolve("somedir"), "");

        final var handler = new TestHandler();
        final var command = new GenerateCommand();

        assertThrows(IOException.class, () -> command.run(new CommandContextImpl(newEnvironment())));
        assertEquals(handler.toString(), "");
    }

    @Test
    public void generate_canNotCreateThirdpartyDirectory() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeWorkspace();
        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord applicationRecord = loadApplicationRecord();

        final var handler = new TestHandler();
        final var command = new GenerateCommand();

        final Environment environment = newEnvironment();

        final var perms = new HashSet<PosixFilePermission>();
        perms.add(PosixFilePermission.OWNER_READ);
        Files.setPosixFilePermissions(FileUtil.getCurrentDirectory(), perms);

        try {
            final DepgenException exception = expectThrows(
                    DepgenException.class,
                    () -> command.run(new Command.Context() {
                        @NonNull
                        @Override
                        public Environment environment() {
                            return environment;
                        }

                        @NonNull
                        @Override
                        public ApplicationModel loadModel() {
                            return applicationRecord.getSource();
                        }

                        @NonNull
                        @Override
                        public ApplicationRecord loadRecord() {
                            return applicationRecord;
                        }
                    }));
            assertEquals(
                    exception.getMessage(),
                    "Failed to create directory "
                            + FileUtil.getCurrentDirectory().resolve("thirdparty"));
        } finally {
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(FileUtil.getCurrentDirectory(), perms);
        }
        assertEquals(handler.toString(), "");
    }

    @Test
    public void generate_repositoryRulesInModule_targetsInExtension() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeModule();
        FileUtil.write("MODULE.bazel", """
            module(name = "test")

            # --- depgen-generated repository rules start ---

            # --- depgen-generated repository rules end ---
            """);
        writeConfigFile(dir, """
            options:
              repositoryRuleGenerationStrategy: module
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final var command = new GenerateCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment()));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);

        assertEquals(
                loadAsString(FileUtil.getCurrentDirectory().resolve("MODULE.bazel")),
                "module(name = \"test\")\n"
                    + "\n"
                    + "# --- depgen-generated repository rules start ---\n"
                    + "\n"
                    + "# DO NOT EDIT: Content is auto-generated from //thirdparty:dependencies.yml by"
                    + " https://github.com/realityforge/bazel-depgen version 1\n"
                    + "\n"
                    + "_http_file = use_repo_rule(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n"
                    + "\n"
                    + "_http_file(\n"
                    + "    name = \"com_example__myapp__1_0\",\n"
                    + "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n"
                    + "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                    + "    urls = [\""
                        + dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar\"],\n" + ")\n"
                        + "\n"
                        + "_http_file(\n"
                        + "    name = \"com_example__myapp__1_0__sources\",\n"
                        + "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n"
                        + "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                        + "    urls = [\""
                        + dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n"
                        + ")\n"
                        + "\n"
                        + "_http_file(\n"
                        + "    name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n"
                        + "    downloaded_file_path ="
                        + " \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n"
                        + "    sha256 ="
                        + " \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                        + "    urls = [\""
                        + dir.toUri() + "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n"
                        + ")\n"
                        + "\n"
                        + "# --- depgen-generated repository rules end ---\n");
        final String extension = loadAsString(FileUtil.getCurrentDirectory().resolve("thirdparty/dependencies.bzl"));
        assertFalse(extension.contains("def generate_workspace_rules():"));
        assertTrue(extension.contains("def generate_targets():"));
    }

    @Test
    public void generate_repositoryRulesInModule_targetsInExtension_j2clWithDependenciesAndJsAssets() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeModule();
        FileUtil.write("MODULE.bazel", """
            module(name = "test")

            # --- depgen-generated repository rules start ---

            # --- depgen-generated repository rules end ---
            """);
        writeConfigFile(dir, """
            options:
              repositoryRuleGenerationStrategy: module
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);

        final Path jarFile1 = createJarFile(outputStream -> {
            createJarEntry(outputStream, "com/biz/MyFile1.js", "");
            createJarEntry(outputStream, "com/biz/MyOtherFile.js", "");
            createJarEntry(outputStream, "com/biz/MyBlah.js", "");
            createJarEntry(outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "");
            createJarEntry(outputStream, "com/biz/TheClass.native.js", "");
            createJarEntry(outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "");
        });
        final Path jarFile2 = createJarFile("foo.js", "");
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0", jarFile1);
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", jarFile2);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");

        final var command = new GenerateCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment()));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);

        assertEquals(
                loadAsString(FileUtil.getCurrentDirectory().resolve("MODULE.bazel")),
                "module(name = \"test\")\n"
                    + "\n"
                    + "# --- depgen-generated repository rules start ---\n"
                    + "\n"
                    + "# DO NOT EDIT: Content is auto-generated from //thirdparty:dependencies.yml by"
                    + " https://github.com/realityforge/bazel-depgen version 1\n"
                    + "\n"
                    + "_http_file = use_repo_rule(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n"
                    + "_http_archive = use_repo_rule(\"@bazel_tools//tools/build_defs/repo:http.bzl\","
                    + " \"http_archive\")\n"
                    + "\n"
                    + "_http_file(\n"
                    + "    name = \"com_example__myapp__1_0__sources\",\n"
                    + "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n"
                    + "    sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n"
                    + "    urls = [\""
                        + dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" + ")\n"
                        + "\n"
                        + "_http_archive(\n"
                        + "    name = \"com_example__myapp__1_0__js_sources\",\n"
                        + "    sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n"
                        + "    urls = [\""
                        + dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n"
                        + "    build_file_content = \"\"\"\n"
                        + "filegroup(\n"
                        + "    name = \"srcs\",\n"
                        + "    visibility = [\"//visibility:public\"],\n"
                        + "    srcs = [\"foo.js\"],\n"
                        + ")\n"
                        + "\"\"\",\n"
                        + ")\n"
                        + "\n"
                        + "_http_file(\n"
                        + "    name = \"com_example__mylib__1_0__sources\",\n"
                        + "    downloaded_file_path = \"com/example/mylib/1.0/mylib-1.0-sources.jar\",\n"
                        + "    sha256 = \"e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f\",\n"
                        + "    urls = [\""
                        + dir.toUri() + "com/example/mylib/1.0/mylib-1.0-sources.jar\"],\n" + ")\n"
                        + "\n"
                        + "_http_archive(\n"
                        + "    name = \"com_example__mylib__1_0__js_sources\",\n"
                        + "    sha256 = \"e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f\",\n"
                        + "    urls = [\""
                        + dir.toUri() + "com/example/mylib/1.0/mylib-1.0-sources.jar\"],\n"
                        + "    build_file_content = \"\"\"\n"
                        + "filegroup(\n"
                        + "    name = \"srcs\",\n"
                        + "    visibility = [\"//visibility:public\"],\n"
                        + "    srcs = [\n"
                        + "        \"com/biz/MyBlah.js\",\n"
                        + "        \"com/biz/MyFile1.js\",\n"
                        + "        \"com/biz/MyOtherFile.js\",\n"
                        + "    ],\n"
                        + ")\n"
                        + "\"\"\",\n"
                        + ")\n"
                        + "\n"
                        + "_http_file(\n"
                        + "    name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n"
                        + "    downloaded_file_path ="
                        + " \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n"
                        + "    sha256 ="
                        + " \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                        + "    urls = [\""
                        + dir.toUri() + "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n"
                        + ")\n"
                        + "\n"
                        + "# --- depgen-generated repository rules end ---\n");
        final String extension = loadAsString(FileUtil.getCurrentDirectory().resolve("thirdparty/dependencies.bzl"));
        assertFalse(extension.contains("def generate_workspace_rules():"));
        assertTrue(extension.contains("def generate_targets():"));
        assertTrue(extension.contains("@com_example__myapp__1_0__js_sources//:srcs"));
        assertTrue(extension.contains("@com_example__mylib__1_0__js_sources//:srcs"));
    }

    @Test
    public void generate_repositoryRulesInExtension_targetsInBuild() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeWorkspace();
        writeConfigFile(dir, """
            options:
              targetGenerationStrategy: build
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path buildFile = FileUtil.getCurrentDirectory().resolve("thirdparty/BUILD.bazel");
        FileUtil.write(buildFile, """
            package(default_visibility = ["//visibility:public"])

            # --- depgen-generated targets start ---

            # --- depgen-generated targets end ---
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final var command = new GenerateCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment()));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);

        final String extension = loadAsString(FileUtil.getCurrentDirectory().resolve("thirdparty/dependencies.bzl"));
        assertTrue(extension.contains("def generate_workspace_rules():"));
        assertFalse(extension.contains("def generate_targets():"));
        assertFalse(extension.contains("# Dependency Graph Generated from the input data"));

        final String build = loadAsString(buildFile);
        assertTrue(build.contains("# --- depgen-generated targets start ---"));
        assertTrue(build.contains("_CONFIG_SHA256 = \""));
        assertTrue(build.contains("exports_files([\"dependencies.yml\"])"));
        assertTrue(build.contains("name = \"update_depgen_generated_outputs\""));
        assertTrue(build.contains("# Dependency Graph Generated from the input data"));
        assertTrue(build.contains("# --- depgen-generated targets end ---"));
    }

    @Test
    public void generate_repositoryRulesInModule_targetsInBuild_noExtensionGenerated_warnsIfStaleExtensionPresent()
            throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        FileUtil.write("MODULE.bazel", """
            module(name = "test")

            # --- depgen-generated repository rules start ---

            # --- depgen-generated repository rules end ---
            """);
        final Path buildFile = FileUtil.getCurrentDirectory().resolve("thirdparty/BUILD.bazel");
        FileUtil.write(buildFile, """
            package(default_visibility = ["//visibility:public"])

            # --- depgen-generated targets start ---

            # --- depgen-generated targets end ---
            """);
        writeConfigFile(dir, """
            options:
              repositoryRuleGenerationStrategy: module
              targetGenerationStrategy: build
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path extensionFile = FileUtil.getCurrentDirectory().resolve("thirdparty/dependencies.bzl");
        FileUtil.write(extensionFile, "stale extension");

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final var handler = new TestHandler();
        final var command = new GenerateCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment(handler)));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertOutputContains(
                handler.toString(),
                "Generated extension file '" + extensionFile + "' is no longer used and can be removed manually.");
        assertEquals(loadAsString(extensionFile), "stale extension");
        assertTrue(loadAsString(FileUtil.getCurrentDirectory().resolve("MODULE.bazel"))
                .contains("_http_file("));
        assertTrue(loadAsString(buildFile).contains("update_depgen_generated_outputs"));
    }
}
