package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CommandLineArgsParsingTest
  extends AbstractTest
{
  @Test
  public void defaultDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = runCommand( 2 );
      assertOutputContains( output, "Error: Default dependencies file does not exist: " );
    } );
  }

  @Test
  public void defaultCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );
      FileUtil.write( ".repository", "NotADir" );

      final String output = runCommand( 2 );
      assertOutputContains( output, "Error: Default cache directory exists but is not a directory: " );
    } );
  }

  @Test
  public void unexpectedArgument()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = runCommand( 2, "Bleep" );
      assertOutputContains( output, "Error: Unexpected argument: Bleep" );
    } );
  }

  @Test
  public void specifiedDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = runCommand( 2, "--dependencies-file", "deps.txt" );
      assertOutputContains( output, "Error: Specified dependencies file does not exist. Specified value: deps.txt" );
    } );
  }

  @Test
  public void specifiedCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );
      FileUtil.write( "StoreMeHere", "NotADir" );

      final String output = runCommand( 2, "--cache-dir", "StoreMeHere" );
      assertOutputContains( output,
                            "Error: Specified cache directory exists but is not a directory. Specified value: StoreMeHere" );
    } );
  }

  @Test
  public void help()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = runCommand( 2, "--help" );
      assertOutputContains( output, "-h, --help\n" );
      assertOutputContains( output, "-q, --quiet\n" );
      assertOutputContains( output, "-v, --verbose\n" );
      assertOutputContains( output, "-d, --dependencies-file <argument>\n" );
      assertOutputContains( output, "-s, --settings-file <argument>\n" );
      assertOutputContains( output, "-r, --cache-dir <argument>\n" );
    } );
  }

  @Test
  public void outputInVerbose()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = runCommand( "--verbose" );
      assertOutputContains( output, "Bazel DepGen Starting...\n" );
      assertOutputContains( output, "\n  Dependencies file: " );
      assertOutputContains( output, "\n  Settings file: " );
      assertOutputContains( output, "\n  Local Cache directory: " );
    } );
  }

  @Test
  public void noOutputInNormalCase()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = runCommand();
      assertEquals( output, "" );
    } );
  }

  @Test
  public void missingDefaultSettingsIsFine()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      runCommand();
    } );
  }

  @Test
  public void invalidDefaultSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );
      assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2" ).toFile().mkdir() );
      FileUtil.write( ".m2/settings.xml", "JHSGDJHDS()*&(&Y*&" );

      final String output = runCommand( 4 );
      assertOutputContains( output, "Error: Problem loading settings from " );
    } );
  }

  @Test
  public void defaultSettingsIsDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );
      assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2/settings.xml" ).toFile().mkdirs() );

      final String output = runCommand( 4 );
      assertOutputContains( output, "Error: Problem loading settings from " );
    } );
  }

  @Test
  public void missingSpecifiedSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      final String output = runCommand( 2, "--settings-file", "some_settings.xml" );
      assertOutputContains( output,
                            "Error: Specified settings file does not exist. Specified value: some_settings.xml\n" );
    } );
  }

  @Test
  public void validDefaultSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2" ).toFile().mkdir() );
      FileUtil.write( ".m2/settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      final String output = runCommand();
      assertOutputContains( output, "" );
    } );
  }

  @Test
  public void validSpecifiedSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      FileUtil.write( "some_settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      final String output = runCommand( "--settings-file", "some_settings.xml" );
      assertOutputContains( output, "" );
    } );
  }
}
