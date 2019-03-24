package org.realityforge.bazel.depgen;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.realityforge.bazel.depgen.config.DepGenConfig;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * The entry point in which to run the tool.
 */
public class Main
{
  private static final String DEFAULT_WORKSPACE_DIR = ".";
  private static final String DEFAULT_DEPENDENCIES_FILE = "dependencies.yml";
  private static final String DEFAULT_EXTENSION_FILE = "3rdparty/workspace.bzl";
  private static final String DEFAULT_CACHE_DIR = ".repository";
  private static final int HELP_OPT = 'h';
  private static final int QUIET_OPT = 'q';
  private static final int VERBOSE_OPT = 'v';
  private static final int WORKSPACE_DIR_OPT = 'w';
  private static final int DEPENDENCIES_FILE_OPT = 'd';
  private static final int SETTINGS_FILE_OPT = 's';
  private static final int EXTENSION_FILE_OPT = 'e';
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

    new CLOptionDescriptor( "workspace-dir",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            WORKSPACE_DIR_OPT,
                            "The path to directory containing the WORKSPACE file. Defaults to '" +
                            DEFAULT_WORKSPACE_DIR + "'." ),

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

    new CLOptionDescriptor( "extension-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            EXTENSION_FILE_OPT,
                            "The path to the Bazel extension file that will be generated. Defaults to '" +
                            DEFAULT_EXTENSION_FILE + "' in the workspace directory." ),

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
  private static final Logger c_logger = Logger.getGlobal();
  private static Path c_workspaceDir;
  private static Path c_dependenciesFile;
  private static Path c_settingsFile;
  private static Path c_extensionFile;
  private static Path c_cacheDir;

