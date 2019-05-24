package org.realityforge.bazel.depgen.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;

@SuppressWarnings( { "Duplicates", "StringBufferReplaceableByString" } )
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
                      getRelativePathToDependenciesYaml() +
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

  private void emitExtensionFile( final Path extensionFile )
    throws Exception
  {
    try ( final StarlarkOutput output = new StarlarkOutput( extensionFile ) )
    {
      emitDoNotEdit( output );

      output.newLine();

      emitModuleDocstring( output );

      emitDependencyGraphIfRequired( output );

      output.write( "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")" );
      output.newLine();

      output.writeMacro( _record.getSource().getOptions().getWorkspaceMacroName(),
                         _record.getArtifacts()
                           .stream()
                           .filter( a -> null == a.getReplacementModel() )
                           .map( a -> "omit_" + a.getAlias() + " = False" )
                           .collect( Collectors.toList() ), macro -> {
          macro.writeMultilineComment( o -> {
            o.write( "Repository rules macro to load dependencies specified by '" +
                     getRelativePathToDependenciesYaml() +
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
                emitArtifactHttpFileRule( o, artifact );

                final String sourceSha256 = artifact.getSourceSha256();
                if ( null != sourceSha256 )
                {
                  o.newLine();
                  final List<String> sourceUrls = artifact.getSourceUrls();
                  assert null != sourceUrls && !sourceUrls.isEmpty();
                  emitArtifactSourcesHttpFileRule( o, artifact );
                }
              } );
            }
          }
        } );

      output.newLine();

      output.writeMacro( _record.getSource().getOptions().getTargetMacroName(),
                         _record.getArtifacts()
                           .stream()
                           .filter( a -> null == a.getReplacementModel() )
                           .map( a -> "omit_" + a.getAlias() + " = False" )
                           .collect( Collectors.toList() ), macro -> {
          macro.writeMultilineComment( o -> o.write( "Macro to define targets for dependencies specified by '" +
                                                     getRelativePathToDependenciesYaml() +
                                                     "'." ) );
          for ( final ArtifactRecord artifact : _record.getArtifacts() )
          {
            emitArtifact( macro, artifact );
          }
        } );
    }
  }

  private void emitArtifact( @Nonnull final StarlarkOutput output, @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    if ( null == artifact.getReplacementModel() )
    {
      output.newLine();
      output.writeIfCondition( "not omit_" + artifact.getAlias(), o -> emitArtifactTargets( o, artifact ) );
    }
  }

  private void emitArtifactTargets( @Nonnull final StarlarkOutput output,
                                    @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    artifact.emitAlias( output );
    final Nature nature = artifact.getNature();
    if ( Nature.Library == nature )
    {
      artifact.emitJavaImport( output, "" );
    }
    else if ( Nature.Plugin == nature )
    {
      artifact.emitPluginLibrary( output, "" );
    }
    else
    {
      assert Nature.LibraryAndPlugin == nature;
      artifact.emitPluginLibrary( output, "__plugins" );
      artifact.emitJavaLibraryAndPlugin( output );
    }
  }

  private void emitModuleDocstring( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.writeMultilineComment( o -> {
      o.write( "Macro rules to load dependencies defined in '" + getRelativePathToDependenciesYaml() + "'." );
      o.newLine();
      o.write( "Invoke '" +
               _record.getSource().getOptions().getWorkspaceMacroName() +
               "' from a WORKSPACE file." );
      o.write( "Invoke '" +
               _record.getSource().getOptions().getTargetMacroName() +
               "' from a BUILD.bazel file." );
    } );
  }

  private void emitArtifactSourcesHttpFileRule( @Nonnull final StarlarkOutput output,
                                                @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    final String sourceSha256 = artifact.getSourceSha256();
    assert null != sourceSha256;

    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + artifact.getName() + "__sources\"" );
    final Artifact a = artifact.getNode().getArtifact();
    assert null != a;
    final StringBuilder sb = new StringBuilder();
    sb.append( "\"" );
    sb.append( a.getGroupId().replaceAll( "\\.", "/" ) );
    sb.append( "/" );
    sb.append( a.getArtifactId() );
    sb.append( "/" );
    sb.append( a.getVersion() );
    sb.append( "/" );
    sb.append( a.getArtifactId() );
    sb.append( "-" );
    sb.append( a.getVersion() );
    sb.append( "-sources." );
    sb.append( a.getExtension() );
    sb.append( "\"" );
    arguments.put( "downloaded_file_path", sb.toString() );
    arguments.put( "sha256", "\"" + sourceSha256.toLowerCase() + "\"" );
    final List<String> urls = artifact.getSourceUrls();
    assert null != urls && !urls.isEmpty();
    arguments.put( "urls", urls.stream().map( v -> "\"" + v + "\"" ).collect( Collectors.toList() ) );
    output.writeCall( "http_file", arguments );
  }

  private void emitArtifactHttpFileRule( @Nonnull final StarlarkOutput output,
                                         @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + artifact.getName() + "\"" );
    final Artifact a = artifact.getNode().getArtifact();
    assert null != a;
    final StringBuilder sb = new StringBuilder();
    sb.append( "\"" );
    sb.append( a.getGroupId().replaceAll( "\\.", "/" ) );
    sb.append( "/" );
    sb.append( a.getArtifactId() );
    sb.append( "/" );
    sb.append( a.getVersion() );
    sb.append( "/" );
    sb.append( a.getArtifactId() );
    sb.append( "-" );
    sb.append( a.getVersion() );
    sb.append( a.getClassifier().isEmpty() ? "" : "-" + a.getClassifier() );
    sb.append( "." );
    sb.append( a.getExtension() );
    sb.append( "\"" );
    arguments.put( "downloaded_file_path", sb.toString() );
    final String sha256 = artifact.getSha256();
    assert null != sha256;
    arguments.put( "sha256", "\"" + sha256.toLowerCase() + "\"" );
    final List<String> urls = artifact.getUrls();
    assert null != urls && !urls.isEmpty();
    arguments.put( "urls", urls.stream().map( v -> "\"" + v + "\"" ).collect( Collectors.toList() ) );
    output.writeCall( "http_file", arguments );
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

  private void emitDoNotEdit( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.write( "# DO NOT EDIT: File is auto-generated from " +
                  getRelativePathToDependenciesYaml() +
                  " by https://github.com/realityforge/bazel-depgen" );
  }

  @Nonnull
  private Path getRelativePathToDependenciesYaml()
  {
    final Path configLocation = _record.getSource().getConfigLocation();
    final Path extensionFile = _record.getSource().getOptions().getExtensionFile();
    return extensionFile.getParent()
      .toAbsolutePath()
      .normalize()
      .relativize( configLocation.toAbsolutePath().normalize() );
  }
}
