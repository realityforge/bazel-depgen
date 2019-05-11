package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.aether.repository.AuthenticationContext;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationRecordTest
  extends AbstractTest
{
  @Test
  public void build_simple_noDeps()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertNotNull( record.getNode() );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      assertTrue( record.getAuthenticationContexts().isEmpty() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
      assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertNull( artifactRecord.getSourceSha256() );
      assertNull( artifactRecord.getSourceUrls() );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
    } );
  }

  @Test
  public void build_artifact_with_source()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertNotNull( record.getNode() );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      assertTrue( record.getAuthenticationContexts().isEmpty() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
      assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertEquals( artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getSourceUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar" ) );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
    } );
  }

  @Test
  public void build_artifact_with_source_where_localInclude_overrides_global_exclude()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "    includeSource: false\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    includeSource: true\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertNotNull( record.getNode() );

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      assertTrue( record.getAuthenticationContexts().isEmpty() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getSourceUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar" ) );
    } );
  }

  @Test
  public void build_artifact_with_source_but_global_includeSourceFalse()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "    includeSource: false\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertNull( artifactRecord.getSourceSha256() );
      assertNull( artifactRecord.getSourceUrls() );
    } );
  }

  @Test
  public void build_artifact_with_source_but_local_includeSourceFalse()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    includeSource: false\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertNull( artifactRecord.getSourceSha256() );
      assertNull( artifactRecord.getSourceUrls() );
    } );
  }

  @Test
  public void build_namePrefixPresent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  namePrefix: myapp\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getName(), "myapp_com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias(), "myapp_com_example__myapp" );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
    } );
  }

  @Test
  public void build_namePrefixPresent_with_trailing_underscore()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  namePrefix: myapp_\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getName(), "myapp_com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias(), "myapp_com_example__myapp" );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
    } );
  }

  @Test
  public void getAuthenticationContexts()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      final String settingsContent =
        "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
        "  <servers>\n" +
        "    <server>\n" +
        "      <id>my-repo</id>\n" +
        "      <username>root</username>\n" +
        "      <password>secret</password>\n" +
        "    </server>\n" +
        "  </servers>\n" +
        "</settings>\n";
      Files.write( settingsFile, settingsContent.getBytes( StandardCharsets.UTF_8 ) );

      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "repositories:\n" +
                         "  central: http://repo1.maven.org/maven2\n" +
                         "  my-repo: http://my-repo.example.com/maven2\n" );
      final ApplicationRecord record = loadApplicationRecord();

      final Map<String, AuthenticationContext> contexts = record.getAuthenticationContexts();
      assertEquals( contexts.size(), 1 );
      final AuthenticationContext context = contexts.get( "my-repo" );
      assertNotNull( context );
      assertEquals( context.get( AuthenticationContext.USERNAME ), "root" );
      assertEquals( context.get( AuthenticationContext.PASSWORD ), "secret" );
    } );
  }

  @Test
  public void build_manyDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtB:jar::2.0:runtime",
                                           "org.test4j:core:jar::44.0:test" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtB:2.0",
                                           // Provided ignored by traversal
                                           "com.example:container:jar::4.0:provided",
                                           // System collected but ignored at later stage
                                           "com.example:kernel:jar::4.0:system" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      assertTrue( record.getAuthenticationContexts().isEmpty() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 4 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp," +
                    "com.example:mylib," +
                    "com.example:rtA," +
                    "com.example:rtB" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getName(), "com_example__mylib__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__mylib" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtB" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getName(), "com_example__rta__33_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__rta" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtA:33.0" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtB" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtB" );
        assertEquals( artifactRecord.getName(), "com_example__rtb__2_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__rtb" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtB:2.0" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_singleDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertEquals( artifactRecord.getSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
        assertNull( artifactRecord.getSourceSha256() );
        assertNull( artifactRecord.getSourceUrls() );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getName(), "com_example__mylib__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__mylib" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0" );
        assertEquals( artifactRecord.getSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/mylib/1.0/mylib-1.0.jar" ) );
        assertNull( artifactRecord.getSourceSha256() );
        assertNull( artifactRecord.getSourceUrls() );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_singleDependency_with_Sources()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertEquals( artifactRecord.getSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
        assertEquals( artifactRecord.getSourceSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getSourceUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar" ) );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getName(), "com_example__mylib__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__mylib" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0" );
        assertEquals( artifactRecord.getSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/mylib/1.0/mylib-1.0.jar" ) );
        assertEquals( artifactRecord.getSourceSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getSourceUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/mylib/1.0/mylib-1.0-sources.jar" ) );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void multipleDependenciesWithSameKeyOmitsSecond()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:jar:sources:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );

      final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
      final List<ArtifactRecord> deps = artifactRecord.getDeps();
      assertEquals( deps.size(), 1 );
      assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib:jar:1.0" );
    } );
  }

  @Test
  public void multipleDependenciesWithSameKeyOmitsSecondIfBothHaveClassifiers()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:jar:stripped:1.0",
                                           "com.example:mylib:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:stripped:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );

      final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
      final List<ArtifactRecord> deps = artifactRecord.getDeps();
      assertEquals( deps.size(), 1 );
      assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib:jar:stripped:1.0" );
    } );
  }

  @Test
  public void build_singleRuntimeDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:jar:33.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:rtA" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getName(), "com_example__rta__33_0" );
        assertEquals( artifactRecord.getAlias(), "com_example__rta" );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtA:33.0" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_versionlessDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "  - coord: com.example:mylib\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_conflicts()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::33.0:runtime" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:mylib:1.0",
                                           "com.example:rtA:jar::32.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:32.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:rtA:33.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib,com.example:rtA" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
        assertEquals( artifactRecord.getNode().getDependency().getArtifact().getVersion(), "33.0" );
      }
    } );
  }

  @Test
  public void build_replacement()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "replacements:\n" +
                         "  - coord: com.example:mylib\n" +
                         "    target: \"@com_example//:mylib\"\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertNull( artifactRecord.getReplacementModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertNotNull( artifactRecord.getDeps().get( 0 ).getReplacementModel() );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertNotNull( artifactRecord.getReplacementModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_singleOptionalDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    includeOptional: true\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:jar::1.0:compile:optional" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp,com.example:mylib" );

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
        assertNotNull( artifactRecord );
        assertNotNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void findArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ArtifactRecord artifact1 = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifact1 );
      assertEquals( artifact1.getKey(), "com.example:myapp" );

      assertNull( record.findArtifact( "com.example", "other-no-exist" ) );
    } );
  }

  @Test
  public void getArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ArtifactRecord artifact1 = record.getArtifact( "com.example", "myapp" );
      assertNotNull( artifact1 );
      assertEquals( artifact1.getKey(), "com.example:myapp" );

      assertThrows( NullPointerException.class, () -> record.getArtifact( "com.example", "other-no-exist" ) );
    } );
  }
}
