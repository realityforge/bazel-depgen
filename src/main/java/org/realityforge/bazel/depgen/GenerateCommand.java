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
    final OptionsModel options = record.getSource().getOptions();
    final Path extensionFile = options.getExtensionFile();
    final Path dir = extensionFile.getParent();
    final Path extensionBuildfile = dir.resolve( "BUILD.bazel" );

    if ( !dir.toFile().exists() && !dir.toFile().mkdirs() )
    {
      throw new IllegalStateException( "Failed to create directory " + dir.toFile() );
    }

    // The tool will emit the `BUILD.bazel` file for the package containing the extension
    // if none exist. If a `BUILD.bazel` exists then the tool assumes the user has supplied
    // it or it is an artifact from a previous run.
    if ( !extensionBuildfile.toFile().exists() )
    {
      try ( final StarlarkOutput output = new StarlarkOutput( extensionBuildfile ) )
      {
        record.writeDefaultDependenciesBuild( output );
      }
    }

    try ( final StarlarkOutput output = new StarlarkOutput( extensionFile ) )
    {
      record.writeBazelExtension( output );
    }
    return ExitCodes.SUCCESS_EXIT_CODE;
  }
}
