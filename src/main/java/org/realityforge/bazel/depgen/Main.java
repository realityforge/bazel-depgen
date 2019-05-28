package org.realityforge.bazel.depgen;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.graph.DependencyCycle;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.InvalidModelException;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.util.StarlarkOutput;
import org.realityforge.bazel.depgen.util.YamlUtil;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;

/**
 * The entry point in which to run the tool.
 */
public class Main
{
  private static final int HELP_OPT = 'h';
  private static final int QUIET_OPT = 'q';
  private static final int VERBOSE_OPT = 'v';
  private static final int EMIT_DEPENDENCY_GRAPH_OPT = 1;
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]
    {
      new CLOptionDescriptor( "help",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              HELP_OPT,
                              "print this message and exit" ),
      new CLOptionDescriptor( "quiet",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              QUIET_OPT,
                              "Do not output unless an error occurs.",
                              new int[]{ VERBOSE_OPT } ),
      new CLOptionDescriptor( "verbose",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              VERBOSE_OPT,
                              "Verbose output of differences.",
                              new int[]{ QUIET_OPT } ),
      Options.DEPENDENCIES_DESCRIPTOR,
      Options.SETTINGS_DESCRIPTOR,
      Options.CACHE_DESCRIPTOR,
      new CLOptionDescriptor( "emit-dependency-graph",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              EMIT_DEPENDENCY_GRAPH_OPT,
                              "Emit the computed dependency graph after it is calculated." )
    };
  private static final Environment c_environment = new Environment( System.console(), Logger.getGlobal() );
  private static Path c_dependenciesFile;
  private static Path c_settingsFile;
  private static Path c_cacheDir;
  private static boolean c_emitDependencyGraph;

  public static void main( final String[] args )
  {
    setupLogger();
    if ( !processOptions( args ) )
    {
      System.exit( ExitCodes.ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    final Logger logger = c_environment.logger();
    try
    {
      final ApplicationRecord record = loadApplicationRecord();
      final Level dependencyGraphLevel = c_emitDependencyGraph ? Level.WARNING : Level.FINE;
      if ( logger.isLoggable( dependencyGraphLevel ) )
      {
        logger.log( dependencyGraphLevel, "Dependency Graph:" );
        record.getNode()
          .accept( new DependencyGraphEmitter( record.getSource(), line -> logger.log( dependencyGraphLevel, line ) ) );
      }
      generate( record );
    }
    catch ( final InvalidModelException ime )
    {
      final String message = ime.getMessage();
      if ( null != message )
      {
        logger.log( Level.WARNING, message, ime.getCause() );
      }

      logger.log( Level.WARNING,
                  "--- Invalid Config ---\n" +
                  YamlUtil.asYamlString( ime.getModel() ) +
                  "--- End Config ---" );

      System.exit( ExitCodes.ERROR_CONSTRUCTING_MODEL_CODE );
    }
    catch ( final TerminalStateException tse )
    {
      final String message = tse.getMessage();
      if ( null != message )
      {
        logger.log( Level.WARNING, message );
        final Throwable cause = tse.getCause();
        if ( null != cause )
        {
          if ( logger.isLoggable( Level.INFO ) )
          {
            logger.log( Level.INFO, "Cause: " + cause.toString() );
            if ( logger.isLoggable( Level.FINE ) )
            {
              cause.printStackTrace();
            }
          }
        }
      }
      System.exit( tse.getExitCode() );
    }
    catch ( final Throwable t )
    {
      logger.log( Level.WARNING, t.toString(), t );
      System.exit( ExitCodes.ERROR_EXIT_CODE );
    }

    System.exit( ExitCodes.SUCCESS_EXIT_CODE );
  }

  @Nonnull
  private static ApplicationRecord loadApplicationRecord()
    throws DependencyResolutionException
  {
    return loadApplicationRecord( ApplicationModel.parse( loadDependenciesYaml() ) );
  }

  @Nonnull
  private static ApplicationRecord loadApplicationRecord( @Nonnull final ApplicationModel model )
    throws DependencyResolutionException
  {
    final Resolver resolver = ResolverUtil.createResolver( c_environment, c_cacheDir, model, loadSettings() );
    return ApplicationRecord.build( model,
                                    resolveModel( resolver, model ),
                                    resolver.getAuthenticationContexts(),
                                    m -> c_environment.logger().warning( m ) );
  }

  @Nonnull
  private static DependencyNode resolveModel( @Nonnull final Resolver resolver,
                                              @Nonnull final ApplicationModel model )
    throws DependencyResolutionException
  {
    final Logger logger = c_environment.logger();
    final DependencyResult result = resolver.resolveDependencies( model, ( artifactModel, exceptions ) -> {
      // If we get here then the listener has already emitted a warning message so just need to exit
      // We can only get here if either failOnMissingPom or failOnInvalidPom is true and an error occurred
      throw new TerminalStateException( ExitCodes.ERROR_INVALID_POM_CODE );
    } );

    final List<DependencyCycle> cycles = result.getCycles();
    if ( !cycles.isEmpty() )
    {
      logger.warning( cycles.size() + " dependency cycles detected when collecting dependencies:" );
      for ( final DependencyCycle cycle : cycles )
      {
        logger.warning( cycle.toString() );
      }
      throw new TerminalStateException( ExitCodes.ERROR_CYCLES_PRESENT_CODE );
    }
    final List<Exception> exceptions = result.getCollectExceptions();
    if ( !exceptions.isEmpty() )
    {
      logger.warning( exceptions.size() + " errors collecting dependencies:" );
      for ( final Exception exception : exceptions )
      {
        logger.log( Level.WARNING, null, exception );
      }
      throw new TerminalStateException( ExitCodes.ERROR_COLLECTING_DEPENDENCIES_CODE );
    }

    return result.getRoot();
  }

  private static void generate( @Nonnull final ApplicationRecord record )
    throws Exception
  {
    final Path extensionFile = record.getSource().getOptions().getExtensionFile();
    final Path dir = extensionFile.getParent();
    final Path buildfile = dir.resolve( "BUILD.bazel" );

    if ( !dir.toFile().exists() && !dir.toFile().mkdirs() )
    {
      throw new IllegalStateException( "Failed to create directory " + dir.toFile() );
    }

    // The tool will only emit the `BUILD.bazel` file if none exist. If one exists then
    // the tool assumes the user has supplied it or it is an artifact from a previous run.
    if ( !buildfile.toFile().exists() )
    {
      try ( final StarlarkOutput output = new StarlarkOutput( buildfile ) )
      {
        record.writeDefaultBuild( output );
      }
    }

    try ( final StarlarkOutput output = new StarlarkOutput( extensionFile ) )
    {
      record.writeBazelExtension( output );
    }
  }

  @Nonnull
  private static Settings loadSettings()
  {
    try
    {
      return SettingsUtil.loadSettings( c_settingsFile, c_environment.logger() );
    }
    catch ( final SettingsBuildingException e )
    {
      throw new TerminalStateException( "Error: Problem loading settings from " + c_settingsFile,
                                        ExitCodes.ERROR_LOADING_SETTINGS_CODE );
    }
  }

  @Nonnull
  private static ApplicationConfig loadDependenciesYaml()
  {
    try
    {
      return ApplicationConfig.parse( c_dependenciesFile );
    }
    catch ( final Throwable t )
    {
      throw new TerminalStateException( "Error: Failed to read dependencies file " + c_dependenciesFile,
                                        t,
                                        ExitCodes.ERROR_PARSING_DEPENDENCIES_CODE );
    }
  }

  private static void setupLogger()
  {
    final ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter( new RawFormatter() );
    handler.setLevel( Level.ALL );
    final Logger logger = c_environment.logger();
    logger.setUseParentHandlers( false );
    logger.addHandler( handler );
    logger.setLevel( Level.INFO );
  }

  private static boolean processOptions( @Nonnull final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    final Logger logger = c_environment.logger();
    if ( null != parser.getErrorString() )
    {
      logger.log( Level.SEVERE, "Error: " + parser.getErrorString() );
      return false;
    }
    // Get a list of parsed options
    final List<CLOption> options = parser.getArguments();
    for ( final CLOption option : options )
    {
      switch ( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
        {
          logger.log( Level.SEVERE, "Error: Unexpected argument: " + option.getArgument() );
          return false;
        }

        case Options.DEPENDENCIES_FILE_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            logger.log( Level.SEVERE,
                        "Error: Specified dependencies file does not exist. Specified value: " + argument );
            return false;
          }
          c_dependenciesFile = file.toPath().toAbsolutePath().normalize();
          break;
        }
        case Options.SETTINGS_FILE_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            logger.log( Level.SEVERE,
                        "Error: Specified settings file does not exist. Specified value: " + argument );
            return false;
          }
          c_settingsFile = file.toPath().toAbsolutePath().normalize();
          break;
        }

        case Options.CACHE_DIR_OPT:
        {
          final String argument = option.getArgument();
          final File dir = new File( argument );
          if ( dir.exists() && !dir.isDirectory() )
          {
            logger.log( Level.SEVERE,
                        "Error: Specified cache directory exists but is not a directory. Specified value: " +
                        argument );
            return false;
          }
          c_cacheDir = dir.toPath();
          break;
        }

        case EMIT_DEPENDENCY_GRAPH_OPT:
        {
          c_emitDependencyGraph = true;
          break;
        }

        case VERBOSE_OPT:
        {
          logger.setLevel( Level.ALL );
          break;
        }
        case QUIET_OPT:
        {
          logger.setLevel( Level.WARNING );
          break;
        }
        case HELP_OPT:
        {
          printUsage();
          return false;
        }
      }
    }

    if ( null == c_dependenciesFile )
    {
      final File file = Paths.get( Options.DEFAULT_DEPENDENCIES_FILE ).toFile();
      if ( !file.exists() )
      {
        logger.log( Level.SEVERE, "Error: Default dependencies file does not exist: " + file );
        return false;
      }
      c_dependenciesFile = file.toPath();
    }
    if ( null == c_settingsFile )
    {
      c_settingsFile =
        Paths.get( System.getProperty( "user.home" ), ".m2", "settings.xml" ).toAbsolutePath().normalize();
    }
    if ( null == c_cacheDir )
    {
      final File dir = Paths.get( Options.DEFAULT_CACHE_DIR ).toFile();
      if ( dir.exists() && !dir.isDirectory() )
      {
        logger.log( Level.SEVERE, "Error: Default cache directory exists but is not a directory: " + dir );
        return false;
      }
      c_cacheDir = dir.toPath();
    }

    if ( logger.isLoggable( Level.FINE ) )
    {
      logger.log( Level.FINE, "Bazel DepGen Starting..." );
      logger.log( Level.FINE, "  Dependencies file: " + c_dependenciesFile );
      logger.log( Level.FINE, "  Settings file: " + c_settingsFile );
      logger.log( Level.FINE, "  Local Cache directory: " + c_cacheDir );
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );
    c_environment.logger().log( Level.INFO,
                                "java " + Main.class.getName() + " [options]" + lineSeparator +
                                "Options: " + lineSeparator +
                                CLUtil.describeOptions( OPTIONS ) );
  }
}
