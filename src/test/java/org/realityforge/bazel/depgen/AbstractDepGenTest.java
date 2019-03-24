package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;

abstract class AbstractDepGenTest
{
  @Nonnull
  final Path getApplicationJar()
  {
    return Paths.get( System.getProperty( "depgen.jar" ) ).toAbsolutePath().normalize();
  }

  final void writeWorkspace( @Nonnull final String content )
    throws IOException
  {
    FileUtil2.write( "WORKSPACE", content );
  }

  final void writeDependencies( @Nonnull final String content )
    throws IOException
  {
    FileUtil2.write( "dependencies.yml", content );
  }
}
