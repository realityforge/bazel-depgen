package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.OptionsModel;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;

final class HashCommand
  extends ConfigurableCommand
{
  private static final int VERIFY_SHA256_OPT = 1;
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]
    {
      new CLOptionDescriptor( "verify-sha256",
                              CLOptionDescriptor.ARGUMENT_REQUIRED,
                              VERIFY_SHA256_OPT,
                              "Return zero if the calculated hash matches specified sha256 value " +
                              "otherwise return a non-zero value." )
    };
  @Nullable
  private String _expectedSha256;

  HashCommand()
  {
    super( Main.HASH_COMMAND, OPTIONS );
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
        case VERIFY_SHA256_OPT:
        {
          _expectedSha256 = option.getArgument();
          break;
        }
      }
    }

    return true;
  }

  @Override
  int run( @Nonnull final Context context )
  {
    final ApplicationModel model = context.loadModel();
    final String configSha256 = model.getConfigSha256();
    final Logger logger = context.environment().logger();
    if ( null == _expectedSha256 )
    {
      if ( logger.isLoggable( Level.WARNING ) )
      {
        logger.log( Level.WARNING, "Content SHA256: " + configSha256 );
      }
      return ExitCodes.SUCCESS_EXIT_CODE;
    }
    else if ( _expectedSha256.equals( configSha256 ) )
    {
      // If we are verifying, it is quiet mode and verification is a success, there
      // is no need to emit any message. This stops the tool filling up bazel build
      // logs with spurious messages.
      if ( logger.isLoggable( Level.INFO ) )
      {
        logger.log( Level.INFO, "Content SHA256: " + configSha256 );
      }
      return ExitCodes.SUCCESS_EXIT_CODE;
    }
    else
    {
      if ( logger.isLoggable( Level.WARNING ) )
      {
        logger.log( Level.WARNING, "Content SHA256: " + configSha256 + " (Expected " + _expectedSha256 + ")" );
        final OptionsModel options = model.getOptions();
        if ( options.verifyConfigSha256() )
        {
          final Path pathToExtensionDir =
            options.getWorkspaceDirectory().relativize( options.getExtensionFile().getParent() );
          final Path extensionFile = pathToExtensionDir.resolve( options.getExtensionFile().getFileName() );
          final Path configLocation = pathToExtensionDir.resolve( model.getConfigLocation().getFileName() );
          logger.log( Level.WARNING,
                      "Depgen generated extension file '" + extensionFile + "' is out of date with " +
                      "the configuration file '" + configLocation + "." );
          logger.log( Level.WARNING,
                      "Please run command 'bazel run //" + pathToExtensionDir + ":regenerate_depgen_extension' " +
                      "to update the extension." );
        }
      }
      return ExitCodes.ERROR_BAD_SHA256_CONFIG_CODE;
    }
  }
}
