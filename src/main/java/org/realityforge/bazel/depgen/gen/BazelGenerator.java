package org.realityforge.bazel.depgen.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;

@SuppressWarnings( "Duplicates" )
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

    mkdirs( extensionFile.getParent() );

    try ( final StarlarkFileOutput output = new StarlarkFileOutput( extensionFile ) )
    {
      emitDoNotEdit( output );

      output.newLine();

      emitDependencyGraphIfRequired( output );

      output.write( "load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')" );
      output.newLine();

      emitArtifactRule( output );

      output.newLine();

      // Load versions rule so can assert bazel version
      output.write( "load(\"@bazel_skylib//:lib/versions.bzl\", \"versions\")" );
      output.newLine();

      for ( final ArtifactRecord artifact : _record.getArtifacts() )
      {
        output.write( "# " + artifact.getKey() );
        output.write( "#   name " + artifact.getName() );
        final String sha256 = artifact.getSha256();
        if ( null != sha256 )
        {
          output.write( "#   sha256 " + sha256 );
          output.write( "#   urls " + artifact.getUrls() );
        }
        final String sourceSha256 = artifact.getSourceSha256();
        if ( null != sourceSha256 )
        {
          output.write( "#   sourceSha256 " + sourceSha256 );
          output.write( "#   sourceUrls " + artifact.getSourceUrls() );
        }
        if ( !artifact.getRuntimeDeps().isEmpty() )
        {
          output.write( "#   runtimeDeps: " +
                        artifact.getRuntimeDeps()
                          .stream()
                          .map( ArtifactRecord::getKey )
                          .collect( Collectors.joining( " " ) ) );
        }
        if ( !artifact.getDeps().isEmpty() )
        {
          output.write( "#   deps: " +
                        artifact.getDeps()
                          .stream()
                          .map( ArtifactRecord::getKey )
                          .collect( Collectors.joining( " " ) ) );
        }
      }

      //TODO: Add omit snippets like
/*
def closure_repositories(
    omit_foo=False,
    omit_bar=False):
  if not omit_foo:
    foo()
  if not omit_bar:
    bar()

def foo():
  native.maven_jar(name = "foo", ...)

def bar():
  native.maven_jar(name = "bar", ...)
*/
      output.write( "def " + _record.getSource().getOptions().getGenerateRulesMacroName() + "():" );
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
      output.newLine();
      output.write( "# Verify the version of Bazel is compatible" );
      output.write( "versions.check(\"0.23.0\")" );

      //TODO: Add assertion in output to verify dependencies.yml file has hash that matches value that was last generated from

      for ( final ArtifactRecord artifact : _record.getArtifacts() )
      {
        if ( null != artifact.getReplacementModel() )
        {
          continue;
        }
        output.newLine();
        emitArtifactHttpFileRule( output, artifact );

        final String sourceSha256 = artifact.getSourceSha256();
        if ( null != sourceSha256 )
        {
          output.newLine();
          final List<String> sourceUrls = artifact.getSourceUrls();
          assert null != sourceUrls && !sourceUrls.isEmpty();
          emitArtifactSourcesHttpFileRule( output, artifact );
        }
      }

      output.decIndent();
    }
  }

  private void emitArtifactSourcesHttpFileRule( @Nonnull final StarlarkFileOutput output,
                                                @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    final String sourceSha256 = artifact.getSourceSha256();
    assert null != sourceSha256;
    final StringBuilder sb = new StringBuilder();
    sb.append( "http_file(name = '" );
    sb.append( artifact.getName() );
    sb.append( "__sources', urls = [" );
    final List<String> urls = artifact.getSourceUrls();
    assert null != urls && !urls.isEmpty();
    sb.append( urls.stream().map( v -> "'" + v + "'" ).collect( Collectors.joining( "," ) ) );
    sb.append( "], downloaded_file_path = '" );
    final Artifact a = artifact.getNode().getArtifact();
    assert null != a;
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
    sb.append( "', sha256 = '" );
    sb.append( sourceSha256 );
    sb.append( "')" );
    output.write( sb.toString() );
  }

  private void emitArtifactHttpFileRule( @Nonnull final StarlarkFileOutput output,
                                         @Nonnull final ArtifactRecord artifact )
    throws IOException
  {
    final StringBuilder sb = new StringBuilder();
    sb.append( "http_file(name = '" );
    sb.append( artifact.getName() );
    sb.append( "', urls = [" );
    final List<String> urls = artifact.getUrls();
    assert null != urls && !urls.isEmpty();
    sb.append( urls.stream().map( v -> "'" + v + "'" ).collect( Collectors.joining( "," ) ) );
    sb.append( "], downloaded_file_path = '" );
    final Artifact a = artifact.getNode().getArtifact();
    assert null != a;
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
    sb.append( "', sha256 = '" );
    sb.append( artifact.getSha256() );
    sb.append( "')" );
    output.write( sb.toString() );
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
    output.write( "# DO NOT EDIT: File is auto-generated from " + getRelativePathToDependenciesYaml() );
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

  private void emitArtifactRule( @Nonnull final StarlarkFileOutput output )
    throws IOException
  {
    final InputStream resource = BazelGenerator.class.getResourceAsStream( "artifact.bzl" );
    if ( null == resource )
    {
      throw new IllegalStateException( "Unable to locate artifact.bzl resource." );
    }

    final BufferedReader stream = new BufferedReader( new InputStreamReader( resource ) );
    String line;
    while ( ( line = stream.readLine() ) != null )
    {
      output.write( line );
    }
  }
}
