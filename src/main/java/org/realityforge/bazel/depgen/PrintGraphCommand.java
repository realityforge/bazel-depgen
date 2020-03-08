package org.realityforge.bazel.depgen;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.record.ApplicationRecord;

final class PrintGraphCommand
  extends Command
{
  PrintGraphCommand()
  {
    super( Main.PRINT_GRAPH_COMMAND, "Compute and print the dependency graph for the dependency configuration." );
  }

  @Override
  int run( @Nonnull final Context context )
    throws Exception
  {
    final ApplicationRecord record = context.loadRecord();
    final Logger logger = context.environment().logger();
    if ( logger.isLoggable( Level.WARNING ) )
    {
      logger.log( Level.WARNING, "Dependency Graph:" );
      record.getNode().accept( new DependencyGraphEmitter( record.getSource(), l -> logger.log( Level.WARNING, l ) ) );
    }
    return ExitCodes.SUCCESS_EXIT_CODE;
  }
}
