package org.realityforge.bazel.depgen.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.model.OptionsModel;
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

    mkdirs( dir );

    emitBuildFileIfNecessary( buildfile );

    emitExtensionFile( extensionFile );
  }

  private void emitBuildFileIfNecessary( @Nonnull final Path buildfile )
    throws Exception
  {
    // The tool will only emit the `BUILD.bazel` file if none exist. If one exists then
    // the tool assumes the user has supplied it or it is an artifact from a previous run.
    if ( !buildfile.toFile().exists() )
    {
      try ( final StarlarkOutput output = new StarlarkOutput( buildfile ) )
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
  }

  private void emitExtensionFile( @Nonnull final Path extensionFile )
    throws Exception
  {
    try ( final StarlarkOutput output = new StarlarkOutput( extensionFile ) )
    {
      writeBazelExtension( output );
    }
  }

  private void writeBazelExtension( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final Path toConfig = _record.getPathFromExtensionToConfig();
    output.write( "# DO NOT EDIT: File is auto-generated from " + toConfig +
                  " by https://github.com/realityforge/bazel-depgen" );
    output.newLine();

    output.writeMultilineComment( o -> {
      o.write( "Macro rules to load dependencies defined in '" + toConfig + "'." );
      o.newLine();
      final OptionsModel options = _record.getSource().getOptions();
      o.write( "Invoke '" + options.getWorkspaceMacroName() + "' from a WORKSPACE file." );
      o.write( "Invoke '" + options.getTargetMacroName() + "' from a BUILD.bazel file." );
    } );

    _record.emitDependencyGraphIfRequired( output );

    output.write( "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")" );
    output.newLine();

    _record.writeWorkspaceMacro( output );

    output.newLine();

    _record.writeTargetMacro( output );
  }

  private void mkdirs( @Nonnull final Path path )
  {
    if ( !path.toFile().exists() && !path.toFile().mkdirs() )
    {
      throw new IllegalStateException( "Failed to create directory " + path.toFile() );
    }
  }
}
