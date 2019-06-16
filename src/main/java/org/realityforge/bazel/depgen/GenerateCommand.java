package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.util.StarlarkOutput;

final class GenerateCommand
  extends Command
{
  GenerateCommand()
  {
    super( Main.GENERATE_COMMAND );
  }

  int run( @Nonnull final Context context )
    throws Exception
  {
    final ApplicationRecord record = context.loadRecord();
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
    return ExitCodes.SUCCESS_EXIT_CODE;
  }
}
