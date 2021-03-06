package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class GenerateCommandTest
  extends AbstractTest
{
  @Test
  public void generate()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    final String url = dir.toUri().toString();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
    final ApplicationModel model = loadApplicationModel();

    final TestHandler handler = new TestHandler();
    final GenerateCommand command = new GenerateCommand();
    final int exitCode = command.run( new CommandContextImpl( newEnvironment() ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    assertEquals( handler.toString(), "" );

    assertFalse( Files.exists( FileUtil.getCurrentDirectory().resolve( "BUILD.bazel" ) ) );
    assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/BUILD.bazel" ) ),
                  "# File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "# Contents can be edited and will not be overridden.\n" +
                  "package(default_visibility = [\"//visibility:public\"])\n" +
                  "\n" +
                  "load(\"//thirdparty:dependencies.bzl\", \"generate_targets\")\n" +
                  "\n" +
                  "generate_targets()\n" +
                  "\n" +
                  "exports_files([\"dependencies.yml\"])\n" );
    //@formatter:off
    assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/dependencies.bzl" ) ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", \"java_import\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"" + model.getConfigSha256() + "\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"" + url + "com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"" + url + "com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"" + url + "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    native.genrule(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        srcs = [\n" +
                  "            \":org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            \"//thirdparty:dependencies.yml\",\n" +
                  "            \"@bazel_tools//tools/jdk:current_java_runtime\",\n" +
                  "        ],\n" +
                  "        toolchains = [\"@bazel_tools//tools/jdk:current_java_runtime\"],\n" +
                  "        outs = [\"command-output.txt\"],\n" +
                  "        cmd = \"$(JAVA) -jar $(location :org_realityforge_bazel_depgen__bazel_depgen) --config-file $(location //thirdparty:dependencies.yml) --quiet hash --verify-sha256 %s > \\\"$@\\\"\" % (_CONFIG_SHA256),\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.genrule(\n" +
                  "        name = \"regenerate_depgen_extension_script\",\n" +
                  "        srcs = [\n" +
                  "            \":org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            \"//thirdparty:dependencies.yml\",\n" +
                  "            \"@bazel_tools//tools/jdk:current_java_runtime\",\n" +
                  "        ],\n" +
                  "        toolchains = [\"@bazel_tools//tools/jdk:current_java_runtime\"],\n" +
                  "        outs = [\"regenerate_depgen_extension_script.sh\"],\n" +
                  "        cmd = \"echo \\\"$(JAVA) -jar $(location :org_realityforge_bazel_depgen__bazel_depgen) --directory \\\\$$BUILD_WORKSPACE_DIRECTORY --config-file $(location //thirdparty:dependencies.yml) \\$$@ generate \\\" > \\\"$@\\\"\",\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.sh_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        srcs = [\"regenerate_depgen_extension_script\"],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\n" +
                  "            \":org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            \"//thirdparty:dependencies.yml\",\n" +
                  "            \"@bazel_tools//tools/jdk:current_java_runtime\",\n" +
                  "        ],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.alias(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        actual = \":com_example__myapp__1_0\",\n" +
                  "    )\n" +
                  "    java_import(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "        data = [\":verify_config_sha256\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.alias(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        actual = \":org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "    )\n" +
                  "    java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" );
    //@formatter:on
  }

  @Test
  public void generate_buildFilesExist()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    final String url = dir.toUri().toString();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    final Path configPackage = FileUtil.getCurrentDirectory().resolve( "BUILD.bazel" );
    final Path extensionPackage = FileUtil.getCurrentDirectory().resolve( "thirdparty/BUILD.bazel" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
    FileUtil.write( configPackage, "" );
    FileUtil.write( extensionPackage, "" );

    final ApplicationModel model = loadApplicationModel();

    final TestHandler handler = new TestHandler();
    final GenerateCommand command = new GenerateCommand();
    final int exitCode = command.run( new CommandContextImpl( newEnvironment() ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    assertEquals( handler.toString(), "" );

    // File contents not changed
    assertEquals( loadAsString( configPackage ), "" );
    // File contents not changed
    assertEquals( loadAsString( extensionPackage ), "" );

    assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/dependencies.bzl" ) ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", \"java_import\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"" +
                  model.getConfigSha256() +
                  "\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"" +
                  url +
                  "com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"" +
                  url +
                  "com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"" +
                  url +
                  "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    native.genrule(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        srcs = [\n" +
                  "            \":org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            \"//thirdparty:dependencies.yml\",\n" +
                  "            \"@bazel_tools//tools/jdk:current_java_runtime\",\n" +
                  "        ],\n" +
                  "        toolchains = [\"@bazel_tools//tools/jdk:current_java_runtime\"],\n" +
                  "        outs = [\"command-output.txt\"],\n" +
                  "        cmd = \"$(JAVA) -jar $(location :org_realityforge_bazel_depgen__bazel_depgen) --config-file $(location //thirdparty:dependencies.yml) --quiet hash --verify-sha256 %s > \\\"$@\\\"\" % (_CONFIG_SHA256),\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.genrule(\n" +
                  "        name = \"regenerate_depgen_extension_script\",\n" +
                  "        srcs = [\n" +
                  "            \":org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            \"//thirdparty:dependencies.yml\",\n" +
                  "            \"@bazel_tools//tools/jdk:current_java_runtime\",\n" +
                  "        ],\n" +
                  "        toolchains = [\"@bazel_tools//tools/jdk:current_java_runtime\"],\n" +
                  "        outs = [\"regenerate_depgen_extension_script.sh\"],\n" +
                  "        cmd = \"echo \\\"$(JAVA) -jar $(location :org_realityforge_bazel_depgen__bazel_depgen) --directory \\\\$$BUILD_WORKSPACE_DIRECTORY --config-file $(location //thirdparty:dependencies.yml) \\$$@ generate \\\" > \\\"$@\\\"\",\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.sh_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        srcs = [\"regenerate_depgen_extension_script\"],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\n" +
                  "            \":org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            \"//thirdparty:dependencies.yml\",\n" +
                  "            \"@bazel_tools//tools/jdk:current_java_runtime\",\n" +
                  "        ],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.alias(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        actual = \":com_example__myapp__1_0\",\n" +
                  "    )\n" +
                  "    java_import(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "        data = [\":verify_config_sha256\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    native.alias(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        actual = \":org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "    )\n" +
                  "    java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" );
  }

  @Test
  public void generate_directoryIsAFile()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "options:\n" +
                     "  extensionFile: somedir/dependencies.bzl\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    FileUtil.write( FileUtil.getCurrentDirectory().resolve( "thirdparty" ).resolve( "somedir" ), "" );

    final TestHandler handler = new TestHandler();
    final GenerateCommand command = new GenerateCommand();

    assertThrows( FileNotFoundException.class, () -> command.run( new CommandContextImpl( newEnvironment() ) ) );
    assertEquals( handler.toString(), "" );
  }

  @Test
  public void generate_canNotCreateThirdpartyDirectory()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeWorkspace();
    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord applicationRecord = loadApplicationRecord();

    final TestHandler handler = new TestHandler();
    final GenerateCommand command = new GenerateCommand();

    final Environment environment = newEnvironment();

    final HashSet<PosixFilePermission> perms = new HashSet<>();
    perms.add( PosixFilePermission.OWNER_READ );
    Files.setPosixFilePermissions( FileUtil.getCurrentDirectory(), perms );

    try
    {
      final DepgenException exception =
        expectThrows( DepgenException.class, () -> command.run( new Command.Context()
        {
          @Nonnull
          @Override
          public Environment environment()
          {
            return environment;
          }

          @Nonnull
          @Override
          public ApplicationModel loadModel()
          {
            return applicationRecord.getSource();
          }

          @Nonnull
          @Override
          public ApplicationRecord loadRecord()
          {
            return applicationRecord;
          }
        } ) );
      assertEquals( exception.getMessage(),
                    "Failed to create directory " + FileUtil.getCurrentDirectory().resolve( "thirdparty" ) );
    }
    finally
    {
      perms.add( PosixFilePermission.OWNER_WRITE );
      perms.add( PosixFilePermission.OWNER_EXECUTE );
      Files.setPosixFilePermissions( FileUtil.getCurrentDirectory(), perms );
    }
    assertEquals( handler.toString(), "" );
  }
}
