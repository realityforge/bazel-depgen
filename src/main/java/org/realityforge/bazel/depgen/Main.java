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
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyCycle;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.InvalidModelException;
import org.realityforge.bazel.depgen.model.OptionsModel;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * The entry point in which to run the tool.
 */
public class Main
{
  private static final String DEFAULT_DEPENDENCIES_FILE = "dependencies.yml";
  private static final String DEFAULT_CACHE_DIR = ".repository";
  private static final int HELP_OPT = 'h';
  private static final int QUIET_OPT = 'q';
  private static final int VERBOSE_OPT = 'v';
  private static final int DEPENDENCIES_FILE_OPT = 'd';
  private static final int SETTINGS_FILE_OPT = 's';
  private static final int CACHE_DIR_OPT = 'r';
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]{
    new CLOptionDescriptor( "help",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            HELP_OPT,
                            "print this message and exit" ),
    new CLOptionDescriptor( "quiet",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            QUIET_OPT,
                            "Do not output unless an error occurs, just return 0 on no difference.",
                            new int[]{ VERBOSE_OPT } ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "Verbose output of differences.",
                            new int[]{ QUIET_OPT } ),
    new CLOptionDescriptor( "dependencies-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            DEPENDENCIES_FILE_OPT,
                            "The path to the yaml file containing the dependencies. Defaults to '" +
                            DEFAULT_DEPENDENCIES_FILE + "' in the workspace directory." ),
    new CLOptionDescriptor( "settings-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            SETTINGS_FILE_OPT,
                            "The path to the settings.xml used by Maven extract repository credentials. " +
                            "Defaults to '~/.m2/settings.xml'." ),
    new CLOptionDescriptor( "cache-dir",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            CACHE_DIR_OPT,
                            "The path to the directory in which to cache downloads from remote " +
                            "repositories. Defaults to '" + DEFAULT_CACHE_DIR + "' in the workspace directory." )
  };
  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 2;
  private static final int ERROR_PARSING_DEPENDENCIES_CODE = 3;
  private static final int ERROR_LOADING_SETTINGS_CODE = 4;
  private static final int ERROR_CONSTRUCTING_MODEL_CODE = 5;
  private static final int ERROR_INVALID_POM_CODE = 6;
  private static final Logger c_logger = Logger.getGlobal();
  private static Path c_dependenciesFile;
  private static Path c_settingsFile;
  private static Path c_cacheDir;

  public static void main( final String[] args )
  {
    setupLogger();
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    try
    {
      final ApplicationModel model = ApplicationModel.parse( loadDependenciesYaml() );

      final OptionsModel options = model.getOptions();
      final Resolver resolver =
        ResolverUtil.createResolver( c_logger,
                                     c_cacheDir,
                                     ResolverUtil.getRemoteRepositories( model.getRepositories(), loadSettings() ),
                                     options.failOnMissingPom(),
                                     options.failOnInvalidPom() );

      final List<Dependency> dependencies = resolver.deriveRootDependencies( model, ( artifactModel, exceptions ) -> {
        // If we get here then the listener has already emitted a warning message so just need to exit
        // We can only get here if either failOnMissingPom or failOnInvalidPom is true and an error occurred
        System.exit( ERROR_INVALID_POM_CODE );
      } );

      final CollectResult collectResult = resolver.collectDependencies( dependencies );

      final List<DependencyCycle> cycles = collectResult.getCycles();
      if ( !cycles.isEmpty() )
      {
        c_logger.warning( cycles.size() + " dependency cycle detected collecting dependencies:" );
        for ( final DependencyCycle cycle : cycles )
        {
          c_logger.warning( cycle.toString() );
        }

      }
      final List<Exception> exceptions = collectResult.getExceptions();
      if ( !exceptions.isEmpty() )
      {
        c_logger.warning( exceptions.size() + " errors collecting dependencies:" );
        for ( final Exception exception : exceptions )
        {
          c_logger.log( Level.WARNING, null, exception );
        }
      }
    }
    catch ( final InvalidModelException ime )
    {
      final String message = ime.getMessage();
      if ( null != message )
      {
        c_logger.log( Level.WARNING, message, ime.getCause() );
      }
      final Representer representer = new OmitNullRepresenter();

      c_logger.log( Level.WARNING,
                    "--- Invalid Config ---\n" +
                    new Yaml( representer ).dumpAs( ime.getModel(), Tag.MAP, DumperOptions.FlowStyle.BLOCK ) +
                    "--- End Config ---" );

      System.exit( ERROR_CONSTRUCTING_MODEL_CODE );
    }
    catch ( final TerminalStateException tse )
    {
      final String message = tse.getMessage();
      if ( null != message )
      {
        c_logger.log( Level.WARNING, message, tse.getCause() );
      }
      System.exit( tse.getExitCode() );
    }
    catch ( final Throwable t )
    {
      c_logger.log( Level.WARNING, t.toString(), t.getCause() );
      System.exit( ERROR_EXIT_CODE );
    }

    System.exit( SUCCESS_EXIT_CODE );
  }

  @Nonnull
  private static Settings loadSettings()
  {
    try
    {
      return SettingsUtil.loadSettings( c_settingsFile, c_logger );
    }
    catch ( final SettingsBuildingException e )
    {
      throw new TerminalStateException( "Error: Problem loading settings from " + c_settingsFile,
                                        ERROR_LOADING_SETTINGS_CODE );
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
                                        ERROR_PARSING_DEPENDENCIES_CODE );
    }
  }

  private static void setupLogger()
  {
    c_logger.setUseParentHandlers( false );
    final ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter( new RawFormatter() );
    handler.setLevel( Level.ALL );
    c_logger.addHandler( handler );
    c_logger.setLevel( Level.INFO );
  }

  private static boolean processOptions( final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if ( null != parser.getErrorString() )
    {
      c_logger.log( Level.SEVERE, "Error: " + parser.getErrorString() );
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
          c_logger.log( Level.SEVERE, "Error: Unexpected argument: " + option.getArgument() );
          return false;
        }

        case DEPENDENCIES_FILE_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified dependencies file does not exist. Specified value: " + argument );
            return false;
          }
          c_dependenciesFile = file.toPath().toAbsolutePath().normalize();
          break;
        }
        case SETTINGS_FILE_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified settings file does not exist. Specified value: " + argument );
            return false;
          }
          c_settingsFile = file.toPath().toAbsolutePath().normalize();
          break;
        }

        case CACHE_DIR_OPT:
        {
          final String argument = option.getArgument();
          final File dir = new File( argument );
          if ( dir.exists() && !dir.isDirectory() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified cache directory exists but is not a directory. Specified value: " +
                          argument );
            return false;
          }
          c_cacheDir = dir.toPath();
          break;
        }

        case VERBOSE_OPT:
        {
          c_logger.setLevel( Level.ALL );
          break;
        }
        case QUIET_OPT:
        {
          c_logger.setLevel( Level.WARNING );
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
      final File file = Paths.get( DEFAULT_DEPENDENCIES_FILE ).toFile();
      if ( !file.exists() )
      {
        c_logger.log( Level.SEVERE, "Error: Default dependencies file does not exist: " + file );
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
      final File dir = Paths.get( DEFAULT_CACHE_DIR ).toFile();
      if ( dir.exists() && !dir.isDirectory() )
      {
        c_logger.log( Level.SEVERE, "Error: Default cache directory exists but is not a directory: " + dir );
        return false;
      }
      c_cacheDir = dir.toPath();
    }

    if ( c_logger.isLoggable( Level.FINE ) )
    {
      c_logger.log( Level.FINE, "Bazel DepGen Starting..." );
      c_logger.log( Level.FINE, "  Dependencies file: " + c_dependenciesFile );
      c_logger.log( Level.FINE, "  Settings file: " + c_settingsFile );
      c_logger.log( Level.FINE, "  Local Cache directory: " + c_cacheDir );
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );
    c_logger.log( Level.INFO,
                  "java " + Main.class.getName() + " [options]" + lineSeparator + "Options: " + lineSeparator +
                  CLUtil.describeOptions( OPTIONS ) );
  }
}