  public static void main( final String[] args )
    throws ArtifactResolutionException
  {
    setupLogger();
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    try
    {
      final DepGenConfig config = loadDependenciesYaml();

      final RepositorySystem system = newRepositorySystem();
      final RepositorySystemSession session = newRepositorySystemSession( system );

      final List<RemoteRepository> repositories = getRemoteRepositories( config );

      //TODO: Insert code here.
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
  private static List<RemoteRepository> getRemoteRepositories( @Nullable final DepGenConfig config )
  {
    final List<RemoteRepository> repositories = new ArrayList<>();
    final Map<String, String> servers = null != config ? config.getRepositories() : null;
    if ( null == servers )
    {
      final RemoteRepository mavenCentral =
        new RemoteRepository.Builder( "central", "default", "https://repo.maven.apache.org/maven2/" ).build();
      repositories.add( mavenCentral );
    }
    else
    {
      final Settings settings = loadSettings();

      for ( final Map.Entry<String, String> server : servers.entrySet() )
      {
        final RemoteRepository.Builder builder =
          new RemoteRepository.Builder( server.getKey(), "default", server.getValue() );
        final Server serverSetting = settings.getServer( server.getKey() );
        if ( null != serverSetting )
        {
          final Authentication authentication =
            new AuthenticationBuilder()
              .addUsername( serverSetting.getUsername() )
              .addPassword( serverSetting.getPassword() )
              .build();
          builder.setAuthentication( authentication );
        }
        repositories.add( builder.build() );
      }
    }
    return repositories;
  }

  @Nonnull
  private static Settings loadSettings()
  {
    try
    {
      final SettingsBuildingRequest request =
        new DefaultSettingsBuildingRequest().setUserSettingsFile( c_settingsFile.toFile() );
      final SettingsBuildingResult result = new DefaultSettingsBuilderFactory().newInstance().build( request );
      result.getProblems().forEach( problem -> c_logger.warning( problem.toString() ) );
      return result.getEffectiveSettings();
    }
    catch ( final SettingsBuildingException e )
    {
      throw new TerminalStateException( "Error: Problem loading settings from " + c_settingsFile,
                                        ERROR_LOADING_SETTINGS_CODE );
    }
  }

  @Nullable
  private static DepGenConfig loadDependenciesYaml()
  {
    try
    {
      final Yaml yaml = new Yaml( new Constructor( DepGenConfig.class ) );
      return yaml.load( new FileReader( c_dependenciesFile.toFile() ) );
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

        case WORKSPACE_DIR_OPT:
        {
          final String argument = option.getArgument();
          final File dir = new File( argument );
          if ( !dir.exists() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified workspace directory does not exist. Specified value: " + argument );
            return false;
          }
          if ( !dir.isDirectory() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified workspace directory is not a directory. Specified value: " + argument );
            return false;
          }
          final File workspaceFile = new File( dir, "WORKSPACE" );
          if ( !workspaceFile.exists() || !workspaceFile.isFile() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified workspace directory does not contain a WORKSPACE file. Specified value: " +
                          argument );
            return false;
          }
          c_workspaceDir = dir.toPath().toAbsolutePath().normalize();
          break;
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

        case EXTENSION_FILE_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( file.exists() && file.isDirectory() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Specified bazel extension file exists but is a directory. Specified value: " +
                          argument );
            return false;
          }
          c_extensionFile = file.toPath().toAbsolutePath().normalize();
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

    if ( null == c_workspaceDir )
    {
      final Path dir = Paths.get( DEFAULT_WORKSPACE_DIR ).toAbsolutePath().normalize();
      final File workspaceFile = dir.resolve( "WORKSPACE" ).toFile();
      if ( !workspaceFile.exists() || !workspaceFile.isFile() )
      {
        c_logger.log( Level.SEVERE,
                      "Error: Default workspace directory does not contain a WORKSPACE file. Directory: " + dir );
        return false;
      }
      c_workspaceDir = dir;
    }
    if ( null == c_dependenciesFile )
    {
      final File file = c_workspaceDir.resolve( DEFAULT_DEPENDENCIES_FILE ).toFile();
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
    if ( null == c_extensionFile )
    {
      final File file = c_workspaceDir.resolve( DEFAULT_EXTENSION_FILE ).toFile();
      if ( file.exists() && file.isDirectory() )
      {
        c_logger.log( Level.SEVERE,
                      "Error: Default bazel extension file exists but is a directory: " + file );
        return false;
      }
      c_extensionFile = file.toPath();
    }
    if ( null == c_cacheDir )
    {
      final File dir = c_workspaceDir.resolve( DEFAULT_CACHE_DIR ).toFile();
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
      c_logger.log( Level.FINE, "  Workspace directory: " + c_workspaceDir );
      c_logger.log( Level.FINE, "  Dependencies file: " + c_dependenciesFile );
      c_logger.log( Level.FINE, "  Settings file: " + c_settingsFile );
      c_logger.log( Level.FINE, "  Bazel Extension file: " + c_extensionFile );
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

  @Nonnull
  private static RepositorySystem newRepositorySystem()
  {
    // Use the pre-populated DefaultServiceLocator rather than explicitly registering components
    final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
    locator.addService( TransporterFactory.class, FileTransporterFactory.class );
    locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

    locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
    {
      @Override
      public void serviceCreationFailed( @Nonnull final Class<?> type,
                                         @Nonnull final Class<?> impl,
                                         @Nonnull final Throwable exception )
      {
        c_logger.log( Level.SEVERE,
                      "Service creation failed for " + type + " implementation " + impl + ": " + exception.getMessage(),
                      exception );
      }
    } );

    final RepositorySystem service = locator.getService( RepositorySystem.class );
    if ( null == service )
    {
      throw new IllegalStateException( "Unable create RepositorySystem" );
    }
    return service;
  }

  @Nonnull
  private static RepositorySystemSession newRepositorySystemSession( @Nonnull final RepositorySystem system )
  {
    final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    final LocalRepository localRepository = new LocalRepository( c_cacheDir.toString() );

    session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepository ) );

    // Avoid using repositories set up in artifact's pom.xml
    session.setIgnoreArtifactDescriptorRepositories( true );

    session.setTransferListener( new SimpleTransferListener( c_logger ) );
    session.setRepositoryListener( new SimpleRepositoryListener( c_logger ) );

    return session;
  }
}
