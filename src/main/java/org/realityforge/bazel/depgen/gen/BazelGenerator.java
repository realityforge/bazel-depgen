package org.realityforge.bazel.depgen.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.record.ApplicationRecord;

@SuppressWarnings( { "Duplicates" } )
public final class BazelGenerator
{
  @Nonnull
  private final ApplicationRecord _record;

  public BazelGenerator( @Nonnull final ApplicationRecord record )
  {
    _record = Objects.requireNonNull( record );
  }

  public void generate()
    throws Exception
  {
    final Path extensionFile = _record.getSource().getOptions().getExtensionFile();
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
      try ( final StarlarkOutput output1 = new StarlarkOutput( buildfile ) )
      {
        writeDefaultBuild( output1 );
      }
    }

    try ( final StarlarkOutput output = new StarlarkOutput( extensionFile ) )
    {
      _record.writeBazelExtension( output );
    }
  }

  private void writeDefaultBuild( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.write( "# File is auto-generated from " +
                  _record.getPathFromExtensionToConfig() +
                  " by https://github.com/realityforge/bazel-depgen" );
    output.write( "# Contents can be edited and will not be overridden." );

    output.write( "package(default_visibility = [\"//visibility:public\"])" );
    output.newLine();

    final Path extensionFile = _record.getSource().getOptions().getExtensionFile();
    final Path workspaceDirectory = _record.getSource().getOptions().getWorkspaceDirectory();

    output.write( "load(\"//" +
                  workspaceDirectory.relativize( extensionFile.getParent() ) +
                  ":" +
                  extensionFile.getName( extensionFile.getNameCount() - 1 ) +
                  "\", \"" +
                  _record.getSource().getOptions().getTargetMacroName() +
                  "\")" );
    output.newLine();

    output.write( _record.getSource().getOptions().getTargetMacroName() + "()" );
  }
}
