package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class MainTest
  extends AbstractTest
{
  @Test
  public void printUsage()
  {
    final TestHandler handler = new TestHandler();
    Main.printUsage( newEnvironment( createLogger( handler ) ) );
    assertEquals( handler.getRecords().stream().map( LogRecord::getMessage ).collect( Collectors.joining( "\n" ) ),
                  "java org.realityforge.bazel.depgen.Main [options] [command]\n" +
                  "\tPossible Commands:\n" +
                  "\t\tgenerate: Generate the bazel extension from the dependency configuration.\n" +
                  "\t\tprint-graph: Compute and print the dependency graph for the dependency configuration.\n" +
                  "\tOptions:\n" +
                  "\t-h, --help\n" +
                  "\t\tprint this message and exit\n" +
                  "\t-q, --quiet\n" +
                  "\t\tDo not output unless an error occurs.\n" +
                  "\t-v, --verbose\n" +
                  "\t\tVerbose output of differences.\n" +
                  "\t-d, --dependencies-file <argument>\n" +
                  "\t\tThe path to the yaml file containing the dependencies. Defau\n" +
                  "\t\tlts to 'dependencies.yml' in the workspace directory.\n" +
                  "\t-s, --settings-file <argument>\n" +
                  "\t\tThe path to the settings.xml used by Maven to extract reposi\n" +
                  "\t\ttory credentials. Defaults to '~/.m2/settings.xml'.\n" +
                  "\t-r, --cache-dir <argument>\n" +
                  "\t\tThe path to the directory in which to cache downloads from r\n" +
                  "\t\temote repositories. Defaults to '.repository' in the workspa\n" +
                  "\t\tce directory." );
  }

  @Test
  public void processOptions_noCommand()
  {
    assertEquals( processOptions(), "Error: No command specified. Please specify a command." );
  }

  @Test
  public void processOptions_defaultDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = processOptions( "generate" );
      assertOutputContains( output, "Error: Default dependencies file does not exist: " );
    } );
  }

  @Test
  public void processOptions_defaultCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );
      FileUtil.write( ".repository", "NotADir" );

      final String output = processOptions( "generate" );
      assertOutputContains( output, "Error: Default cache directory exists but is not a directory: " );
    } );
  }

  @Test
  public void processOptions_unexpectedArgument()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( "generate", "Bleep" );
      assertOutputContains( output, "Error: Unknown command: Bleep" );
    } );
  }

  @Test
  public void processOptions_specifiedDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = processOptions( "--dependencies-file", "deps.txt", "generate" );
      assertOutputContains( output, "Error: Specified dependencies file does not exist. Specified value: deps.txt" );
    } );
  }

  @Test
  public void processOptions_specifiedCacheDirectoryNotDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );
      FileUtil.write( "StoreMeHere", "NotADir" );

      final String output = processOptions( "--cache-dir", "StoreMeHere", "generate" );
      assertOutputContains( output,
                            "Error: Specified cache directory exists but is not a directory. Specified value: StoreMeHere" );
    } );
  }

  @Test
  public void processOptions_missingSpecifiedSettings()
    throws Exception
  {
    // Need to declare repositories otherwise we never even try to load settings
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      final String output = processOptions( "--settings-file", "some_settings.xml", "generate" );
      assertOutputContains( output,
                            "Error: Specified settings file does not exist. Specified value: some_settings.xml" );
    } );
  }

  @Test
  public void processOptions_help()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( "--help" );
      assertOutputContains( output, "-h, --help\n" );
      assertOutputContains( output, "-q, --quiet\n" );
      assertOutputContains( output, "-v, --verbose\n" );
      assertOutputContains( output, "-d, --dependencies-file <argument>\n" );
      assertOutputContains( output, "-s, --settings-file <argument>\n" );
      assertOutputContains( output, "-r, --cache-dir <argument>\n" );
    } );
  }

  @Test
  public void processOptions_outputInVerbose()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( "--verbose", "generate" );
      assertOutputContains( output, "Bazel DepGen Starting...\n" );
      assertOutputContains( output, "\n  Dependencies file: " );
      assertOutputContains( output, "\n  Settings file: " );
      assertOutputContains( output, "\n  Local Cache directory: " );
    } );
  }

  @Nonnull
  private String processOptions( @Nonnull final String... args )
  {
    final TestHandler handler = new TestHandler();
    Main.processOptions( newEnvironment( createLogger( handler ) ), args );
    return handler.getRecords().stream().map( LogRecord::getMessage ).collect( Collectors.joining( "\n" ) );
  }
}