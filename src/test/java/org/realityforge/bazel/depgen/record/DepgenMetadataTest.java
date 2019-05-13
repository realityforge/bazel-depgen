package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DepgenMetadataTest
  extends AbstractTest
{
  @Test
  public void getSha256_fileNonExist()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      // Create random file
      final Path artifact = FileUtil.createLocalTempDir().resolve( "file.dat" );
      Files.write( artifact, new byte[]{ 1, 2, 3 } );

      assertFalse( file.toFile().exists() );

      assertEquals( metadata.getSha256( "", artifact.toFile() ),
                    "039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81" );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "<default>.sha256=039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81\n" );
    } );
  }

  @Test
  public void getSha256_fileExistsButNoEntry()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );
      Files.write( file, new byte[ 0 ] );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      // Create random file
      final Path artifact = FileUtil.createLocalTempDir().resolve( "file.dat" );
      Files.write( artifact, new byte[]{ 1, 2, 3 } );

      assertTrue( file.toFile().exists() );

      final String sha256 = metadata.getSha256( "", artifact.toFile() );
      assertEquals( sha256, "039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81" );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "<default>.sha256=039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81\n" );
    } );
  }

  @Test
  public void getSha256_fileExistsContainsEntry()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );
      Files.write( file, "<default>.sha256=ABCD\n".getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path artifact = FileUtil.createLocalTempDir().resolve( "file.dat" );

      // Artifact does not exist so must be getting it from cache
      assertFalse( artifact.toFile().exists() );

      assertTrue( file.toFile().exists() );

      final String sha256 = metadata.getSha256( "", artifact.toFile() );
      assertEquals( sha256, "ABCD" );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "<default>.sha256=ABCD\n" );
    } );
  }

  @Test
  public void getSha256_multipleCallsWIllNotAccessUnderlingCacheIfNotModified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );
      Files.write( file, "<default>.sha256=ABCD\n".getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path artifact = FileUtil.createLocalTempDir().resolve( "file.dat" );

      // Artifact does not exist so must be getting it from cache
      assertFalse( artifact.toFile().exists() );
      assertTrue( file.toFile().exists() );

      assertEquals( metadata.getSha256( "", artifact.toFile() ), "ABCD" );

      // Delete the underlying cache file
      assertTrue( file.toFile().delete() );

      assertFalse( artifact.toFile().exists() );
      assertFalse( file.toFile().exists() );

      // This is accessing property when neither cache nor artifact exists
      assertEquals( metadata.getSha256( "", artifact.toFile() ), "ABCD" );
    } );
  }

  @Test
  public void getSha256_withNonEmptyClassifier()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      // Create random file
      final Path artifact = FileUtil.createLocalTempDir().resolve( "file.dat" );
      Files.write( artifact, new byte[]{ 1, 2, 3 } );

      assertFalse( file.toFile().exists() );

      assertEquals( metadata.getSha256( "sources", artifact.toFile() ),
                    "039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81" );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "sources.sha256=039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81\n" );
    } );
  }

  @Test
  public void getUrls_fileUrls()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path dir1 = FileUtil.createLocalTempDir();
      final Path dir2 = FileUtil.createLocalTempDir();
      final Path dir3 = FileUtil.createLocalTempDir();

      final URI uri = dir1.toUri();

      final RemoteRepository repo1 = new RemoteRepository.Builder( "dir1", "default", uri.toString() ).build();
      final RemoteRepository repo2 = new RemoteRepository.Builder( "dir2", "default", dir2.toUri().toString() ).build();
      final RemoteRepository repo3 = new RemoteRepository.Builder( "dir3", "default", dir3.toUri().toString() ).build();

      deployTempArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir2, "com.example:myapp:1.0" );

      assertFalse( file.toFile().exists() );

      final List<String> urls =
        metadata.getUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                          Arrays.asList( repo1, repo2, repo3 ),
                          Collections.emptyMap() );
      assertEquals( urls.size(), 2 );
      assertTrue( urls.get( 0 ).startsWith( repo1.getUrl() ) );
      assertTrue( urls.get( 1 ).startsWith( repo2.getUrl() ) );
      assertTrue( urls.get( 0 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertTrue( urls.get( 1 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "<default>.dir1.url=" +
                    repo1.getUrl().replaceAll( ":", "\\\\:" ) + "com/example/myapp/1.0/myapp-1.0.jar\n" +
                    "<default>.dir2.url=" +
                    repo2.getUrl().replaceAll( ":", "\\\\:" ) + "com/example/myapp/1.0/myapp-1.0.jar\n" +
                    "<default>.dir3.url=-\n" );
    } );
  }

  @Test
  public void getUrls_alreadyCached()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      Files.write( file, ( "<default>.dir1.url=http\\://a.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                           "<default>.dir2.url=http\\://b.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                           "<default>.dir3.url=-\n" ).getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final RemoteRepository repo1 = new RemoteRepository.Builder( "dir1", "default", "http://a.com" ).build();
      final RemoteRepository repo2 = new RemoteRepository.Builder( "dir2", "default", "http://b.com" ).build();
      final RemoteRepository repo3 = new RemoteRepository.Builder( "dir3", "default", "http://c.com" ).build();

      final List<String> urls =
        metadata.getUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                          Arrays.asList( repo1, repo2, repo3 ),
                          Collections.emptyMap() );
      assertEquals( urls.size(), 2 );
      assertEquals( urls.get( 0 ), "http://a.com/com/example/myapp/1.0/myapp-1.0.jar" );
      assertEquals( urls.get( 1 ), "http://b.com/com/example/myapp/1.0/myapp-1.0.jar" );
    } );
  }

  @Test
  public void getUrls_noArtifactPresent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      Files.write( file, ( "<default>.dir1.url=http\\://a.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                           "<default>.dir2.url=http\\://b.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                           "<default>.dir3.url=-\n" ).getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final RemoteRepository repo1 = new RemoteRepository.Builder( "dir1", "default", "http://a.com" ).build();
      final RemoteRepository repo2 = new RemoteRepository.Builder( "dir2", "default", "http://b.com" ).build();
      final RemoteRepository repo3 = new RemoteRepository.Builder( "dir3", "default", "http://c.com" ).build();

      final List<String> urls =
        metadata.getUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                          Arrays.asList( repo1, repo2, repo3 ),
                          Collections.emptyMap() );
      assertEquals( urls.size(), 2 );
      assertEquals( urls.get( 0 ), "http://a.com/com/example/myapp/1.0/myapp-1.0.jar" );
      assertEquals( urls.get( 1 ), "http://b.com/com/example/myapp/1.0/myapp-1.0.jar" );
    } );
  }

  @Nonnull
  private String loadPropertiesContent( @Nonnull final Path file )
    throws IOException
  {
    return new String( Files.readAllBytes( file ), StandardCharsets.ISO_8859_1 ).replaceAll( "^#[^\n]*\n", "" );
  }
}
