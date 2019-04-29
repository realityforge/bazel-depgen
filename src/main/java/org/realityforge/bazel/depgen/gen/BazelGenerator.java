package org.realityforge.bazel.depgen.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;

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

      emitArtifactRule( output );

      output.newLine();

      // Load versions rule so can assert bazel version
      output.write( "load(\"@bazel_skylib//:lib/versions.bzl\", \"versions\")" );
      output.newLine();

      for ( final ArtifactRecord artifact : _record.getArtifacts() )
      {
        output.write( "# " + artifact.getKey() );
        output.write( "#   runtimeDeps: " +
                      artifact.getRuntimeDeps()
                        .stream()
                        .map( ArtifactRecord::getKey )
                        .collect( Collectors.joining( " " ) ) );
        output.write( "#   deps: " +
                      artifact.getDeps()
                        .stream()
                        .map( ArtifactRecord::getKey )
                        .collect( Collectors.joining( " " ) ) );
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
      output.write( "def generate_workspace_rules():" );
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
      output.newLine();

      //TODO: Add assertion in output to verify dependencies.yml file has hash that matches value that was last generated from

      output.decIndent();
    }
  }

  private void emitDependencyGraphIfRequired( final StarlarkFileOutput output )
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
