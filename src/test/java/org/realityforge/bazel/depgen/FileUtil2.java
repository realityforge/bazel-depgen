package org.realityforge.bazel.depgen;

import gir.Task;
import gir.io.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import static org.testng.Assert.*;

/**
 * TODO: Almost all methods in this class should be migrated to gir.
 */
@SuppressWarnings( "WeakerAccess" )
public final class FileUtil2
{
  /**
   * Run the supplied action in temp directory.
   *
   * @param action the action.
   * @throws Exception if action throws an error.
   */
  public static void inTempDir( @Nonnull final Task action )
    throws Exception
  {
    final Path dir = createTempDir();
    try
    {
      FileUtil.inDirectory( dir, action );
    }
    finally
    {
      FileUtil.deleteDirIfExists( dir );
    }
  }

  @Nonnull
  public static Path createTempDir()
    throws IOException
  {
    final File dir = File.createTempFile( "bazel-depgen", "dir" );
    assertTrue( dir.delete() );
    assertTrue( dir.mkdir() );
    return dir.toPath();
  }

  public static void write( @Nonnull final String pathElement, @Nonnull final String content )
    throws IOException
  {
    write( pathElement, content.getBytes() );
  }

  public static void write( @Nonnull final String pathElement, @Nonnull final byte[] data )
    throws IOException
  {
    final Path baseDirectory = FileUtil.getCurrentDirectory();
    final Path path = baseDirectory.resolve( pathElement ).toAbsolutePath().normalize();
    //noinspection ResultOfMethodCallIgnored
    path.getParent().toFile().mkdirs();
    Files.write( path, data );
  }
}
