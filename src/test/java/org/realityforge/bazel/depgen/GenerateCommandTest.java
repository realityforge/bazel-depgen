package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.io.IOException;
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
    assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/dependencies.bzl" ),
                                model.getConfigSha256(),
                                url ),
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
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void generate_buildFilesExist()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

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

    assertEquals( loadAsString( FileUtil.getCurrentDirectory().resolve( "thirdparty/dependencies.bzl" ),
                                model.getConfigSha256(),
                                dir.toUri().toString() ),
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
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
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

    assertThrows( IOException.class, () -> command.run( new CommandContextImpl( newEnvironment() ) ) );
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
