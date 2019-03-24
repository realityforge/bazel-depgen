package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CommandLineArgsParsingTest
  extends AbstractDepGenTest
{
  @Test
  public void defaultWorkspaceMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );

      final String output = runCommand( 2 );
      assertOutputContains( output,
                            "Error: Default workspace directory does not contain a WORKSPACE file. Directory: " );
    } );
  }

  @Test
  public void defaultDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );

      final String output = runCommand( 2 );
      assertOutputContains( output, "Error: Default dependencies file does not exist: " );
    } );
  }

  @Test
  public void defaultCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );
      writeDependencies( "" );
      FileUtil2.write( ".repository", "NotADir" );

      final String output = runCommand( 2 );
      assertOutputContains( output, "Error: Default cache directory exists but is not a directory: " );
    } );
  }

  @Test
  public void defaultExtensionFileExistsAsDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );
      writeDependencies( "" );
      assertTrue( FileUtil.getCurrentDirectory().resolve( "3rdparty/workspace.bzl" ).toFile().mkdirs() );

      final String output = runCommand( 2 );
      assertOutputContains( output, "Error: Default bazel extension file exists but is a directory: " );
    } );
  }

  @Test
  public void unexpectedArgument()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );
      writeDependencies( "" );

      final String output = runCommand( 2, "Bleep" );
      assertOutputContains( output, "Error: Unexpected argument: Bleep" );
    } );
  }

  @Test
  public void specifiedWorkspaceDoesNotExist()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final String output = runCommand( 2, "--workspace-dir", "subdir" );
      assertOutputContains( output,
                            "Error: Specified workspace directory does not exist. Specified value: subdir" );
    } );
  }

  @Test
  public void specifiedWorkspaceIsAFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      FileUtil2.write( "subdir", "" );

      final String output = runCommand( 2, "--workspace-dir", "subdir" );
      assertOutputContains( output,
                            "Error: Specified workspace directory is not a directory. Specified value: subdir" );
    } );
  }

  @Test
  public void specifiedWorkspaceMissingWORKSPACE()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path subdir = FileUtil.getCurrentDirectory().resolve( "subdir" );
      assertTrue( subdir.toFile().mkdir() );
      FileUtil2.write( "subdir/dependencies.yml", "" );

      final String output = runCommand( 2, "--workspace-dir", "subdir" );
      assertOutputContains( output,
                            "Error: Specified workspace directory does not contain a WORKSPACE file. Specified value: subdir" );
    } );
  }

  @Test
  public void specifiedDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );

      final String output = runCommand( 2, "--dependencies-file", "deps.txt" );
      assertOutputContains( output, "Error: Specified dependencies file does not exist. Specified value: deps.txt" );
    } );
  }

  @Test
  public void specifiedCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );
      writeDependencies( "" );
      FileUtil2.write( "StoreMeHere", "NotADir" );

      final String output = runCommand( 2, "--cache-dir", "StoreMeHere" );
      assertOutputContains( output,
                            "Error: Specified cache directory exists but is not a directory. Specified value: StoreMeHere" );
    } );
  }

  @Test
  public void specifiedExtensionFileExistsAsDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace( "" );
      writeDependencies( "" );
      assertTrue( FileUtil.getCurrentDirectory().resolve( "3rdparty/other_workspace.bzl" ).toFile().mkdirs() );

      final String output = runCommand( 2, "--extension-file", "3rdparty/other_workspace.bzl" );
      assertOutputContains( output,
                            "Error: Specified bazel extension file exists but is a directory. Specified value: 3rdparty/other_workspace.bzl" );
    } );
  }
}
