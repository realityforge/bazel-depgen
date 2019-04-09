package org.realityforge.bazel.depgen.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.model.ApplicationModel;

public final class BazelGenerator
{
  @Nonnull
  private final ApplicationModel _model;

  public BazelGenerator( @Nonnull final ApplicationModel model )
  {
    _model = Objects.requireNonNull( model );
  }

  public void generate()
    throws Exception
  {
    final Path extensionFile = _model.getOptions().getExtensionFile();

    mkdirs( extensionFile.getParent() );

    try ( final StarlarkFileOutput output = new StarlarkFileOutput( extensionFile ) )
    {
      emitDoNotEdit( output );

      output.newLine();

      emitArtifactRule( output );

      output.newLine();

      // Load versions rule so can assert bazel version
      output.write( "load(\"@bazel_skylib//:lib/versions.bzl\", \"versions\")" );
      output.newLine();

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
      output.write( "def setup_workspace():" );
      output.incIndent();
      output.write( "\"\"\"Load all dependencies needed by the project.\"\"\"" );
      output.newLine();
      output.write( "# Verify the version of Bazel is compatible" );
      output.write( "versions.check(\"0.23.0\")" );
      output.newLine();

      //TODO: Add assertion in output to verify dependencies.yml file has hash that matches value that was last generated from

      output.decIndent();
    }
  }

  private void mkdirs( final Path path )
  {
    if ( !path.toFile().exists() && !path.toFile().mkdirs() )
    {
      throw new IllegalStateException( "Failed to create directory " + path.toFile() );
    }
  }

  private void emitDoNotEdit( @Nonnull final StarlarkFileOutput output )
    throws IOException
  {
    final Path configLocation = _model.getConfigLocation();
    final Path extensionFile = _model.getOptions().getExtensionFile();
    final Path relativePathToWorkspace =
      extensionFile.getParent().toAbsolutePath().normalize().relativize( configLocation.toAbsolutePath().normalize() );
    output.write( "# DO NOT EDIT: File is auto-generated from " + relativePathToWorkspace );
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
