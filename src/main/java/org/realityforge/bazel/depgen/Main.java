package org.realityforge.bazel.depgen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyCycle;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.InvalidModelException;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;
import org.realityforge.bazel.depgen.util.ArtifactUtil;
import org.realityforge.bazel.depgen.util.BazelUtil;
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
  private static final int VERSION_OPT = 2;
  private static final int HELP_OPT = 'h';
  private static final int QUIET_OPT = 'q';
  private static final int VERBOSE_OPT = 'v';
  private static final int RESET_CACHED_METADATA_OPT = 1;
  private static final int RUN_DIR_OPT = 'd';
  private static final int CACHE_DIR_OPT = 'r';
  private static final int SETTINGS_FILE_OPT = 's';
  private static final int CONFIG_FILE_OPT = 'c';
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]
    {
      new CLOptionDescriptor( "version",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              VERSION_OPT,
                              "print the version and exit" ),
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
      new CLOptionDescriptor( "directory",
                              CLOptionDescriptor.ARGUMENT_REQUIRED,
                              RUN_DIR_OPT,
                              "The directory to run the tool from." ),
      new CLOptionDescriptor( "config-file",
                              CLOptionDescriptor.ARGUMENT_REQUIRED,
                              CONFIG_FILE_OPT,
                              "The path to the yaml file containing the dependency configuration. Defaults" +
                              " to '" + ApplicationConfig.DEFAULT_MODULE + "/" + ApplicationConfig.FILENAME + "'." ),
      new CLOptionDescriptor( "settings-file",
                              CLOptionDescriptor.ARGUMENT_REQUIRED,
                              SETTINGS_FILE_OPT,
                              "The path to the settings.xml used by Maven to extract repository credentials. " +
                              "Defaults to '~/.m2/settings.xml'." ),
      new CLOptionDescriptor( "cache-directory",
                              CLOptionDescriptor.ARGUMENT_REQUIRED,
                              CACHE_DIR_OPT,
                              "The path to the directory in which to cache downloads from remote " +
                              "repositories. Defaults to \"$(bazel info output_base)/.depgen-cache\"." ),
      new CLOptionDescriptor( "reset-cached-metadata",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              RESET_CACHED_METADATA_OPT,
                              "Recalculate metadata about an artifact." )
    };
  static final String GENERATE_COMMAND = "generate";
  static final String PRINT_GRAPH_COMMAND = "print-graph";
  static final String INIT_COMMAND = "init";
  static final String HASH_COMMAND = "hash";
  static final String INFO_COMMAND = "info";
  private static final Map<String, Supplier<Command>> COMMAND_MAP =
    Collections.unmodifiableMap( new HashMap<String, Supplier<Command>>()
    {
      {
        put( GENERATE_COMMAND, GenerateCommand::new );
        put( PRINT_GRAPH_COMMAND, PrintGraphCommand::new );
        put( HASH_COMMAND, HashCommand::new );
        put( INFO_COMMAND, InfoCommand::new );
        put( INIT_COMMAND, InitCommand::new );
      }
    } );
  private static final Set<String> VALID_COMMANDS = Collections.unmodifiableSet( COMMAND_MAP.keySet() );

  public static void main( final String[] args )
  {
    final Environment environment =
      new Environment( System.console(), Paths.get( "" ).toAbsolutePath(), Logger.getGlobal() );
    setupLogger( environment );
    System.exit( run( environment, args ) );
  }

  static int run( @Nonnull final Environment environment, @Nonnull final String... args )
  {
    if ( !processOptions( environment, args ) )
    {
      return ExitCodes.ERROR_PARSING_ARGS_EXIT_CODE;
    }

    try
    {
      return environment.getCommand().run( new CommandContextImpl( environment ) );
    }
    catch ( final InvalidModelException ime )
    {
      final Logger logger = environment.logger();
      final String message = ime.getMessage();
      if ( null != message )
      {
        logger.log( Level.WARNING, message, ime.getCause() );
      }

      logger.log( Level.WARNING,
                  "--- Invalid Config ---\n" +
                  YamlUtil.asYamlString( ime.getModel() ) +
                  "--- End Config ---" );

      return ExitCodes.ERROR_CONSTRUCTING_MODEL_CODE;
    }
    catch ( final TerminalStateException tse )
    {
      final String message = tse.getMessage();
      if ( null != message )
      {
        final Logger logger = environment.logger();
        logger.log( Level.WARNING, message );
        final Throwable cause = tse.getCause();
        if ( null != cause )
        {
          if ( logger.isLoggable( Level.INFO ) )
          {
            logger.log( Level.INFO, "Cause: " + cause.toString() );
            if ( logger.isLoggable( Level.FINE ) )
            {
              logger.log( Level.FINE, null, cause );
            }
          }
        }
      }
      return tse.getExitCode();
    }
    catch ( final Throwable t )
    {
      environment.logger().log( Level.WARNING, t.toString(), t );
      return ExitCodes.ERROR_EXIT_CODE;
    }
  }

  @Nonnull
  static ApplicationModel loadModel( @Nonnull final Environment environment )
  {
    return ApplicationModel.parse( loadConfigFile( environment ), environment.shouldResetCachedMetadata() );
  }

  @Nonnull
  static ApplicationRecord loadRecord( @Nonnull final Environment environment )
    throws DependencyResolutionException
  {
    final ApplicationModel model = loadModel( environment );
    final Resolver resolver =
      ResolverUtil.createResolver( environment,
                                   getCacheDirectory( environment, model ),
                                   model,
                                   loadSettings( environment ) );
    final ApplicationRecord record =
      ApplicationRecord.build( model,
                               resolveModel( environment, resolver, model ),
                               resolver.getAuthenticationContexts(),
                               m -> environment.logger().warning( m ) );
    cacheArtifactsInRepositoryCache( environment, record );
    return record;
  }

  static void cacheArtifactsInRepositoryCache( @Nonnull final Environment environment,
                                               @Nonnull final ApplicationRecord record )
  {
    final Path repositoryCache = BazelUtil.getRepositoryCache( environment.currentDirectory().toFile() );
    if ( null != repositoryCache )
    {
      // We only attempt to copy into repositoryCache if there is one ... which there
      // always is if there is a local WORKSPACE
      for ( final ArtifactRecord artifact : record.getArtifacts() )
      {
        if ( null == artifact.getReplacementModel() )
        {
          final Artifact a = artifact.getArtifact();
          final File file = a.getFile();
          assert null != file;
          final String sha256 = artifact.getSha256();
          assert null != sha256;
          cacheRepositoryFile( environment.logger(), repositoryCache, a.toString(), file, sha256 );
          final String sourceSha256 = artifact.getSourceSha256();
          if ( null != sourceSha256 )
          {
            final SubArtifact sourcesArtifact = new SubArtifact( a, "sources", "jar" );
            final String localFilename = ArtifactUtil.artifactToLocalFilename( sourcesArtifact );
            final File sourcesFile = file.toPath().getParent().resolve( localFilename ).toFile();

            cacheRepositoryFile( environment.logger(),
                                 repositoryCache,
                                 sourcesArtifact.toString(),
                                 sourcesFile,
                                 sourceSha256 );
          }
        }
      }
    }
  }

  static void cacheRepositoryFile( @Nonnull final Logger logger,
                                   @Nonnull final Path repositoryCache,
                                   @Nonnull final String label,
                                   @Nonnull final File file,
                                   @Nonnull final String sha256 )
  {
    final Path targetPath =
      repositoryCache.resolve( "content_addressable" ).resolve( "sha256" ).resolve( sha256 ).resolve( "file" );
    if ( !Files.exists( targetPath ) )
    {
      try
      {
        Files.createDirectories( targetPath.getParent() );
        Files.copy( file.toPath(), targetPath );
        logger.log( Level.FINE, "Installed artifact '" + label + "' into repository cache." );
      }
      catch ( final IOException ioe )
      {
        final String message = "Failed to cache artifact '" + label + "' in repository cache.";
        logger.log( Level.WARNING, message, ioe );
      }
    }
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
  private static DependencyNode resolveModel( @Nonnull final Environment environment,
                                              @Nonnull final Resolver resolver,
                                              @Nonnull final ApplicationModel model )
    throws DependencyResolutionException
  {
    final Logger logger = environment.logger();
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

  @Nonnull
  private static Settings loadSettings( @Nonnull final Environment environment )
  {
    final Path settingsFile = environment.getSettingsFile();
    try
    {
      return SettingsUtil.loadSettings( settingsFile, environment.logger() );
    }
    catch ( final SettingsBuildingException e )
    {
      throw new TerminalStateException( "Error: Problem loading settings from " + settingsFile,
                                        ExitCodes.ERROR_LOADING_SETTINGS_CODE );
    }
  }

  @Nonnull
  static ApplicationConfig loadConfigFile( @Nonnull final Environment environment )
  {
    final Path dependenciesFile = environment.getConfigFile();
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

  static void setupLogger( @Nonnull final Environment environment )
  {
    final ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter( new RawFormatter() );
    handler.setLevel( Level.ALL );
    final Logger logger = environment.logger();
    logger.setUseParentHandlers( false );
    logger.addHandler( handler );
    logger.setLevel( Level.INFO );
  }

  static boolean processOptions( @Nonnull final Environment environment, @Nonnull final String... args )
  {
    // Parse the arguments
    final CLArgsParser parser =
      new CLArgsParser( args, OPTIONS, lastOptionCode -> CLOption.TEXT_ARGUMENT == lastOptionCode );

    //Make sure that there was no errors parsing arguments
    final Logger logger = environment.logger();
    if ( null != parser.getErrorString() )
    {
      logger.log( Level.SEVERE, "Error: " + parser.getErrorString() );
      return false;
    }
    // Retrieve run directory first as some of the other options are interpreted relative to current directory
    for ( final CLOption option : parser.getArguments() )
    {
      if ( RUN_DIR_OPT == option.getId() )
      {
        final String argument = option.getArgument();
        final Path directory = environment.currentDirectory().resolve( argument ).toAbsolutePath().normalize();
        if ( !Files.exists( directory ) )
        {
          logger.log( Level.SEVERE,
                      "Error: Specified directory does not exist. Specified value: " + argument );
          return false;
        }
        if ( !Files.isDirectory( directory ) )
        {
          logger.log( Level.SEVERE,
                      "Error: Specified directory is not a directory. Specified value: " + argument );
          return false;
        }
        environment.setCurrentDirectory( directory );
      }
    }
    // Get a list of parsed options
    for ( final CLOption option : parser.getArguments() )
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
          environment.setCommand( COMMAND_MAP.get( command ).get() );
          break;
        }
        case RUN_DIR_OPT:
        {
          break;
        }
        case CONFIG_FILE_OPT:
        {
          final String argument = option.getArgument();
          environment.setConfigFile( environment.currentDirectory().resolve( argument ).toAbsolutePath().normalize() );
          break;
        }
        case SETTINGS_FILE_OPT:
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

        case CACHE_DIR_OPT:
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
        case RESET_CACHED_METADATA_OPT:
        {
          environment.markResetCachedMetadata();
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
        case VERSION_OPT:
        {
          environment.logger().log( Level.WARNING, "DepGen Version: " + DepGenConfig.getVersion() );
          return false;
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
    final String[] unParsedArgs = parser.getUnParsedArgs();
    if ( unParsedArgs.length > 0 )
    {
      if ( !environment.getCommand().processOptions( environment, unParsedArgs ) )
      {
        return false;
      }
    }

    if ( environment.hasConfigFile() && environment.getCommand().requireConfigFile() )
    {
      if ( !environment.getConfigFile().toFile().exists() )
      {
        logger.log( Level.SEVERE,
                    "Error: Specified config file does not exist. Specified value: " + environment.getConfigFile() );
        return false;
      }
    }

    if ( !environment.hasConfigFile() )
    {
      final Path configFile =
        environment.currentDirectory()
          .resolve( ApplicationConfig.DEFAULT_MODULE )
          .resolve( ApplicationConfig.FILENAME )
          .toAbsolutePath()
          .normalize();
      if ( environment.getCommand().requireConfigFile() && !configFile.toFile().exists() )
      {
        logger.log( Level.SEVERE,
                    "Error: Default config file does not exist: " +
                    ApplicationConfig.DEFAULT_MODULE + "/" + ApplicationConfig.FILENAME );
        return false;
      }
      environment.setConfigFile( configFile );
    }

    if ( !environment.hasSettingsFile() )
    {
      final Path settingsFile =
        Paths.get( System.getProperty( "user.home" ), ".m2", "settings.xml" ).toAbsolutePath().normalize();
      environment.setSettingsFile( settingsFile );
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  static void printUsage( @Nonnull final Environment environment )
  {
    final Logger logger = environment.logger();
    logger.severe( "java " + Main.class.getName() + " [options] [command]" );
    logger.severe( "\tPossible Commands:" );
    logger.severe( "\t\t" + GENERATE_COMMAND + ": Generate the bazel extension from the dependency configuration." );
    logger.severe( "\t\t" + PRINT_GRAPH_COMMAND + ": Compute and print the dependency graph " +
                   "for the dependency configuration." );
    logger.severe( "\t\t" + HASH_COMMAND + ": Generate a hash of the content of the dependency configuration." );
    logger.severe( "\t\t" +
                   INIT_COMMAND +
                   ": Initialize an empty dependency configuration and workspace infrastructure." );
    logger.severe( "\t\t" + INFO_COMMAND + ": Print runtime info about the tool." );
    logger.severe( "\tOptions:" );
    final String[] options =
      CLUtil.describeOptions( OPTIONS ).toString().split( System.getProperty( "line.separator" ) );
    for ( final String line : options )
    {
      logger.severe( line );
    }
  }
}
