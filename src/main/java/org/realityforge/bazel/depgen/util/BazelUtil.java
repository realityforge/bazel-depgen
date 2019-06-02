package org.realityforge.bazel.depgen.util;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BazelUtil
{
  private BazelUtil()
  {
  }

  @Nonnull
  public static String cleanNamePart( @Nonnull final String name )
  {
    return name.toLowerCase().replaceAll( "[^a-z0-9]", "_" );
  }

  @Nullable
  public static File getRepositoryCache( @Nonnull final File cwd )
  {
    try
    {
      final String repositoryCache =
        Exec.capture( p -> p.command( "bazel", "info", "repository_cache" ).directory( cwd ), 0 );
      return new File( repositoryCache.trim() );
    }
    catch ( final Exception e )
    {
      return getDefaultRepositoryCache();
    }
  }

  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Nullable
  static File getDefaultRepositoryCache()
  {
    try
    {
      final File dir = File.createTempFile( "bazel-depgen", "dir" );
      dir.delete();
      dir.mkdir();
      final File file = new File( dir, "WORKSPACE" );
      Files.write( file.toPath(), new byte[ 0 ] );
      final String repositoryCache =
        Exec.capture( p -> p.command( "bazel", "info", "repository_cache" ).directory( dir ), 0 );
      file.delete();
      dir.delete();
      return new File( repositoryCache.trim() );
    }
    catch ( final Throwable ignored )
    {
      return null;
    }
  }
}
