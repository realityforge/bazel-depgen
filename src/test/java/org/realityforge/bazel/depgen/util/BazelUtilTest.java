package org.realityforge.bazel.depgen.util;

import gir.io.FileUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class BazelUtilTest
  extends AbstractTest
{
  @Test
  public void cleanNamePart()
  {
    assertEquals( BazelUtil.cleanNamePart( "com.example:mylib:0.98" ), "com_example_mylib_0_98" );
    assertEquals( BazelUtil.cleanNamePart( "com.example:My-App:22-RC1" ), "com_example_my_app_22_rc1" );
  }

  @Test
  public void getDefaultRepositoryCache()
    throws Exception
  {
    // These tests assume that there is no WORKSPACE in parent directory from isolated directory
    // They also assume that bazel is present on build machine
    inIsolatedDirectory( () -> {
      final Path repositoryCache = BazelUtil.getDefaultRepositoryCache();
      assertNotNull( repositoryCache );
      assertTrue( repositoryCache.toAbsolutePath().toString().endsWith( "/cache/repos/v1" ) );
    } );
  }

  @Test
  public void getRepositoryCache()
    throws Exception
  {
    // These tests assume that there is no WORKSPACE in parent directory from isolated directory
    // They also assume that bazel is present on build machine
    inIsolatedDirectory( () -> {
      final Path cwd = FileUtil.getCurrentDirectory();
      final Path dir = FileUtil.createLocalTempDir();
      FileUtil.write( "WORKSPACE", "" );
      writeBazelrc( dir );
      final Path repositoryCache = BazelUtil.getRepositoryCache( cwd.toFile() );
      assertNotNull( repositoryCache );
      assertEquals( repositoryCache.toAbsolutePath().normalize(), dir );
    } );
  }

  @Test
  public void getRepositoryCache_WORKSPACE_notPresent()
    throws Exception
  {
    // These tests assume that there is no WORKSPACE in parent directory from isolated directory
    // They also assume that bazel is present on build machine
    inIsolatedDirectory( () -> {
      final Path repositoryCache = BazelUtil.getRepositoryCache( FileUtil.getCurrentDirectory().toFile() );
      assertNotNull( repositoryCache );
      assertTrue( repositoryCache.toAbsolutePath().toString().endsWith( "/cache/repos/v1" ) );
    } );
  }

  @Test
  public void getOutputBase()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path cwd = FileUtil.getCurrentDirectory();
      Files.write( cwd.resolve( "WORKSPACE" ), new byte[ 0 ] );
      final File repositoryCache = BazelUtil.getOutputBase( cwd.toFile() );
      assertNotNull( repositoryCache );
    } );
  }

  @Test
  public void getOutputBase_WORKSPACE_notPresent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final File repositoryCache = BazelUtil.getOutputBase( FileUtil.getCurrentDirectory().toFile() );
      assertNull( repositoryCache );
    } );
  }
}
