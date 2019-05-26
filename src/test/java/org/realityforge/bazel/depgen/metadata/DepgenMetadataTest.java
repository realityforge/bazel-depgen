package org.realityforge.bazel.depgen.metadata;

import gir.io.FileUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.LicenseType;
import org.realityforge.bazel.depgen.record.LicenseRecord;
import org.testng.Assert;
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
                          Collections.emptyMap(),
                          Assert::fail );
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
                          Collections.emptyMap(),
                          Assert::fail );
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
                          Collections.emptyMap(),
                          Assert::fail );
      assertEquals( urls.size(), 2 );
      assertEquals( urls.get( 0 ), "http://a.com/com/example/myapp/1.0/myapp-1.0.jar" );
      assertEquals( urls.get( 1 ), "http://b.com/com/example/myapp/1.0/myapp-1.0.jar" );
    } );
  }

  @Test
  public void getUrls_cachedButWithBadUrl()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final Path dir1 = FileUtil.createLocalTempDir();

      final URI uri = dir1.toUri();

      // dir1 has a http protocol in cache but our configuration uses file protocol so we should end up
      // searching remote repository and updating cache.
      Files.write( file, ( "<default>.dir1.url=http\\://a.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                           "<default>.dir2.url=http\\://b.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                           "<default>.dir3.url=-\n" ).getBytes( StandardCharsets.ISO_8859_1 ) );

      deployTempArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final RemoteRepository repo1 = new RemoteRepository.Builder( "dir1", "default", uri.toString() ).build();
      final RemoteRepository repo2 = new RemoteRepository.Builder( "dir2", "default", "http://b.com" ).build();
      final RemoteRepository repo3 = new RemoteRepository.Builder( "dir3", "default", "http://c.com" ).build();

      final String expectedWarning =
        "Cache entry '<default>.dir1.url' for artifact 'com.example:myapp:jar:1.0' contains a url " +
        "'http://a.com/com/example/myapp/1.0/myapp-1.0.jar' that does not match the repository url '" +
        repo1.getUrl() + "'. Removing cache entry.";
      final List<String> urls =
        metadata.getUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                          Arrays.asList( repo1, repo2, repo3 ),
                          Collections.emptyMap(),
                          e -> assertEquals( e, expectedWarning ) );
      assertEquals( urls.size(), 2 );
      assertTrue( urls.get( 0 ).startsWith( repo1.getUrl() ) );
      assertTrue( urls.get( 0 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertEquals( urls.get( 1 ), "http://b.com/com/example/myapp/1.0/myapp-1.0.jar" );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "<default>.dir1.url=" +
                    repo1.getUrl().replaceAll( ":", "\\\\:" ) + "com/example/myapp/1.0/myapp-1.0.jar\n" +
                    "<default>.dir2.url=http\\://b.com/com/example/myapp/1.0/myapp-1.0.jar\n" +
                    "<default>.dir3.url=-\n" );
    } );
  }

  @Test
  public void getProcessors_jar_withNoProcessor()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      assertFalse( file.toFile().exists() );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path path = createTempJarFile();
      final List<String> processors = metadata.getProcessors( path.toFile() );
      assertNull( processors );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ), "processors=-\n" );
    } );
  }

  @Test
  public void getProcessors_jarWithSingleProcessor()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path path = createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                                       "react4j.processor.ReactProcessor\n" );
      final List<String> processors = metadata.getProcessors( path.toFile() );
      assertNotNull( processors );
      assertEquals( processors, Collections.singletonList( "react4j.processor.ReactProcessor" ) );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ), "processors=react4j.processor.ReactProcessor\n" );
    } );
  }

  @Test
  public void getProcessors_jarWithMultipleProcessor()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path path = createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                                       "react4j.processor.ReactProcessor\n" +
                                       "arez.processor.ArezProcessor\n" );
      final List<String> processors = metadata.getProcessors( path.toFile() );
      assertNotNull( processors );
      assertEquals( processors, Arrays.asList( "react4j.processor.ReactProcessor", "arez.processor.ArezProcessor" ) );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "processors=react4j.processor.ReactProcessor,arez.processor.ArezProcessor\n" );
    } );
  }

  @Test
  public void getProcessors_cachedProperties_noProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      Files.write( file, "processors=-\n".getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path path = createTempJarFile();
      final List<String> processors = metadata.getProcessors( path.toFile() );
      assertNull( processors );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ), "processors=-\n" );
    } );
  }

  @Test
  public void getProcessors_cachedProperties_multipleProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      Files.write( file,
                   "processors=react4j.processor.ReactProcessor,arez.processor.ArezProcessor\n".getBytes(
                     StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path path = createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                                       "react4j.processor.ReactProcessor\n" +
                                       "arez.processor.ArezProcessor\n" );
      final List<String> processors = metadata.getProcessors( path.toFile() );
      assertNotNull( processors );
      assertEquals( processors, Arrays.asList( "react4j.processor.ReactProcessor", "arez.processor.ArezProcessor" ) );

      assertTrue( file.toFile().exists() );

      assertEquals( loadPropertiesContent( file ),
                    "processors=react4j.processor.ReactProcessor,arez.processor.ArezProcessor\n" );
    } );
  }

  @Test
  public void getLicenses_notCached()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path pomFile = file.getParent().resolve( "file.pom" );
      Files.write( pomFile,
                   ( "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                     "  <modelVersion>4.0.0</modelVersion>\n" +
                     "  <groupId>com.example.myapp</groupId>\n" +
                     "  <artifactId>myapp</artifactId>\n" +
                     "  <version>1.1.0</version>\n" +
                     "  <packaging>jar</packaging>\n" +
                     "  <licenses>\n" +
                     "    <license>\n" +
                     "      <name>The Apache Software License, Version 2.0</name>\n" +
                     "      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>\n" +
                     "      <distribution>repo</distribution>\n" +
                     "    </license>\n" +
                     "  </licenses>\n" +
                     "</project>\n" ).getBytes( StandardCharsets.US_ASCII ) );

      final List<LicenseRecord> licenses = metadata.getLicenses( pomFile );
      assertNotNull( licenses );

      assertEquals( licenses.size(), 1 );
      final LicenseRecord license1 = licenses.get( 0 );
      assertEquals( license1.getType(), LicenseType.notice );
      assertEquals( license1.getName(), "The Apache Software License, Version 2.0" );

      assertEquals( loadPropertiesContent( file ), "licenses=notice\\:The Apache Software License, Version 2.0\n" );
    } );
  }

  @Test
  public void getLicenses_cachedProperties()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final String content =
        "licenses=notice\\:The Apache Software License, Version 2.0|" +
        "restricted\\:GNU Lesser General Public License\n";
      Files.write( file, content.getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path pomFile = file.getParent().resolve( "file.pom" );
      final List<LicenseRecord> licenses = metadata.getLicenses( pomFile );
      assertNotNull( licenses );

      assertEquals( licenses.size(), 2 );
      final LicenseRecord license1 = licenses.get( 0 );
      assertEquals( license1.getType(), LicenseType.notice );
      assertEquals( license1.getName(), "The Apache Software License, Version 2.0" );
      final LicenseRecord license2 = licenses.get( 1 );
      assertEquals( license2.getType(), LicenseType.restricted );
      assertEquals( license2.getName(), "GNU Lesser General Public License" );

      assertEquals( loadPropertiesContent( file ), content );
    } );
  }

  @Test
  public void getLicenses_cachedProperties_nullValue()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path file = FileUtil.createLocalTempDir().resolve( "file.properties" );

      final String content = "licenses=-\n";
      Files.write( file, content.getBytes( StandardCharsets.ISO_8859_1 ) );

      final DepgenMetadata metadata = new DepgenMetadata( file );

      final Path pomFile = file.getParent().resolve( "file.pom" );
      final List<LicenseRecord> licenses = metadata.getLicenses( pomFile );
      assertNull( licenses );

      assertEquals( loadPropertiesContent( file ), content );
    } );
  }
}
