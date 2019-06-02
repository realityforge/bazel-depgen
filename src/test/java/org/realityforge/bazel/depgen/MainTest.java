package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.logging.Logger;
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
    assertEquals( handler.toString(),
                  "java org.realityforge.bazel.depgen.Main [options] [command]\n" +
                  "\tPossible Commands:\n" +
                  "\t\tgenerate: Generate the bazel extension from the dependency configuration.\n" +
                  "\t\tprint-graph: Compute and print the dependency graph for the dependency configuration.\n" +
                  "\t\thash: Generate a hash of the content of the dependency configuration.\n" +
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
                  "\t-r, --cache-directory <argument>\n" +
                  "\t\tThe path to the directory in which to cache downloads from r\n" +
                  "\t\temote repositories. Defaults to \"$(bazel info output_base)/.\n" +
                  "\t\tdepgen-cache\".\n" +
                  "\t--reset-cached-metadata\n" +
                  "\t\tRecalculate metadata about an artifact." );
  }

  @Test
  public void processOptions_noCommand()
  {
    assertEquals( processOptions( false ), "Error: No command specified. Please specify a command." );
  }

  @Test
  public void processOptions_defaultDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = processOptions( false, "generate" );
      assertOutputContains( output, "Error: Default dependencies file does not exist: " );
    } );
  }

  @Test
  public void processOptions_unexpectedArgument()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( false, "generate", "Bleep" );
      assertOutputContains( output, "Error: Unknown command: Bleep" );
    } );
  }

  @Test
  public void processOptions_specifiedDependenciesMissing()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();

      final String output = processOptions( false, "--dependencies-file", "deps.txt", "generate" );
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

      final String output = processOptions( false, "--cache-directory", "StoreMeHere", "generate" );
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
      writeDependencies( "repositories:\n" +
                         "  - name: central" +
                         "    url: http://repo1.maven.org/maven2\n" );

      final String output = processOptions( false, "--settings-file", "some_settings.xml", "generate" );
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

      final String output = processOptions( false, "--help" );
      assertOutputContains( output, "-h, --help\n" );
      assertOutputContains( output, "-q, --quiet\n" );
      assertOutputContains( output, "-v, --verbose\n" );
      assertOutputContains( output, "-d, --dependencies-file <argument>\n" );
      assertOutputContains( output, "-s, --settings-file <argument>\n" );
      assertOutputContains( output, "-r, --cache-directory <argument>\n" );
      assertOutputContains( output, "--reset-cached-metadata\n" );
    } );
  }

  @Test
  public void processOptions_error()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = processOptions( false, "--some-command-no-exist" );
      assertEquals( output, "Error: Unknown option --some-command-no-exist" );
    } );
  }

  @Test
  public void printBanner_cacheExplicitlySpecified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Logger logger = createLogger( handler );
      final Environment environment = newEnvironment( logger );
      final Path dependenciesFile = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( dependenciesFile );
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      environment.setSettingsFile( settingsFile );
      final Path cacheDir = FileUtil.getCurrentDirectory().resolve( ".depgen-cache" );
      environment.setCacheDir( cacheDir );
      Main.printBanner( environment );
      final String output = handler.toString();
      assertOutputContains( output, "Bazel DepGen Starting...\n" );
      assertOutputContains( output, "\n  Dependencies file: " + dependenciesFile );
      assertOutputContains( output, "\n  Settings file: " + settingsFile );
      assertOutputContains( output, "\n  Cache directory: " + cacheDir );
    } );
  }

  @Test
  public void printBanner()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final TestHandler handler = new TestHandler();
      final Logger logger = createLogger( handler );
      final Environment environment = newEnvironment( logger );
      final Path dependenciesFile = FileUtil.getCurrentDirectory().resolve( "dependencies.yml" );
      environment.setDependenciesFile( dependenciesFile );
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      environment.setSettingsFile( settingsFile );
      Main.printBanner( environment );
      final String output = handler.toString();
      assertOutputContains( output, "Bazel DepGen Starting...\n" );
      assertOutputContains( output, "\n  Dependencies file: " + dependenciesFile );
      assertOutputContains( output, "\n  Settings file: " + settingsFile );
      assertOutputDoesNotContain( output, "\n  Cache directory: " );
    } );
  }

  @Test
  public void getCacheDirectory_default()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Path cacheDirectory = Main.getCacheDirectory( newEnvironment(), loadApplicationModel() );
      assertNotNull( cacheDirectory );
    } );
  }

  @Test
  public void getCacheDirectory_explicitlySpecified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final Environment environment = newEnvironment();
      final Path cacheDir = FileUtil.createLocalTempDir();
      environment.setCacheDir( cacheDir );
      final Path cacheDirectory = Main.getCacheDirectory( environment, loadApplicationModel() );
      assertEquals( cacheDirectory, cacheDir );
    } );
  }

  @Test
  public void getCacheDirectory_outsideWorkspace()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      final TerminalStateException exception =
        expectThrows( TerminalStateException.class,
                      () -> Main.getCacheDirectory( newEnvironment(), loadApplicationModel() ) );
      assertEquals( exception.getMessage(),
                    "Error: Cache directory not specified and unable to derive default directory (Is the bazel command on the path?). Explicitly pass the cache directory as an option." );
      assertEquals( exception.getExitCode(), ExitCodes.ERROR_INVALID_DEFAULT_CACHE_DIR_CODE );
    } );
  }

  @SuppressWarnings( "SameParameterValue" )
  @Nonnull
  private String processOptions( final boolean expectedResult, @Nonnull final String... args )
  {
    final TestHandler handler = new TestHandler();
    final Environment environment = newEnvironment( createLogger( handler ) );
    final boolean result = Main.processOptions( environment, args );
    assertEquals( expectedResult, result, "Return value for Main.processOptions" );
    return handler.toString();
  }
}
