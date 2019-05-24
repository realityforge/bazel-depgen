package org.realityforge.bazel.depgen.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;

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
      output.write( "# DO NOT EDIT: File is auto-generated from " +
                    _record.getPathFromExtensionToConfig() +
                    " by https://github.com/realityforge/bazel-depgen" );
      output.newLine();

      output.writeMultilineComment( o -> {
        o.write( "Macro rules to load dependencies defined in '" + _record.getPathFromExtensionToConfig() + "'." );
        o.newLine();
        o.write( "Invoke '" +
                 _record.getSource().getOptions().getWorkspaceMacroName() +
                 "' from a WORKSPACE file." );
        o.write( "Invoke '" +
                 _record.getSource().getOptions().getTargetMacroName() +
                 "' from a BUILD.bazel file." );
      } );

      emitDependencyGraphIfRequired( output );

      output.write( "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")" );
      output.newLine();

      writeWorkspaceMacro( output );

      output.newLine();

      _record.writeTargetMacro( output );
    }
  }

  private void writeWorkspaceMacro( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.writeMacro( _record.getSource().getOptions().getWorkspaceMacroName(),
                       _record.getArtifacts()
                         .stream()
                         .filter( a -> null == a.getReplacementModel() )
                         .map( a -> "omit_" + a.getAlias() + " = False" )
                         .collect( Collectors.toList() ), macro -> {
        macro.writeMultilineComment( o -> {
          o.write( "Repository rules macro to load dependencies specified by '" +
                   _record.getPathFromExtensionToConfig() +
                   "'." );
          o.newLine();
          o.write( "Must be run from a WORKSPACE file." );
        } );

        for ( final ArtifactRecord artifact : _record.getArtifacts() )
        {
          if ( null == artifact.getReplacementModel() )
          {
            macro.newLine();
            macro.writeIfCondition( "not omit_" + artifact.getAlias(), o -> {
              artifact.emitArtifactHttpFileRule( o );

              final String sourceSha256 = artifact.getSourceSha256();
              if ( null != sourceSha256 )
              {
                o.newLine();
                final List<String> sourceUrls = artifact.getSourceUrls();
                assert null != sourceUrls && !sourceUrls.isEmpty();
                artifact.emitArtifactSourcesHttpFileRule( o );
              }
            } );
          }
        }
      } );
  }

  private void emitDependencyGraphIfRequired( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    if ( _record.getSource().getOptions().emitDependencyGraph() )
    {
      output.write( "# Dependency Graph Generated from the input data" );
      _record.getNode().accept( new DependencyGraphEmitter( _record.getSource(), line -> {

        try
        {
          output.write( "# " + line );
        }
        catch ( final IOException ioe )
        {
          throw new IllegalStateException( "Failed to write to file", ioe );
        }
      } ) );
      output.newLine();
    }
  }

  private void mkdirs( @Nonnull final Path path )
  {
    if ( !path.toFile().exists() && !path.toFile().mkdirs() )
    {
      throw new IllegalStateException( "Failed to create directory " + path.toFile() );
    }
  }
}
