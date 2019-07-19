package org.realityforge.bazel.depgen;

import gir.io.IoUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class InitCommandTest
  extends AbstractTest
{
  @Test
  public void init()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDirectory = environment.getConfigFile().getParent();
    deployDepGenArtifactToCacheDir( environment.getCacheDir() );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Created configuration directory " + configDirectory );
    assertOutputContains( output, "Created configuration file " + environment.getConfigFile() );
    final Path workspaceFile = environment.currentDirectory().resolve( "WORKSPACE" );
    assertOutputContains( output, "Created WORKSPACE file " + workspaceFile );

    assertEquals( loadAsString( environment.getConfigFile() ), loadTemplate() );
    assertTrue( Files.exists( configDirectory.resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) ) );
    assertEquals( loadAsString( workspaceFile ),
                  "workspace(name = \"" + workspaceFile.getParent().getFileName() + "\")\n" +
                  "\n" +
                  "load(\"//thirdparty:dependencies.yml\", \"generate_workspace_rules\")\n" +
                  "\n" +
                  "generate_workspace_rules()\n" );
  }

  @Test
  public void init_differentConfigLocation()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDirectory = environment.currentDirectory().resolve( "subdir" ).resolve( "thirdparty" );
    environment.setConfigFile( configDirectory.resolve( "somefile.yaml" ) );
    deployDepGenArtifactToCacheDir( environment.getCacheDir() );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Created configuration directory " + configDirectory );
    assertOutputContains( output, "Created configuration file " + environment.getConfigFile() );
    final Path workspaceFile = environment.currentDirectory().resolve( "WORKSPACE" );
    assertOutputContains( output, "Created WORKSPACE file " + workspaceFile );

    assertEquals( loadAsString( environment.getConfigFile() ), loadTemplate() );
    assertTrue( Files.exists( configDirectory.resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) ) );
    assertEquals( loadAsString( workspaceFile ),
                  "workspace(name = \"" + workspaceFile.getParent().getFileName() + "\")\n" +
                  "\n" +
                  "load(\"//subdir/thirdparty:somefile.yaml\", \"generate_workspace_rules\")\n" +
                  "\n" +
                  "generate_workspace_rules()\n" );
  }

  @Test
  public void init_unknownArg()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );

    final boolean parsed = command.processOptions( environment, "blah" );
    assertFalse( parsed );
    assertEquals( handler.toString(), "Error: Invalid argument: blah" );
  }

  @Test
  public void init_no_generate()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDirectory = environment.getConfigFile().getParent();
    deployDepGenArtifactToCacheDir( environment.getCacheDir() );

    assertTrue( command.processOptions( environment, "--no-generate" ) );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Created configuration directory " + configDirectory );
    assertOutputContains( output, "Created configuration file " + environment.getConfigFile() );
    final Path workspaceFile = environment.currentDirectory().resolve( "WORKSPACE" );
    assertOutputContains( output, "Created WORKSPACE file " + workspaceFile );

    assertEquals( loadAsString( environment.getConfigFile() ), loadTemplate() );
    assertFalse( Files.exists( configDirectory.resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) ) );
    assertEquals( loadAsString( workspaceFile ),
                  "workspace(name = \"" + workspaceFile.getParent().getFileName() + "\")\n" +
                  "\n" +
                  "load(\"//thirdparty:dependencies.yml\", \"generate_workspace_rules\")\n" +
                  "\n" +
                  "generate_workspace_rules()\n" );
  }

  @Test
  public void init_no_create_workspace()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDirectory = environment.getConfigFile().getParent();
    deployDepGenArtifactToCacheDir( environment.getCacheDir() );

    assertTrue( command.processOptions( environment, "--no-generate", "--no-create-workspace" ) );
    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Created configuration directory " + configDirectory );
    assertOutputContains( output, "Created configuration file " + environment.getConfigFile() );
    final Path workspaceFile = environment.currentDirectory().resolve( "WORKSPACE" );
    assertOutputDoesNotContain( output, "Created WORKSPACE file " + workspaceFile );

    assertTrue( Files.exists( environment.getConfigFile() ) );
    assertFalse( Files.exists( configDirectory.resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) ) );
    assertFalse( Files.exists( workspaceFile ) );
  }

  @Test
  public void init_preExistingWorkspace()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDirectory = environment.getConfigFile().getParent();
    final Path workspaceFile = environment.currentDirectory().resolve( "WORKSPACE" );

    Files.write( workspaceFile, "# This is a comment".getBytes( StandardCharsets.UTF_8 ) );

    deployDepGenArtifactToCacheDir( environment.getCacheDir() );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Created configuration directory " + configDirectory );
    assertOutputContains( output, "Created configuration file " + environment.getConfigFile() );
    assertOutputDoesNotContain( output, "Created WORKSPACE file " + workspaceFile );

    assertTrue( Files.exists( environment.getConfigFile() ) );
    assertTrue( Files.exists( configDirectory.resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) ) );
    assertEquals( loadAsString( workspaceFile ), "# This is a comment" );
  }

  @Test
  public void init_preExistingConfigDirectory()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDirectory = environment.getConfigFile().getParent();
    deployDepGenArtifactToCacheDir( environment.getCacheDir() );

    Files.createDirectories( configDirectory );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.SUCCESS_EXIT_CODE );
    final String output = handler.toString();
    assertOutputDoesNotContain( output, "Created configuration directory " + configDirectory );
    assertOutputContains( output, "Created configuration file " + environment.getConfigFile() );

    assertAllFilesExist( environment );
  }

  @Test
  public void init_configFileExists()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );

    final Path configFile = environment.getConfigFile();
    Files.createDirectories( configFile.getParent() );
    Files.write( configFile, new byte[]{ 'X' } );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.ERROR_DEPENDENCY_CONFIG_PRESENT_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Error: Configuration file already exists. File: " + configFile );

    assertEquals( loadAsString( configFile ), "X" );
    assertExtensionNoExist( environment );
    assertWorkspaceNoExist( environment );
  }

  @Test
  public void init_configDirReadOnly()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );

    final Path configFile = environment.getConfigFile();
    final Path configDirectory = configFile.getParent();
    Files.createDirectories( configDirectory );
    final HashSet<PosixFilePermission> perms = new HashSet<>();
    perms.add( PosixFilePermission.OWNER_READ );
    Files.setPosixFilePermissions( configDirectory, perms );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.ERROR_INIT_WRITE_FAILED_CODE );
    final String output = handler.toString();
    assertOutputContains( output, "Failed to create configuration file. File: " + configFile );

    assertNoFilesExist( environment );
  }

  @Test
  public void init_configDirParentReadOnly()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );

    final Path configFile = environment.getConfigFile();
    final HashSet<PosixFilePermission> perms = new HashSet<>();
    perms.add( PosixFilePermission.OWNER_READ );
    Files.setPosixFilePermissions( environment.currentDirectory(), perms );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.ERROR_INIT_WRITE_FAILED_CODE );
    final String output = handler.toString();
    assertOutputContains( output,
                          "Error: Failed to create directory to contain configuration file. Directory: " +
                          configFile.getParent() );

    assertNoFilesExist( environment );

    perms.add( PosixFilePermission.OWNER_WRITE );
    perms.add( PosixFilePermission.OWNER_EXECUTE );
    Files.setPosixFilePermissions( environment.currentDirectory(), perms );
  }

  @Test
  public void init_WORKSPACE_writeFailed()
    throws Exception
  {
    final TestHandler handler = new TestHandler();
    final Command command = new InitCommand();
    final Environment environment = newEnvironment( handler );
    final Path configDir = environment.currentDirectory().resolve( "subdir" ).resolve( "thirdparty" );
    environment.setConfigFile( configDir.resolve( ApplicationConfig.FILENAME ) );

    Files.createDirectories( configDir );

    final HashSet<PosixFilePermission> perms = new HashSet<>();
    perms.add( PosixFilePermission.OWNER_READ );
    perms.add( PosixFilePermission.OWNER_WRITE );
    perms.add( PosixFilePermission.OWNER_EXECUTE );
    Files.setPosixFilePermissions( configDir, perms );

    final HashSet<PosixFilePermission> perms2 = new HashSet<>();
    perms2.add( PosixFilePermission.OWNER_READ );
    perms2.add( PosixFilePermission.OWNER_EXECUTE );
    Files.setPosixFilePermissions( environment.currentDirectory(), perms2 );

    final int exitCode = command.run( new CommandContextImpl( environment ) );
    assertEquals( exitCode, ExitCodes.ERROR_INIT_WRITE_FAILED_CODE );
    final String output = handler.toString();
    assertOutputContains( output,
                          "Error: Failed to create WORKSPACE file. File: " +
                          environment.currentDirectory().resolve( "WORKSPACE" ) );

    assertConfigFileExist( environment );
    assertExtensionNoExist( environment );
    assertWorkspaceNoExist( environment );

    perms2.add( PosixFilePermission.OWNER_WRITE );
    perms2.add( PosixFilePermission.OWNER_EXECUTE );
    Files.setPosixFilePermissions( environment.currentDirectory(), perms2 );
  }

  private void assertAllFilesExist( @Nonnull final Environment environment )
  {
    assertConfigFileExist( environment );
    assertExtensionExist( environment );
    assertWorkspaceExist( environment );
  }

  private void assertWorkspaceExist( @Nonnull final Environment environment )
  {
    assertTrue( Files.exists( environment.currentDirectory().resolve( "WORKSPACE" ) ) );
  }

  private void assertExtensionExist( @Nonnull final Environment environment )
  {
    assertTrue( Files.exists( getExtensionFile( environment ) ) );
  }

  private void assertConfigFileExist( @Nonnull final Environment environment )
  {
    assertTrue( Files.exists( environment.getConfigFile() ) );
  }

  private void assertNoFilesExist( @Nonnull final Environment environment )
  {
    assertConfigFileNoExist( environment );
    assertExtensionNoExist( environment );
    assertWorkspaceNoExist( environment );
  }

  private void assertWorkspaceNoExist( @Nonnull final Environment environment )
  {
    assertFalse( Files.exists( environment.currentDirectory().resolve( "WORKSPACE" ) ) );
  }

  private void assertExtensionNoExist( @Nonnull final Environment environment )
  {
    assertFalse( Files.exists( getExtensionFile( environment ) ) );
  }

  @Nonnull
  private Path getExtensionFile( @Nonnull final Environment environment )
  {
    return environment.getConfigFile()
      .getParent()
      .resolve( OptionsConfig.DEFAULT_EXTENSION_FILE );
  }

  private void assertConfigFileNoExist( @Nonnull final Environment environment )
  {
    assertFalse( Files.exists( environment.getConfigFile() ) );
  }

  @Nonnull
  private String loadTemplate()
    throws IOException
  {
    final InputStream inputStream = getClass().getResourceAsStream( "templates/dependencies.yml" );
    assertNotNull( inputStream );
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IoUtil.copy( inputStream, baos );
    return new String( baos.toByteArray(), StandardCharsets.ISO_8859_1 );
  }
}
