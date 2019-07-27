package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.util.StarlarkOutput;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;

final class InitCommand
  extends ConfigurableCommand
{
  private static final int NO_CREATE_WORKSPACE_OPT = 1;
  private static final int NO_GENERATE_OPT = 2;
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]
    {
      new CLOptionDescriptor( "no-create-workspace",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              NO_CREATE_WORKSPACE_OPT,
                              "Skip generation of WORKSPACE file even if it is not present." ),
      new CLOptionDescriptor( "no-generate",
                              CLOptionDescriptor.ARGUMENT_DISALLOWED,
                              NO_GENERATE_OPT,
                              "Skip running generate command after initializing configuration." )
    };
  private boolean _createWorkspace = true;
  private boolean _runGenerate = true;

  InitCommand()
  {
    super( Main.INIT_COMMAND, OPTIONS );
  }

  @Override
  boolean processArguments( @Nonnull final Environment environment, @Nonnull final List<CLOption> arguments )
  {
    // Get a list of parsed options
    for ( final CLOption option : arguments )
    {
      switch ( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
        {
          final String argument = option.getArgument();
          environment.logger().log( Level.SEVERE, "Error: Invalid argument: " + argument );
          return false;
        }
        case NO_CREATE_WORKSPACE_OPT:
        {
          _createWorkspace = false;
          break;
        }
        case NO_GENERATE_OPT:
        {
          _runGenerate = false;
          break;
        }
      }
    }

    return true;
  }

  @Override
  int run( @Nonnull final Context context )
    throws Exception
  {
    final Environment environment = context.environment();
    final Path configFile = environment.getConfigFile();
    final Logger logger = environment.logger();
    final Path workspaceDir = environment.currentDirectory();
    if ( Files.exists( configFile ) )
    {
      if ( logger.isLoggable( Level.WARNING ) )
      {
        logger.log( Level.WARNING, "Error: Configuration file already exists. File: " + configFile );
      }
      return ExitCodes.ERROR_DEPENDENCY_CONFIG_PRESENT_CODE;
    }
    else
    {
      if ( !createConfigDirectory( logger, configFile ) )
      {
        return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
      }
      else if ( !createConfigFile( logger, configFile, workspaceDir ) )
      {
        return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
      }
      else if ( _createWorkspace )
      {
        final Path workspaceFile = workspaceDir.resolve( "WORKSPACE" );
        if ( !Files.exists( workspaceFile ) )
        {
          if ( !createWorkspaceFile( logger, workspaceFile, configFile ) )
          {
            return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
          }
        }
      }

      if ( _runGenerate )
      {
        return new GenerateCommand().run( context );
      }
      else
      {
        return ExitCodes.SUCCESS_EXIT_CODE;
      }
    }
  }

  private boolean createConfigDirectory( @Nonnull final Logger logger, @Nonnull final Path configFile )
  {
    final Path configDirectory = configFile.getParent();
    if ( !Files.exists( configDirectory ) )
    {
      try
      {
        Files.createDirectories( configDirectory );
      }
      catch ( final IOException e )
      {
        if ( logger.isLoggable( Level.WARNING ) )
        {
          logger.log( Level.WARNING,
                      "Error: Failed to create directory to contain configuration file. Directory: " +
                      configDirectory );
          logger.log( Level.WARNING, e.toString() );
        }
        return false;
      }
      if ( logger.isLoggable( Level.FINE ) )
      {
        logger.log( Level.FINE, "Created configuration directory " + configDirectory );
      }
    }
    return true;
  }

  private boolean createConfigFile( @Nonnull final Logger logger,
                                    @Nonnull final Path configFile,
                                    @Nonnull final Path workspaceDir )
  {
    try
    {
      final InputStream inputStream = getClass().getResourceAsStream( "templates/dependencies.yml" );
      assert null != inputStream;
      final byte[] data = new byte[ inputStream.available() ];
      final int count = inputStream.read( data );
      if ( data.length != count )
      {
        throw new IOException( "Failed to ready file fully" );
      }

      final String outputData =
        new String( data, StandardCharsets.UTF_8 )
          .replace( "workspaceDirectory: ..",
                    "workspaceDirectory: " + configFile.getParent().relativize( workspaceDir ) );
      Files.write( configFile, outputData.getBytes( StandardCharsets.UTF_8 ) );
    }
    catch ( final IOException e )
    {
      if ( logger.isLoggable( Level.WARNING ) )
      {
        logger.log( Level.WARNING, "Error: Failed to create configuration file. File: " + configFile );
        logger.log( Level.WARNING, e.toString() );
      }
      return false;
    }
    if ( logger.isLoggable( Level.FINE ) )
    {
      logger.log( Level.FINE, "Created configuration file " + configFile );
    }
    return true;
  }

  private boolean createWorkspaceFile( @Nonnull final Logger logger,
                                       @Nonnull final Path workspaceFile,
                                       @Nonnull final Path configFile )
  {
    try
    {
      final Path workspaceDirectory = workspaceFile.getParent();
      final StarlarkOutput output = new StarlarkOutput( workspaceFile );
      output.write( "workspace(name = \"" + workspaceDirectory.getFileName() + "\")" );
      output.newLine();
      output.write( "load(\"//" + workspaceDirectory.relativize( configFile ).getParent() + ":" +
                    configFile.getFileName() + "\", \"generate_workspace_rules\")" );
      output.newLine();
      output.write( "generate_workspace_rules()" );
      output.close();
    }
    catch ( final IOException e )
    {
      if ( logger.isLoggable( Level.WARNING ) )
      {
        logger.log( Level.WARNING, "Error: Failed to create WORKSPACE file. File: " + workspaceFile );
        logger.log( Level.WARNING, e.toString() );
      }
      return false;
    }
    if ( logger.isLoggable( Level.FINE ) )
    {
      logger.log( Level.FINE, "Created WORKSPACE file " + workspaceFile );
    }
    return true;
  }
}
