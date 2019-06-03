package org.realityforge.bazel.depgen;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.realityforge.bazel.depgen.util.BazelUtil;
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
      Options.RESET_CACHED_METADATA_DESCRIPTOR
    };
  private static final String GENERATE_COMMAND = "generate";
  private static final String PRINT_GRAPH_COMMAND = "print-graph";
  private static final String HASH_COMMAND = "hash";
  private static final Set<String> VALID_COMMANDS =
    Collections.unmodifiableSet( new HashSet<>( Arrays.asList( GENERATE_COMMAND,
                                                               PRINT_GRAPH_COMMAND,
                                                               HASH_COMMAND ) ) );
  private static final Environment c_environment =
    new Environment( System.console(), Paths.get( "" ).toAbsolutePath(), Logger.getGlobal() );
  private static boolean c_resetCachedMetadata = false;

  public static void main( final String[] args )
  {
    setupLogger();
    if ( !processOptions( c_environment, args ) )
    {
      System.exit( ExitCodes.ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    try
    {
      final String command = c_environment.getCommand();
      if ( PRINT_GRAPH_COMMAND.equals( command ) )
      {
        printGraph( loadApplicationRecord() );
      }
      else if ( HASH_COMMAND.equals( command ) )
      {
        hash( loadApplicationModel() );
      }
      else
      {
        assert GENERATE_COMMAND.equals( command );
        generate( loadApplicationRecord() );
      }
    }
    catch ( final InvalidModelException ime )
    {
      final Logger logger = c_environment.logger();
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
        final Logger logger = c_environment.logger();
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
      c_environment.logger().log( Level.WARNING, t.toString(), t );
      System.exit( ExitCodes.ERROR_EXIT_CODE );
    }

    System.exit( ExitCodes.SUCCESS_EXIT_CODE );
  }

  private static void hash( @Nonnull final ApplicationModel model )
  {
    final String configSha256 = model.getConfigSha256();
    final Logger logger = c_environment.logger();
    if ( logger.isLoggable( Level.WARNING ) )
    {
      logger.log( Level.WARNING, "Content SHA256: " + configSha256 );
    }
  }

  private static void printGraph( @Nonnull final ApplicationRecord record )
  {
    final Logger logger = c_environment.logger();
    if ( logger.isLoggable( Level.WARNING ) )
    {
      logger.log( Level.WARNING, "Dependency Graph:" );
      record.getNode()
        .accept( new DependencyGraphEmitter( record.getSource(),
                                             line -> logger.log( Level.WARNING, line ) ) );
    }
  }

  @Nonnull
  private static ApplicationModel loadApplicationModel()
  {
    return ApplicationModel.parse( loadDependenciesYaml(), c_resetCachedMetadata );
  }

  @Nonnull
  private static ApplicationRecord loadApplicationRecord()
    throws DependencyResolutionException
  {
    return loadApplicationRecord( loadApplicationModel() );
  }

  @Nonnull
  private static ApplicationRecord loadApplicationRecord( @Nonnull final ApplicationModel model )
    throws DependencyResolutionException
  {
    final Resolver resolver =
      ResolverUtil.createResolver( c_environment, getCacheDirectory( c_environment, model ), model, loadSettings() );
    return ApplicationRecord.build( model,
                                    resolveModel( resolver, model ),
                                    resolver.getAuthenticationContexts(),
                                    m -> c_environment.logger().warning( m ) );
  }

  @Nonnull
  static Path getCacheDirectory( @Nonnull final Environment environment, @Nonnull final ApplicationModel model )
  {
    if ( environment.hasCacheDir() )
    {
      return environment.getCacheDir();
    }
    else
    {
      final File repositoryCache = BazelUtil.getOutputBase( model.getOptions().getWorkspaceDirectory().toFile() );
      if ( null == repositoryCache )
      {
        throw new TerminalStateException( "Error: Cache directory not specified and unable to derive default " +
                                          "directory (Is the bazel command on the path?). Explicitly pass the " +
                                          "cache directory as an option.",
                                          ExitCodes.ERROR_INVALID_DEFAULT_CACHE_DIR_CODE );
      }
      else
      {
        return repositoryCache.toPath().resolve( ".depgen-cache" );
      }
    }
  }

  @Nonnull
  private static DependencyNode resolveModel( @Nonnull final Resolver resolver, @Nonnull final ApplicationModel model )
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
    final Path settingsFile = c_environment.getSettingsFile();
    try
    {
      return SettingsUtil.loadSettings( settingsFile, c_environment.logger() );
    }
    catch ( final SettingsBuildingException e )
    {
      throw new TerminalStateException( "Error: Problem loading settings from " + settingsFile,
                                        ExitCodes.ERROR_LOADING_SETTINGS_CODE );
    }
  }

  @Nonnull
  private static ApplicationConfig loadDependenciesYaml()
  {
    final Path dependenciesFile = c_environment.getDependenciesFile();
    try
    {
      return ApplicationConfig.parse( dependenciesFile );
    }
    catch ( final Throwable t )
    {
      throw new TerminalStateException( "Error: Failed to read dependencies file " + dependenciesFile,
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

  static boolean processOptions( @Nonnull final Environment environment, @Nonnull final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    final Logger logger = environment.logger();
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
          final String command = option.getArgument();
          if ( !VALID_COMMANDS.contains( command ) )
          {
            logger.log( Level.SEVERE, "Error: Unknown command: " + command );
            return false;
          }
          else if ( environment.hasCommand() )
          {
            logger.log( Level.SEVERE, "Error: Duplicate command specified: " + command );
            return false;
          }
          environment.setCommand( command );
          break;
        }
        case Options.DEPENDENCIES_FILE_OPT:
        {
          final String argument = option.getArgument();
          final Path dependenciesFile = environment.currentDirectory().resolve( argument ).toAbsolutePath().normalize();
          if ( !dependenciesFile.toFile().exists() )
          {
            logger.log( Level.SEVERE,
                        "Error: Specified dependencies file does not exist. Specified value: " + argument );
            return false;
          }
          environment.setDependenciesFile( dependenciesFile );
          break;
        }
        case Options.SETTINGS_FILE_OPT:
        {
          final String argument = option.getArgument();
          final Path settingsFile = environment.currentDirectory().resolve( argument ).toAbsolutePath().normalize();
          if ( !settingsFile.toFile().exists() )
          {
            logger.log( Level.SEVERE,
                        "Error: Specified settings file does not exist. Specified value: " + argument );
            return false;
          }
          environment.setSettingsFile( settingsFile );
          break;
        }

        case Options.CACHE_DIR_OPT:
        {
          final String argument = option.getArgument();
          final Path cacheDir = environment.currentDirectory().resolve( argument ).toAbsolutePath().normalize();
          final File dir = cacheDir.toFile();
          if ( dir.exists() && !dir.isDirectory() )
          {
            logger.log( Level.SEVERE,
                        "Error: Specified cache directory exists but is not a directory. Specified value: " +
                        argument );
            return false;
          }
          environment.setCacheDir( cacheDir );
          break;
        }
        case Options.RESET_CACHED_METADATA_OPT:
        {
          c_resetCachedMetadata = true;
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
          printUsage( environment );
          return false;
        }
      }
    }

    if ( !environment.hasCommand() )
    {
      logger.log( Level.SEVERE, "Error: No command specified. Please specify a command." );
      return false;
    }

    if ( !environment.hasDependenciesFile() )
    {
      final Path dependenciesFile =
        environment.currentDirectory().resolve( Options.DEFAULT_DEPENDENCIES_FILE ).toAbsolutePath().normalize();
      if ( !dependenciesFile.toFile().exists() )
      {
        logger.log( Level.SEVERE,
                    "Error: Default dependencies file does not exist: " + Options.DEFAULT_DEPENDENCIES_FILE );
        return false;
      }
      environment.setDependenciesFile( dependenciesFile );
    }

    if ( !environment.hasSettingsFile() )
    {
      final Path settingsFile =
        Paths.get( System.getProperty( "user.home" ), ".m2", "settings.xml" ).toAbsolutePath().normalize();
      environment.setSettingsFile( settingsFile );
    }

    printBanner( environment );

    return true;
  }

  static void printBanner( @Nonnull final Environment environment )
  {
    final Logger logger = environment.logger();
    if ( logger.isLoggable( Level.FINE ) )
    {
      logger.log( Level.FINE, "Bazel DepGen Starting..." );
      logger.log( Level.FINE, "  Dependencies file: " + environment.getDependenciesFile() );
      logger.log( Level.FINE, "  Settings file: " + environment.getSettingsFile() );
      if ( environment.hasCacheDir() )
      {
        logger.log( Level.FINE, "  Cache directory: " + environment.getCacheDir() );
      }
    }
  }

  /**
   * Print out a usage statement
   */
  static void printUsage( @Nonnull final Environment environment )
  {
    final Logger logger = environment.logger();
    logger.info( "java " + Main.class.getName() + " [options] [command]" );
    logger.info( "\tPossible Commands:" );
    logger.info( "\t\t" + GENERATE_COMMAND + ": Generate the bazel extension from the dependency configuration." );
    logger.info( "\t\t" + PRINT_GRAPH_COMMAND + ": Compute and print the dependency graph " +
                 "for the dependency configuration." );
    logger.info( "\t\t" + HASH_COMMAND + ": Generate a hash of the content of the dependency configuration." );
    logger.info( "\tOptions:" );
    final String[] options =
      CLUtil.describeOptions( OPTIONS ).toString().split( System.getProperty( "line.separator" ) );
    for ( final String line : options )
    {
      logger.info( line );
    }
  }
}
