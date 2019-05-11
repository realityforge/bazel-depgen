package org.realityforge.bazel.depgen.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
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
      try ( final StarlarkFileOutput output = new StarlarkFileOutput( buildfile ) )
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
    try ( final StarlarkFileOutput output = new StarlarkFileOutput( extensionFile ) )
    {
      emitDoNotEdit( output );

      output.newLine();

      emitModuleDocstring( output );

      emitDependencyGraphIfRequired( output );

      output.write( "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")" );
      output.newLine();

      output.writeMacroStart( _record.getSource().getOptions().getWorkspaceMacroName(),
                              _record.getArtifacts()
                                .stream()
                                .filter( a -> null == a.getReplacementModel() )
                                .map( a -> "omit_" + a.getAlias() + " = False" )
                                .collect( Collectors.toList() ) );
      output.incIndent();
      output.write( "\"\"\"" );
      output.incIndent();
      output.write( "Repository rules macro to load dependencies specified by '" +
                    getRelativePathToDependenciesYaml() +
                    "'." );
      output.newLine();
      output.write( "Must be run from a WORKSPACE file." );
      output.decIndent();
      output.write( "\"\"\"" );

      for ( final ArtifactRecord artifact : _record.getArtifacts() )
      {
        if ( null != artifact.getReplacementModel() )
        {
          continue;
        }
        output.newLine();
        output.write( "if not omit_" + artifact.getAlias() + ":" );
        output.incIndent();
        emitArtifactHttpFileRule( output, artifact );

        final String sourceSha256 = artifact.getSourceSha256();
        if ( null != sourceSha256 )
        {
          output.newLine();
          final List<String> sourceUrls = artifact.getSourceUrls();
          assert null != sourceUrls && !sourceUrls.isEmpty();
          emitArtifactSourcesHttpFileRule( output, artifact );
        }
        output.decIndent();
      }

      output.decIndent();

      output.newLine();

      output.writeMacroStart( _record.getSource().getOptions().getTargetMacroName(),
                              _record.getArtifacts()
                                .stream()
                                .filter( a -> null == a.getReplacementModel() )
                                .map( a -> "omit_" + a.getAlias() + " = False" )
                                .collect( Collectors.toList() ) );
      output.incIndent();
      output.write( "\"\"\"" );
      output.incIndent();
      output.write( "Macro to define targets for dependencies specified by '" +
                    getRelativePathToDependenciesYaml() +
                    "'." );
      output.decIndent();
      output.write( "\"\"\"" );

      for ( final ArtifactRecord artifact : _record.getArtifacts() )
      {
        if ( null != artifact.getReplacementModel() )
        {
          continue;
        }
        output.newLine();
        output.write( "if not omit_" + artifact.getAlias() + ":" );
        output.incIndent();
        emitAlias( output, artifact );
        emitJavaImport( output, artifact );
        output.decIndent();
      }

      output.decIndent();
    }
  }

  private void emitJavaImport( @Nonnull final StarlarkFileOutput output, @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + artifact.getName() + "\"" );
    arguments.put( "jars", Collections.singletonList( "\"@" + artifact.getName() + "//file\"" ) );
    arguments.put( "licenses", Collections.singletonList( "\"notice\"" ) );
    if ( null != artifact.getSourceSha256() )
    {
      arguments.put( "srcjar", "\"@" + artifact.getName() + "__sources//file\"" );
    }
    arguments.put( "tags",
                   Collections.singletonList( "\"maven_coordinates=" +
                                              artifact.getMavenCoordinatesBazelTag() +
                                              "\"" ) );
    arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    final List<ArtifactRecord> deps = artifact.getDeps();
    if ( !deps.isEmpty() )
    {
      arguments.put( "deps",
                     deps.stream().map( a -> "\":" + a.getAlias() + "\"" ).sorted().collect( Collectors.toList() ) );
    }
    final List<ArtifactRecord> runtimeDeps = artifact.getRuntimeDeps();
    if ( !runtimeDeps.isEmpty() )
    {
      arguments.put( "runtime_deps",
                     runtimeDeps.stream()
                       .map( a -> "\":" + a.getAlias() + "\"" )
                       .sorted()
                       .collect( Collectors.toList() ) );
    }
    output.writeCall( "native.java_import", arguments );
  }

  private void emitAlias( @Nonnull final StarlarkFileOutput output, @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    final LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
    arguments.put( "name", "\"" + artifact.getAlias() + "\"" );
    arguments.put( "actual", "\":" + artifact.getName() + "\"" );
    if ( null != artifact.getArtifactModel() )
    {
      arguments.put( "visibility", Collections.singletonList( "\"//visibility:public\"" ) );
    }
    else
    {
      arguments.put( "visibility", Collections.singletonList( "\"//visibility:private\"" ) );
    }
    output.writeCall( "native.alias", arguments );
  }

  private void emitModuleDocstring( @Nonnull final StarlarkFileOutput output )
    throws IOException
  {
    output.write( "\"\"\"" );
    output.incIndent();
    output.write( "Macro rules to load dependencies defined in '" + getRelativePathToDependenciesYaml() + "'." );
    output.newLine();
    output.write( "Invoke '" +
                  _record.getSource().getOptions().getWorkspaceMacroName() +
                  "' from a WORKSPACE file." );
    output.write( "Invoke '" +
                  _record.getSource().getOptions().getTargetMacroName() +
                  "' from a BUILD.bazel file." );
    output.decIndent();
    output.write( "\"\"\"" );
  }

  private void emitArtifactSourcesHttpFileRule( @Nonnull final StarlarkFileOutput output,
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

  private void emitArtifactHttpFileRule( @Nonnull final StarlarkFileOutput output,
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

  private void emitDependencyGraphIfRequired( @Nonnull final StarlarkFileOutput output )
    throws IOException
  {
    if ( _record.getSource().getOptions().emitDependencyGraph() )
    {
      output.write( "# Dependency Graph Generated from the input data" );
      _record.getNode().accept( new DependencyGraphEmitter( line -> {
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

  private void emitDoNotEdit( @Nonnull final StarlarkFileOutput output )
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
