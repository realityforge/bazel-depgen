package org.realityforge.bazel.depgen;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

final class HashCommand
  extends Command
{
  HashCommand()
  {
    super( Main.HASH_COMMAND );
  }

  int run( @Nonnull final Context context )
  {
    final String configSha256 = context.loadModel().getConfigSha256();
    final Logger logger = context.environment().logger();
    if ( logger.isLoggable( Level.WARNING ) )
    {
      logger.log( Level.WARNING, "Content SHA256: " + configSha256 );
    }
    return ExitCodes.SUCCESS_EXIT_CODE;
  }
}
