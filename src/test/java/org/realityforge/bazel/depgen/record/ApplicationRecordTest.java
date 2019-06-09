package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.aether.repository.AuthenticationContext;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.util.StarlarkOutput;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationRecordTest
  extends AbstractTest
{
  @Test
  public void getPathFromExtensionToConfig()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      assertEquals( record.getPathFromExtensionToConfig(), Paths.get( "../dependencies.yml" ) );
    } );
  }

  @Test
  public void getPathFromExtensionToConfig_nonStandardExtensionFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  extensionFile: workspaceDir/vendor/workspace.bzl\n" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      assertEquals( record.getPathFromExtensionToConfig(), Paths.get( "../../dependencies.yml" ) );
    } );
  }

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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__myapp" );
      assertTrue( artifactRecord.generatesApi() );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
      assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertNull( artifactRecord.getSourceSha256() );
      assertNull( artifactRecord.getSourceUrls() );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getReverseDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 0 );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__myapp" );
      assertTrue( artifactRecord.generatesApi() );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
      assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertEquals( artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
      assertEquals( artifactRecord.getSourceUrls(),
                    Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar" ) );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getReverseDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 0 );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "myapp_com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias( Nature.Java ), "myapp_com_example__myapp" );
      assertTrue( artifactRecord.generatesApi() );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "myapp_com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias( Nature.Java ), "myapp_com_example__myapp" );
      assertTrue( artifactRecord.generatesApi() );
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
                         "  - name: my-repo\n" +
                         "    url: http://my-repo.example.com/maven2\n" );
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
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__myapp" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertNull( artifactRecord.getProcessors() );
        assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getReverseDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__mylib__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__mylib" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0" );
        assertNull( artifactRecord.getProcessors() );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getReverseDeps().size(), 1 );
        assertEquals( artifactRecord.getReverseDeps().get( 0 ).getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getRuntimeDeps().get( 0 ).getKey(), "com.example:rtB" );
        assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 0 );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtA" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtA" );
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__rta__33_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__rta" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtA:33.0" );
        assertNull( artifactRecord.getProcessors() );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getReverseDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
        assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getReverseRuntimeDeps().get( 0 ).getKey(), "com.example:myapp" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "rtB" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:rtB" );
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__rtb__2_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__rtb" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtB:2.0" );
        assertNull( artifactRecord.getProcessors() );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getReverseDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
        assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 1 );
        assertEquals( artifactRecord.getReverseRuntimeDeps().get( 0 ).getKey(), "com.example:mylib" );
      }
    } );
  }

  @Test
  public void build_singleDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();
      final String url = dir.toUri().toString();
      final String urlEncoded = url.replaceAll( ":", "\\\\:" );

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
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__myapp" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertNull( artifactRecord.getProcessors() );
        assertEquals( artifactRecord.getSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
        assertNull( artifactRecord.getSourceSha256() );
        assertNull( artifactRecord.getSourceUrls() );
        assertEquals( artifactRecord.getDeps().size(), 1 );
        assertEquals( artifactRecord.getDeps().get( 0 ).getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
        final Path path =
          artifactRecord.getArtifact().getFile().getParentFile().toPath().resolve( DepgenMetadata.FILENAME );
        assertEquals( loadPropertiesContent( path ),
                      "<default>.local.url=" + urlEncoded + "com/example/myapp/1.0/myapp-1.0.jar\n" +
                      "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" +
                      "processors=-\n" +
                      "sources.present=false\n" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__mylib__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__mylib" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0" );
        assertNull( artifactRecord.getProcessors() );
        assertEquals( artifactRecord.getSha256(),
                      "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
        assertEquals( artifactRecord.getUrls(),
                      Collections.singletonList( dir.toUri() + "com/example/mylib/1.0/mylib-1.0.jar" ) );
        assertNull( artifactRecord.getSourceSha256() );
        assertNull( artifactRecord.getSourceUrls() );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
        final Path path =
          artifactRecord.getArtifact().getFile().getParentFile().toPath().resolve( DepgenMetadata.FILENAME );
        assertEquals( loadPropertiesContent( path ),
                      "<default>.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0.jar\n" +
                      "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" +
                      "processors=-\n" +
                      "sources.present=false\n" );
      }
    } );
  }

  @Test
  public void build_singleDependency_with_Sources()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();
      final String url = dir.toUri().toString();
      final String urlEncoded = url.replaceAll( ":", "\\\\:" );

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final Path cacheDir = FileUtil.createLocalTempDir();
      final ApplicationRecord record = loadApplicationRecord( cacheDir );

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
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__myapp" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
        assertNull( artifactRecord.getProcessors() );
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
        final Path path =
          artifactRecord.getArtifact().getFile().getParentFile().toPath().resolve( DepgenMetadata.FILENAME );
        assertEquals( loadPropertiesContent( path ),
                      "<default>.local.url=" + urlEncoded + "com/example/myapp/1.0/myapp-1.0.jar\n" +
                      "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" +
                      "processors=-\n" +
                      "sources.local.url=" + urlEncoded + "com/example/myapp/1.0/myapp-1.0-sources.jar\n" +
                      "sources.present=true\n" +
                      "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" );
      }

      {
        final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "mylib" );
        assertNotNull( artifactRecord );
        assertNull( artifactRecord.getArtifactModel() );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__mylib__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__mylib" );
        assertTrue( artifactRecord.generatesApi() );
        assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0" );
        assertNull( artifactRecord.getProcessors() );
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
        final Path path =
          artifactRecord.getArtifact().getFile().getParentFile().toPath().resolve( DepgenMetadata.FILENAME );
        assertEquals( loadPropertiesContent( path ),
                      "<default>.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0.jar\n" +
                      "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" +
                      "processors=-\n" +
                      "sources.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0-sources.jar\n" +
                      "sources.present=true\n" +
                      "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" );
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
  public void multipleDependenciesWithSameKeyInRuntimeDeps()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:jar:sources:1.0:runtime",
                                           "com.example:mylib:jar::1.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );

      final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
      final List<ArtifactRecord> deps = artifactRecord.getRuntimeDeps();
      assertEquals( deps.size(), 1 );
      assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib:jar:1.0" );
    } );
  }

  @Test
  public void propagateNature_J2cl()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );

      assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
    } );
  }

  @Test
  public void propagateNature_J2cl_viaDefaultNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  defaultNature: J2cl\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );

      assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
    } );
  }

  @Test
  public void propagateNature_J2cl_declaredTransitivelyPresent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl]\n" +
                              "  - coord: com.example:base:1.0\n" +
                              "    natures: [J2cl]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );

      assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
    } );
  }

  @Test
  public void propagateNature_J2cl_replacementPresent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl]\n" +
                              "  - coord: com.example:base:1.0\n" +
                              "replacements:\n" +
                              "  - coord: com.example:mylib\n" +
                              "    targets:\n" +
                              "      - target: \"@com_example//:mylib\"\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );

      assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                    Collections.singletonList( Nature.J2cl ) );
      assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                    Collections.singletonList( Nature.Java ) );
    } );
  }

  @Test
  public void propagateNature_J2cl_transitiveNonJ2clDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl]\n" +
                              "  - coord: com.example:base:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final IllegalStateException exception = expectThrows( IllegalStateException.class, this::loadApplicationRecord );

      assertEquals( exception.getMessage(),
                    "Artifact 'com.example:base:jar:1.0' does not specify the J2cl nature but is a transitive dependency of 'com.example:myapp:jar:1.0' which has the J2cl nature. This is not a supported scenario." );
    } );
  }

  @Test
  public void propagateNature_J2cl_directNonJ2clDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl]\n" +
                              "  - coord: com.example:mylib:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final IllegalStateException exception = expectThrows( IllegalStateException.class, this::loadApplicationRecord );

      assertEquals( exception.getMessage(),
                    "Artifact 'com.example:mylib:jar:1.0' does not specify the J2cl nature but is a direct dependency of 'com.example:myapp:jar:1.0' which has the J2cl nature. This is not a supported scenario." );
    } );
  }

  @Test
  public void propagateNature_Java()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  defaultNature: J2cl\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [Java]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );

      assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                    Collections.singletonList( Nature.Java ) );
      assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                    Collections.singletonList( Nature.Java ) );
      assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                    Collections.singletonList( Nature.Java ) );
    } );
  }

  @Test
  public void propagateNature_Java_directNonJavaDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  defaultNature: J2cl\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [Java]\n" +
                              "  - coord: com.example:mylib:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final IllegalStateException exception = expectThrows( IllegalStateException.class, this::loadApplicationRecord );

      assertEquals( exception.getMessage(),
                    "Artifact 'com.example:mylib:jar:1.0' does not specify the Java nature but is a direct dependency of 'com.example:myapp:jar:1.0' which has the Java nature. This is not a supported scenario." );
    } );
  }

  @Test
  public void propagateNature_Plugin()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  defaultNature: J2cl\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [Plugin]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 3 );

      assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                    Collections.singletonList( Nature.Plugin ) );
      assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                    Collections.singletonList( Nature.Java ) );
      assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                    Collections.singletonList( Nature.Java ) );
    } );
  }

  @Test
  public void propagateNature_Plugin_directNonJavaDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  defaultNature: J2cl\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [Plugin]\n" +
                              "  - coord: com.example:mylib:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:base:1.0" );

      final IllegalStateException exception = expectThrows( IllegalStateException.class, this::loadApplicationRecord );

      assertEquals( exception.getMessage(),
                    "Artifact 'com.example:mylib:jar:1.0' does not specify the Java nature but is a direct dependency of 'com.example:myapp:jar:1.0' which has the Plugin nature. This is not a supported scenario." );
    } );
  }

  @Test
  public void depsAreSorted()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib1:1.0",
                                           "com.example:mylib3:1.0",
                                           "com.example:mylib2:1.0",
                                           "com.example:mylib4:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib1:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib2:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib3:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib4:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 5 );

      final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
      final List<ArtifactRecord> deps = artifactRecord.getDeps();
      assertEquals( deps.size(), 4 );
      assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib1:jar:1.0" );
      assertEquals( deps.get( 1 ).getArtifact().toString(), "com.example:mylib2:jar:1.0" );
      assertEquals( deps.get( 2 ).getArtifact().toString(), "com.example:mylib3:jar:1.0" );
      assertEquals( deps.get( 3 ).getArtifact().toString(), "com.example:mylib4:jar:1.0" );
    } );
  }

  @Test
  public void runtimeDepsAreSorted()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib1:jar::1.0:runtime",
                                           "com.example:mylib3:jar::1.0:runtime",
                                           "com.example:mylib2:jar::1.0:runtime",
                                           "com.example:mylib4:jar::1.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib1:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib2:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib3:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib4:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 5 );

      final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
      final List<ArtifactRecord> deps = artifactRecord.getRuntimeDeps();
      assertEquals( deps.size(), 4 );
      assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib1:jar:1.0" );
      assertEquals( deps.get( 1 ).getArtifact().toString(), "com.example:mylib2:jar:1.0" );
      assertEquals( deps.get( 2 ).getArtifact().toString(), "com.example:mylib3:jar:1.0" );
      assertEquals( deps.get( 3 ).getArtifact().toString(), "com.example:mylib4:jar:1.0" );
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
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__myapp" );
        assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
        assertTrue( artifactRecord.generatesApi() );
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
        assertEquals( artifactRecord.getName( Nature.Java ), "com_example__rta__33_0" );
        assertEquals( artifactRecord.getAlias( Nature.Java ), "com_example__rta" );
        assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
        assertTrue( artifactRecord.generatesApi() );
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
                         "    targets:\n" +
                         "      - target: \"@com_example//:mylib\"\n" );
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
        assertEquals( artifactRecord.getLabel( Nature.Java ), "com_example__myapp" );
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
        assertEquals( artifactRecord.getLabel( Nature.Java ), "@com_example//:mylib" );
        assertEquals( artifactRecord.getDeps().size(), 0 );
        assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      }
    } );
  }

  @Test
  public void build_exclude()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "excludes:\n" +
                         "  - coord: com.example:mylib\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp" );

      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
    } );
  }

  @Test
  public void build_whereRuntimeDependencyExcluded()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "excludes:\n" +
                         "  - coord: com.example:mylib\n" );
      deployTempArtifactToLocalRepository( dir,
                                           "com.example:myapp:1.0",
                                           "com.example:mylib:jar::1.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      assertEquals( record.getSource().getConfigLocation(),
                    FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ).toAbsolutePath().normalize() );
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example:myapp" );

      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
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

  @Test
  public void getNature_withDefaultJavaNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
    } );
  }

  @Test
  public void getNature_withDefaultPluginNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "react4j.processor.ReactProcessor\n" +
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
      assertTrue( artifactRecord.generatesApi() );
    } );
  }

  @Test
  public void getNature_withExplicitNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    natures: [Plugin]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
      assertTrue( artifactRecord.generatesApi() );
    } );
  }

  @Test
  public void getNature_of_transitiveDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "react4j.processor.ReactProcessor\n" +
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0", jarFile );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      {
        final ArtifactRecord artifactRecord = artifacts.get( 0 );
        assertEquals( artifactRecord.getKey(), "com.example:myapp" );
        assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
        assertTrue( artifactRecord.generatesApi() );
      }
      {
        final ArtifactRecord artifactRecord = artifacts.get( 1 );
        assertEquals( artifactRecord.getKey(), "com.example:mylib" );
        assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
        assertTrue( artifactRecord.generatesApi() );
      }
    } );
  }

  @Test
  public void defaultGeneratesApi()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "react4j.processor.ReactProcessor\n" +
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
      assertTrue( artifactRecord.generatesApi() );
    } );
  }

  @Test
  public void explicitTrueGeneratesApi()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    plugin:\n" +
                         "      generatesApi: true\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "react4j.processor.ReactProcessor\n" +
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
      assertTrue( artifactRecord.generatesApi() );
    } );
  }

  @Test
  public void explicitGeneratesApiForNonPlugin()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    plugin:\n" +
                         "      generatesApi: false\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final IllegalStateException exception =
        expectThrows( IllegalStateException.class, this::loadApplicationRecord );
      assertEquals( exception.getMessage(),
                    "Artifact 'com.example:myapp:jar:1.0' has specified 'plugin' configuration but does not specify the Plugin nature nor does it contain any annotation processors." );
    } );
  }

  @Test
  public void explicitGeneratesApiWhereNoProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    natures: [Plugin]\n" +
                         "    plugin:\n" +
                         "      generatesApi: false\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final IllegalStateException exception =
        expectThrows( IllegalStateException.class, this::loadApplicationRecord );
      assertEquals( exception.getMessage(),
                    "Artifact 'com.example:myapp:jar:1.0' has specified 'plugin' configuration but does not specify the Plugin nature nor does it contain any annotation processors." );
    } );
  }

  @Test
  public void explicitFalseGeneratesApi()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    plugin:\n" +
                         "      generatesApi: false\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "react4j.processor.ReactProcessor\n" +
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
      assertFalse( artifactRecord.generatesApi() );
    } );
  }

  @Test
  public void getAlias_withAliasStrategy()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  aliasStrategy: ArtifactId\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias( Nature.Java ), "myapp" );
    } );
  }

  @Test
  public void getAlias_withAliasStrategyAndPrefix()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  namePrefix: gwt_\n" +
                         "  aliasStrategy: ArtifactId\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "gwt_com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias( Nature.Java ), "gwt_myapp" );
    } );
  }

  @Test
  public void loadWhereDuplicateAliasesExist()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  aliasStrategy: ArtifactId\n" +
                         "artifacts:\n" +
                         "  - coord: com.example.app1:core:42.0\n" +
                         "  - coord: com.example.app2:core:37.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
      deployTempArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

      final IllegalStateException exception = expectThrows( IllegalStateException.class, this::loadApplicationRecord );
      assertEquals( exception.getMessage(),
                    "Multiple artifacts have the same alias 'core' which is not supported. Change the aliasStrategy option globally or for one of the artifacts 'com.example.app1:core:jar:42.0' and 'com.example.app2:core:jar:37.0'." );
    } );
  }

  @Test
  public void loadWhereDuplicateAliasesWorkedAroundViaExplicitAlias()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  aliasStrategy: ArtifactId\n" +
                         "artifacts:\n" +
                         "  - coord: com.example.app1:core:42.0\n" +
                         "    aliasStrategy: GroupIdAndArtifactId\n" +
                         "  - coord: com.example.app2:core:37.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
      deployTempArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example.app1:core,com.example.app2:core" );

      final ArtifactRecord artifactRecord1 = artifacts.get( 0 );
      assertEquals( artifactRecord1.getKey(), "com.example.app1:core" );
      assertEquals( artifactRecord1.getName( Nature.Java ), "com_example_app1__core__42_0" );
      assertEquals( artifactRecord1.getAlias( Nature.Java ), "com_example_app1__core" );

      final ArtifactRecord artifactRecord2 = artifacts.get( 1 );
      assertEquals( artifactRecord2.getKey(), "com.example.app2:core" );
      assertEquals( artifactRecord2.getName( Nature.Java ), "com_example_app2__core__37_0" );
      assertEquals( artifactRecord2.getAlias( Nature.Java ), "core" );
    } );
  }

  @Test
  public void shouldExportDeps_perArtifactConfig()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example.app1:core:42.0\n" +
                         "    exportDeps: true\n" +
                         "  - coord: com.example.app2:core:37.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
      deployTempArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example.app1:core,com.example.app2:core" );

      final ArtifactRecord artifactRecord1 = artifacts.get( 0 );
      assertEquals( artifactRecord1.getKey(), "com.example.app1:core" );
      assertTrue( artifactRecord1.shouldExportDeps() );

      final ArtifactRecord artifactRecord2 = artifacts.get( 1 );
      assertEquals( artifactRecord2.getKey(), "com.example.app2:core" );
      assertFalse( artifactRecord2.shouldExportDeps() );
    } );
  }

  @Test
  public void shouldExportDeps_perArtifactConfig_combinedWithGlobalConfig()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  exportDeps: true\n" +
                         "artifacts:\n" +
                         "  - coord: com.example.app1:core:42.0\n" +
                         "    exportDeps: false\n" +
                         "  - coord: com.example.app2:core:37.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
      deployTempArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 2 );
      assertEquals( artifacts.stream().map( ArtifactRecord::getKey ).collect( Collectors.joining( "," ) ),
                    "com.example.app1:core,com.example.app2:core" );

      final ArtifactRecord artifactRecord1 = artifacts.get( 0 );
      assertEquals( artifactRecord1.getKey(), "com.example.app1:core" );
      assertFalse( artifactRecord1.shouldExportDeps() );

      final ArtifactRecord artifactRecord2 = artifacts.get( 1 );
      assertEquals( artifactRecord2.getKey(), "com.example.app2:core" );
      assertTrue( artifactRecord2.shouldExportDeps() );
    } );
  }

  @Test
  public void parseWhereArtifactContainsProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "react4j.processor.ReactProcessor\n" +
                       "arez.processor.ArezProcessor\n" );

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ApplicationRecord record = loadApplicationRecord();

      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getProcessors(),
                    Arrays.asList( "react4j.processor.ReactProcessor", "arez.processor.ArezProcessor" ) );
    } );
  }

  @Test
  public void artifactWithDefaultNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
    } );
  }

  @Test
  public void artifactWithSpecifiedDefaultNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  defaultNature: J2cl\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.J2cl ) );
    } );
  }

  @Test
  public void artifactWithSpecifiedNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    natures: [J2cl]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      assertEquals( artifacts.size(), 1 );
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.J2cl ) );
    } );
  }

  @Test
  public void writeTargetMacro()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeTargetMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeTargetMacro_omitEnabled()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  supportDependencyOmit: true\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeTargetMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_targets(omit_com_example__myapp = False):\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    if not omit_com_example__myapp:\n" +
                    "        native.alias(\n" +
                    "            name = \"com_example__myapp\",\n" +
                    "            actual = \":com_example__myapp__1_0\",\n" +
                    "        )\n" +
                    "        native.java_import(\n" +
                    "            name = \"com_example__myapp__1_0\",\n" +
                    "            jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "            tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "            visibility = [\"//visibility:private\"],\n" +
                    "        )\n" );
    } );
  }

  @Test
  public void writeTargetMacro_macroNameOverride()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  targetMacroName: generate_myapp_targets\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeTargetMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_myapp_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeTargetMacro_dependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeTargetMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "        deps = [\":com_example__mylib\"],\n" +
                    "    )\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__mylib\",\n" +
                    "        actual = \":com_example__mylib__2_0\",\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__mylib__2_0\",\n" +
                    "        jars = [\"@com_example__mylib__2_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:mylib:2.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeTargetMacro_replacement()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "replacements:\n" +
                              "  - coord: com.example:mylib\n" +
                              "    targets:\n" +
                              "      - target: \"@com_example//:mylib\"\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeTargetMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "        deps = [\":@com_example//:mylib\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeWorkspaceMacro()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeWorkspaceMacro_omitEnabled()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  supportDependencyOmit: true\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_workspace_rules(omit_com_example__myapp = False):\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    if not omit_com_example__myapp:\n" +
                    "        http_file(\n" +
                    "            name = \"com_example__myapp__1_0\",\n" +
                    "            downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "            sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "            urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "        )\n" );
    } );
  }

  @Test
  public void writeWorkspaceMacro_macroNameOverride()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  workspaceMacroName: generate_myapp_workspace_rules\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_myapp_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeWorkspaceMacro_dependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );
      final List<String> urls2 = record.getArtifacts().get( 1 ).getUrls();
      assertNotNull( urls2 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "    )\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__mylib__2_0\",\n" +
                    "        downloaded_file_path = \"com/example/mylib/2.0/mylib-2.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls2.get( 0 ) + "\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeWorkspaceMacro_replacement()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "replacements:\n" +
                              "  - coord: com.example:mylib\n" +
                              "    targets:\n" +
                              "      - target: \"@com_example//:mylib\"\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeDependencyGraphIfRequired()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeDependencyGraphIfRequired( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "# Dependency Graph Generated from the input data\n" +
                    "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                    "#    \\- com.example:mylib:jar:2.0 [compile]\n" +
                    "\n" );
    } );
  }

  @Test
  public void writeDependencyGraphIfRequired_disabledInConfig()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "options:\n" +
                              "  emitDependencyGraph: false\n" +
                              "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeDependencyGraphIfRequired( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ), "" );
    } );
  }

  @Test
  public void writeBazelExtension()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeBazelExtension( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "# DO NOT EDIT: File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\n" +
                    "\n" +
                    "\"\"\"\n" +
                    "    Macro rules to load dependencies defined in '../dependencies.yml'.\n" +
                    "\n" +
                    "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                    "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                    "\"\"\"\n" +
                    "# Dependency Graph Generated from the input data\n" +
                    "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                    "\n" +
                    "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n" +
                    "\n" +
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "    )\n" +
                    "\n" +
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeDefaultBuild()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeDefaultBuild( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "# File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\n" +
                    "# Contents can be edited and will not be overridden.\n" +
                    "package(default_visibility = [\"//visibility:public\"])\n" +
                    "\n" +
                    "load(\"//thirdparty:dependencies.bzl\", \"generate_targets\")\n" +
                    "\n" +
                    "generate_targets()\n" );
    } );
  }

  @Test
  public void writeBazelExtension_j2clArtifactPresent()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl, Java]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<String> urls = record.getArtifacts().get( 0 ).getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeBazelExtension( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "# DO NOT EDIT: File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\n" +
                    "\n" +
                    "\"\"\"\n" +
                    "    Macro rules to load dependencies defined in '../dependencies.yml'.\n" +
                    "\n" +
                    "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                    "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                    "\"\"\"\n" +
                    "# Dependency Graph Generated from the input data\n" +
                    "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                    "\n" +
                    "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n" +
                    "load(\"@com_google_j2cl//build_defs:rules.bzl\", \"j2cl_library\")\n" +
                    "\n" +
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    "    )\n" +
                    "\n" +
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp-j2cl\",\n" +
                    "        actual = \":com_example__myapp__1_0-j2cl\",\n" +
                    "    )\n" +
                    "    j2cl_library(\n" +
                    "        name = \"com_example__myapp__1_0-j2cl\",\n" +
                    "        srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp\",\n" +
                    "        actual = \":com_example__myapp__1_0\",\n" +
                    "    )\n" +
                    "    native.java_import(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }

  @Test
  public void writeBazelExtension_j2cl_withDependencies()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    natures: [J2cl]\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ApplicationRecord record = loadApplicationRecord();
      final List<ArtifactRecord> artifacts = record.getArtifacts();
      final List<String> urls1 = artifacts.get( 0 ).getUrls();
      assertNotNull( urls1 );
      final List<String> urls2 = artifacts.get( 1 ).getUrls();
      assertNotNull( urls2 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      record.writeBazelExtension( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "# DO NOT EDIT: File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\n" +
                    "\n" +
                    "\"\"\"\n" +
                    "    Macro rules to load dependencies defined in '../dependencies.yml'.\n" +
                    "\n" +
                    "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                    "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                    "\"\"\"\n" +
                    "# Dependency Graph Generated from the input data\n" +
                    "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                    "#    \\- com.example:mylib:jar:1.0 [compile]\n" +
                    "\n" +
                    "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n" +
                    "load(\"@com_google_j2cl//build_defs:rules.bzl\", \"j2cl_library\")\n" +
                    "\n" +
                    "def generate_workspace_rules():\n" +
                    "    \"\"\"\n" +
                    "        Repository rules macro to load dependencies specified by '../dependencies.yml'.\n" +
                    "\n" +
                    "        Must be run from a WORKSPACE file.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__myapp__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls1.get( 0 ) + "\"],\n" +
                    "    )\n" +
                    "\n" +
                    "    http_file(\n" +
                    "        name = \"com_example__mylib__1_0\",\n" +
                    "        downloaded_file_path = \"com/example/mylib/1.0/mylib-1.0.jar\",\n" +
                    "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "        urls = [\"" + urls2.get( 0 ) + "\"],\n" +
                    "    )\n" +
                    "\n" +
                    "def generate_targets():\n" +
                    "    \"\"\"\n" +
                    "        Macro to define targets for dependencies specified by '../dependencies.yml'.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__myapp-j2cl\",\n" +
                    "        actual = \":com_example__myapp__1_0-j2cl\",\n" +
                    "    )\n" +
                    "    j2cl_library(\n" +
                    "        name = \"com_example__myapp__1_0-j2cl\",\n" +
                    "        srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "        deps = [\":com_example__mylib-j2cl\"],\n" +
                    "    )\n" +
                    "\n" +
                    "    native.alias(\n" +
                    "        name = \"com_example__mylib-j2cl\",\n" +
                    "        actual = \":com_example__mylib__1_0-j2cl\",\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" +
                    "    j2cl_library(\n" +
                    "        name = \"com_example__mylib__1_0-j2cl\",\n" +
                    "        srcs = [\"@com_example__mylib__1_0__sources//file\"],\n" +
                    "        visibility = [\"//visibility:private\"],\n" +
                    "    )\n" );
    } );
  }
}
