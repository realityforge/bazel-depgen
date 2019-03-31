package org.realityforge.bazel.depgen;

import gir.GirException;
import gir.io.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nonnull;

//TODO: This should move to Gir
final class FileUtil2
{
  private FileUtil2()
  {
  }

  /**
   * Create temp directory inside current directory.
   *
   * @return the new temp directory.
   */
  @Nonnull
  static Path createLocalTempDir()
    throws IOException
  {
    final File dir = File.createTempFile( "gir", "dir", FileUtil.getCurrentDirectory().toFile() );
    if ( !dir.delete() )
    {
      throw new GirException( "Failed to delete intermediate tmp file: " + dir );
    }
    if ( !dir.mkdir() )
    {
      throw new GirException( "Failed to create tmp dir: " + dir );
    }
    return dir.toPath().toAbsolutePath().normalize();
  }
}
