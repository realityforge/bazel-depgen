package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.repository.AuthenticationContext;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.DepGenConfig;
import org.realityforge.bazel.depgen.DepgenValidationException;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;
import org.realityforge.bazel.depgen.model.ReplacementTargetModel;
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
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertEquals( record.getPathFromExtensionToConfig(), Paths.get( ApplicationConfig.FILENAME ) );
  }

  @Test
  public void getPathFromExtensionToConfig_nonStandardExtensionFile()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  extensionFile: workspaceDir/vendor/workspace.bzl\n" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertEquals( record.getPathFromExtensionToConfig(), Paths.get( "../../" + ApplicationConfig.FILENAME ) );
  }

  @Test
  public void build_simple_noDeps()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNotNull( record.getNode() );

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertTrue( record.getAuthenticationContexts().isEmpty() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertNotNull( artifactRecord.getArtifactModel() );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
    assertTrue( artifactRecord.generatesApi() );
    assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
    assertEquals( artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
    assertEquals( artifactRecord.getUrls(),
                  Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
    assertEquals( artifactRecord.getDeps().size(), 0 );
    assertEquals( artifactRecord.getReverseDeps().size(), 0 );
    assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
    assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 0 );
  }

  @Test
  public void build_artifact_with_annotations()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  includeExternalAnnotations: true\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:annotations:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertTrue( record.getSource().getOptions().includeExternalAnnotations() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getExternalAnnotationSha256(),
                  "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
    assertEquals( artifactRecord.getExternalAnnotationUrls(),
                  Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0-annotations.jar" ) );
  }

  @Test
  public void build_artifact_without_annotations_due_to_defaults()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:annotations:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertFalse( record.getSource().getOptions().includeExternalAnnotations() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertNull( artifactRecord.getExternalAnnotationSha256() );
    assertNull( artifactRecord.getExternalAnnotationUrls() );
  }

  @Test
  public void build_artifact_without_annotations_due_to_GlobalOverride()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  includeExternalAnnotations: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:annotations:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertFalse( record.getSource().getOptions().includeExternalAnnotations() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertNull( artifactRecord.getExternalAnnotationSha256() );
    assertNull( artifactRecord.getExternalAnnotationUrls() );
  }

  @Test
  public void build_artifact_without_annotations_due_to_ArtifactOverride()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  includeExternalAnnotations: true\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    includeExternalAnnotations: false\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:annotations:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertTrue( record.getSource().getOptions().includeExternalAnnotations() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertNull( artifactRecord.getExternalAnnotationSha256() );
    assertNull( artifactRecord.getExternalAnnotationUrls() );
  }

  @Test
  public void build_artifact_without_annotations_due_to_Missing()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  includeExternalAnnotations: true\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertTrue( record.getSource().getOptions().includeExternalAnnotations() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertNull( artifactRecord.getExternalAnnotationSha256() );
    assertNull( artifactRecord.getExternalAnnotationUrls() );
  }

  @Test
  public void build_artifact_with_source()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNotNull( record.getNode() );

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertTrue( record.getAuthenticationContexts().isEmpty() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertNotNull( artifactRecord.getArtifactModel() );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
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
  }

  @Test
  public void build_artifact_with_source_where_localInclude_overrides_global_exclude()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "    includeSource: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    includeSource: true\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNotNull( record.getNode() );

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertTrue( record.getAuthenticationContexts().isEmpty() );
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getSourceSha256(),
                  "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4" );
    assertEquals( artifactRecord.getSourceUrls(),
                  Collections.singletonList( dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar" ) );
  }

  @Test
  public void build_artifact_with_source_but_global_includeSourceFalse()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "    includeSource: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 2 );
    final ArtifactRecord artifactRecord1 = artifacts.get( 0 );
    assertNull( artifactRecord1.getSourceSha256() );
    assertNull( artifactRecord1.getSourceUrls() );
    final ArtifactRecord artifactRecord2 = artifacts.get( 1 );
    assertNull( artifactRecord2.getSourceSha256() );
    assertNull( artifactRecord2.getSourceUrls() );
  }

  @Test
  public void build_artifact_with_source_but_local_includeSourceFalse()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    includeSource: false\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertNull( artifactRecord.getSourceSha256() );
    assertNull( artifactRecord.getSourceUrls() );
  }

  @Test
  public void build_namePrefixPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  namePrefix: myapp\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getName( Nature.Java ), "myapp_com_example__myapp" );
    assertTrue( artifactRecord.generatesApi() );
    assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
  }

  @Test
  public void build_namePrefixPresent_with_trailing_underscore()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  namePrefix: myapp_\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getName( Nature.Java ), "myapp_com_example__myapp" );
    assertTrue( artifactRecord.generatesApi() );
    assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
  }

  @Test
  public void getAuthenticationContexts()
    throws Exception
  {
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

    writeConfigFile( dir,
                     "  - name: my-repo\n" +
                     "    url: http://my-repo.example.com/maven2\n" );
    final ApplicationRecord record = loadApplicationRecord();

    final Map<String, AuthenticationContext> contexts = record.getAuthenticationContexts();
    assertEquals( contexts.size(), 1 );
    final AuthenticationContext context = contexts.get( "my-repo" );
    assertNotNull( context );
    assertEquals( context.get( AuthenticationContext.USERNAME ), "root" );
    assertEquals( context.get( AuthenticationContext.PASSWORD ), "secret" );
  }

  @Test
  public void build_manyDependencies()
    throws Exception
  {
    // Provided ignored by traversal
    // System collected but ignored at later stage
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib:1.0",
                                     "com.example:rtA:jar::33.0:runtime" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:mylib:1.0",
                                     "com.example:rtB:jar::2.0:runtime",
                                     "org.test4j:core:jar::44.0:test" );
    deployArtifactToLocalRepository( dir, "com.example:rtA:33.0" );
    deployArtifactToLocalRepository( dir, "com.example:rtB:2.0",
                                     // Provided ignored by traversal
                                     "com.example:container:jar::4.0:provided",
                                     // System collected but ignored at later stage
                                     "com.example:kernel:jar::4.0:system" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertTrue( record.getAuthenticationContexts().isEmpty() );
    assertNonSystemArtifactCount( record, 4 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib,com.example:rtA,com.example:rtB" );

    {
      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__mylib" );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__rta" );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__rtb" );
      assertTrue( artifactRecord.generatesApi() );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtB:2.0" );
      assertNull( artifactRecord.getProcessors() );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getReverseDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
      assertEquals( artifactRecord.getReverseRuntimeDeps().size(), 1 );
      assertEquals( artifactRecord.getReverseRuntimeDeps().get( 0 ).getKey(), "com.example:mylib" );
    }
  }

  @Test
  public void build_singleDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final String url = dir.toUri().toString();
    final String urlEncoded = url.replaceAll( ":", "\\\\:" );

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib" );

    {
      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
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
                    "js_assets=-\n" +
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__mylib" );
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
                    "js_assets=-\n" +
                    "processors=-\n" +
                    "sources.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0-sources.jar\n" +
                    "sources.present=true\n" +
                    "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" );
    }
  }

  @Test
  public void build_singleDependency_with_Sources()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final String url = dir.toUri().toString();
    final String urlEncoded = url.replaceAll( ":", "\\\\:" );

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final Path cacheDir = FileUtil.createLocalTempDir();
    final ApplicationRecord record = loadApplicationRecord( cacheDir );

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib" );

    {
      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
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
                    "js_assets=-\n" +
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__mylib" );
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
                    "js_assets=-\n" +
                    "processors=-\n" +
                    "sources.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0-sources.jar\n" +
                    "sources.present=true\n" +
                    "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n" );
    }
  }

  @Test
  public void multipleDependenciesWithSameKeyOmitsSecond()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib:jar:sources:1.0",
                                     "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 2 );

    final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
    final List<ArtifactRecord> deps = artifactRecord.getDeps();
    assertEquals( deps.size(), 1 );
    assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib:jar:1.0" );
  }

  @Test
  public void multipleDependenciesWithSameKeyInRuntimeDeps()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib:jar:sources:1.0:runtime",
                                     "com.example:mylib:jar::1.0:runtime" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 2 );

    final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
    final List<ArtifactRecord> deps = artifactRecord.getRuntimeDeps();
    assertEquals( deps.size(), 1 );
    assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib:jar:1.0" );
  }

  @Test
  public void propagateNature_J2cl()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 3 );

    assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
  }

  @Test
  public void propagateNature_J2cl_viaDefaultNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  defaultNature: J2cl\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 3 );

    assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
  }

  @Test
  public void propagateNature_J2cl_declaredTransitivelyPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" +
                          "  - coord: com.example:base:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 3 );

    assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
  }

  @Test
  public void propagateNature_J2cl_replacementPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" +
                          "  - coord: com.example:base:1.0\n" +
                          "replacements:\n" +
                          "  - coord: com.example:mylib\n" +
                          "    targets:\n" +
                          "      - target: \"@com_example//:mylib\"\n" +
                          "        nature: J2cl\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 3 );

    assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                  Collections.singletonList( Nature.J2cl ) );
    assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                  Collections.singletonList( Nature.Java ) );
  }

  @Test
  public void propagateNature_J2cl_transitiveNonJ2clDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" +
                          "  - coord: com.example:base:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:base:jar:1.0' does not specify the J2cl nature but is a transitive dependency of 'com.example:myapp:jar:1.0' which has the J2cl nature. This is not a supported scenario." );
  }

  @Test
  public void propagateNature_J2cl_directNonJ2clDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" +
                          "  - coord: com.example:mylib:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:mylib:jar:1.0' does not specify the J2cl nature but is a direct dependency of 'com.example:myapp:jar:1.0' which has the J2cl nature. This is not a supported scenario." );
  }

  @Test
  public void propagateNature_Java()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  defaultNature: J2cl\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Java]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 3 );

    assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                  Collections.singletonList( Nature.Java ) );
    assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                  Collections.singletonList( Nature.Java ) );
    assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                  Collections.singletonList( Nature.Java ) );
  }

  @Test
  public void propagateNature_Java_directNonJavaDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  defaultNature: J2cl\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Java]\n" +
                          "  - coord: com.example:mylib:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:mylib:jar:1.0' does not specify the Java nature but is a direct dependency of 'com.example:myapp:jar:1.0' which has the Java nature. This is not a supported scenario." );
  }

  @Test
  public void propagateNature_Plugin()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  defaultNature: J2cl\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 3 );

    assertEquals( record.getArtifact( "com.example", "myapp" ).getNatures(),
                  Collections.singletonList( Nature.Plugin ) );
    assertEquals( record.getArtifact( "com.example", "mylib" ).getNatures(),
                  Collections.singletonList( Nature.Java ) );
    assertEquals( record.getArtifact( "com.example", "base" ).getNatures(),
                  Collections.singletonList( Nature.Java ) );
  }

  @Test
  public void propagateNature_Plugin_directNonJavaDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  defaultNature: J2cl\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" +
                          "  - coord: com.example:mylib:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:mylib:jar:1.0' does not specify the Java nature but is a direct dependency of 'com.example:myapp:jar:1.0' which has the Plugin nature. This is not a supported scenario." );
  }

  @Test
  public void depsAreSorted()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib1:1.0",
                                     "com.example:mylib3:1.0",
                                     "com.example:mylib2:1.0",
                                     "com.example:mylib4:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib1:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib2:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib3:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib4:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 5 );

    final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
    final List<ArtifactRecord> deps = artifactRecord.getDeps();
    assertEquals( deps.size(), 4 );
    assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib1:jar:1.0" );
    assertEquals( deps.get( 1 ).getArtifact().toString(), "com.example:mylib2:jar:1.0" );
    assertEquals( deps.get( 2 ).getArtifact().toString(), "com.example:mylib3:jar:1.0" );
    assertEquals( deps.get( 3 ).getArtifact().toString(), "com.example:mylib4:jar:1.0" );
  }

  @Test
  public void runtimeDepsAreSorted()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib1:jar::1.0:runtime",
                                     "com.example:mylib3:jar::1.0:runtime",
                                     "com.example:mylib2:jar::1.0:runtime",
                                     "com.example:mylib4:jar::1.0:runtime" );
    deployArtifactToLocalRepository( dir, "com.example:mylib1:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib2:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib3:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib4:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 5 );

    final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
    final List<ArtifactRecord> deps = artifactRecord.getRuntimeDeps();
    assertEquals( deps.size(), 4 );
    assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib1:jar:1.0" );
    assertEquals( deps.get( 1 ).getArtifact().toString(), "com.example:mylib2:jar:1.0" );
    assertEquals( deps.get( 2 ).getArtifact().toString(), "com.example:mylib3:jar:1.0" );
    assertEquals( deps.get( 3 ).getArtifact().toString(), "com.example:mylib4:jar:1.0" );
  }

  @Test
  public void multipleDependenciesWithSameKeyOmitsSecondIfBothHaveClassifiers()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib:jar:stripped:1.0",
                                     "com.example:mylib:jar:sources:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:jar:stripped:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 2 );

    final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
    final List<ArtifactRecord> deps = artifactRecord.getDeps();
    assertEquals( deps.size(), 1 );
    assertEquals( deps.get( 0 ).getArtifact().toString(), "com.example:mylib:jar:stripped:1.0" );
  }

  @Test
  public void build_singleRuntimeDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:rtA:jar::33.0:runtime" );
    deployArtifactToLocalRepository( dir, "com.example:rtA:jar:33.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:rtA" );

    {
      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
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
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__rta" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
      assertTrue( artifactRecord.generatesApi() );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtA:33.0" );
      assertEquals( artifactRecord.getDeps().size(), 0 );
      assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
    }
  }

  @Test
  public void build_versionlessDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "  - coord: com.example:mylib\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib" );

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
  }

  @Test
  public void build_conflicts()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib:1.0",
                                     "com.example:rtA:jar::33.0:runtime" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:mylib:1.0",
                                     "com.example:rtA:jar::32.0:runtime" );
    deployArtifactToLocalRepository( dir, "com.example:rtA:32.0" );
    deployArtifactToLocalRepository( dir, "com.example:rtA:33.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 3 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib,com.example:rtA" );

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
  }

  @Test
  public void build_replacement()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "replacements:\n" +
                     "  - coord: com.example:mylib\n" +
                     "    targets:\n" +
                     "      - target: \"@com_example//:mylib\"\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib" );

    {
      final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
      assertNotNull( artifactRecord );
      assertNotNull( artifactRecord.getArtifactModel() );
      assertNull( artifactRecord.getReplacementModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
      assertEquals( artifactRecord.getLabel( Nature.Java ), ":com_example__myapp" );
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
  }

  @Test
  public void build_exclude()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "excludes:\n" +
                     "  - coord: com.example:mylib\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 1 );
    assertNonSystemArtifactList( record, "com.example:myapp" );

    final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
    assertNotNull( artifactRecord );
    assertNotNull( artifactRecord.getArtifactModel() );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getDeps().size(), 0 );
    assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
  }

  @Test
  public void build_whereRuntimeDependencyExcluded()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "excludes:\n" +
                     "  - coord: com.example:mylib\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:runtime" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 1 );
    assertNonSystemArtifactList( record, "com.example:myapp" );

    final ArtifactRecord artifactRecord = record.findArtifact( "com.example", "myapp" );
    assertNotNull( artifactRecord );
    assertNotNull( artifactRecord.getArtifactModel() );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getDeps().size(), 0 );
    assertEquals( artifactRecord.getRuntimeDeps().size(), 0 );
  }

  @Test
  public void build_singleOptionalDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    includeOptional: true\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:compile:optional" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertEquals( record.getSource().getConfigLocation(), getDefaultConfigFile().toAbsolutePath().normalize() );
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example:myapp,com.example:mylib" );

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
  }

  @Test
  public void findArtifact()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ArtifactRecord artifact1 = record.findArtifact( "com.example", "myapp" );
    assertNotNull( artifact1 );
    assertEquals( artifact1.getKey(), "com.example:myapp" );

    assertNull( record.findArtifact( "com.example", "other-no-exist" ) );

    // Also finds system artifacts
    final ArtifactRecord artifact2 = record.findArtifact( DepGenConfig.getGroupId(), DepGenConfig.getArtifactId() );
    assertNotNull( artifact2 );
    assertEquals( artifact2.getKey(), DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() );
  }

  @Test
  public void getArtifact()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ArtifactRecord artifact1 = record.getArtifact( "com.example", "myapp" );
    assertNotNull( artifact1 );
    assertEquals( artifact1.getKey(), "com.example:myapp" );

    assertThrows( NullPointerException.class, () -> record.getArtifact( "com.example", "other-no-exist" ) );
  }

  @Test
  public void getNature_withDefaultJavaNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
  }

  @Test
  public void getNature_withDefaultPluginNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "react4j.processor.ReactProcessor\n" +
                     "arez.processor.ArezProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
    assertTrue( artifactRecord.generatesApi() );
  }

  @Test
  public void getNature_withExplicitNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [Plugin]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
    assertTrue( artifactRecord.generatesApi() );
  }

  @Test
  public void getNature_of_transitiveDependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 2 );
    {
      final ArtifactRecord artifactRecord = artifacts.get( 0 );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
      assertTrue( artifactRecord.generatesApi() );
    }
    {
      final ArtifactRecord artifactRecord = artifacts.get( 1 );
      assertEquals( artifactRecord.getKey(), "com.example:mylib" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
      assertTrue( artifactRecord.generatesApi() );
    }
  }

  @Test
  public void defaultGeneratesApi()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "react4j.processor.ReactProcessor\n" +
                     "arez.processor.ArezProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
    assertTrue( artifactRecord.generatesApi() );
  }

  @Test
  public void explicitTrueGeneratesApi()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    plugin:\n" +
                     "      generatesApi: true\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "react4j.processor.ReactProcessor\n" +
                     "arez.processor.ArezProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
    assertTrue( artifactRecord.generatesApi() );
  }

  @Test
  public void explicitGeneratesApiForNonPlugin()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    plugin:\n" +
                     "      generatesApi: false\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:myapp:jar:1.0' has specified 'plugin' configuration but does not specify the Plugin nature nor does it contain any annotation processors." );
  }

  @Test
  public void explicitGeneratesApiWhereNoProcessors()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [Plugin]\n" +
                     "    plugin:\n" +
                     "      generatesApi: false\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:myapp:jar:1.0' has specified 'plugin.generatesApi' configuration but does not contain any annotation processors." );
  }

  @Test
  public void explicitFalseGeneratesApi()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    plugin:\n" +
                     "      generatesApi: false\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "react4j.processor.ReactProcessor\n" +
                     "arez.processor.ArezProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Plugin ) );
    assertFalse( artifactRecord.generatesApi() );
  }

  @Test
  public void j2clImportWithSuppress()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [J2cl]\n" +
                     "    j2cl:\n" +
                     "      mode: Import\n" +
                     "      suppress: [\"checkDebuggerStatement\"]\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:myapp:jar:1.0' has specified 'j2cl.suppress' configuration but specified 'j2cl.mode = Import' which is incompatible with 'j2cl.suppress'." );
  }

  @Test
  public void j2clConfigWithoutJ2clNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    j2cl:\n" +
                     "      suppress: [\"checkDebuggerStatement\"]\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:myapp:jar:1.0' has specified 'j2cl' configuration but does not specify the J2cl nature." );
  }

  @Test
  public void javaConfigWithoutJavaNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [J2cl]\n" +
                     "    java:\n" +
                     "      exportDeps: true\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:myapp:jar:1.0' has specified 'java' configuration but does not specify the Java nature." );
  }

  @Test
  public void getName_withNameStrategy()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  nameStrategy: ArtifactId\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getName( Nature.Java ), "myapp" );
  }

  @Test
  public void getName_withNameStrategyAndPrefix()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  namePrefix: gwt_\n" +
                     "  nameStrategy: ArtifactId\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getName( Nature.Java ), "gwt_myapp" );
  }

  @Test
  public void loadWhereDuplicateNamesExist()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  nameStrategy: ArtifactId\n" +
                     "artifacts:\n" +
                     "  - coord: com.example.app1:core:42.0\n" +
                     "  - coord: com.example.app2:core:37.0\n" );
    deployArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
    deployArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Multiple artifacts have the same name 'core' which is not supported. Change the nameStrategy option globally or for one of the artifacts 'com.example.app1:core:jar:42.0' and 'com.example.app2:core:jar:37.0'." );
  }

  @Test
  public void loadWhereDuplicateNamesWorkedAroundViaExplicitNames()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  nameStrategy: ArtifactId\n" +
                     "artifacts:\n" +
                     "  - coord: com.example.app1:core:42.0\n" +
                     "    nameStrategy: GroupIdAndArtifactId\n" +
                     "  - coord: com.example.app2:core:37.0\n" );
    deployArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
    deployArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example.app1:core,com.example.app2:core" );

    final ArtifactRecord artifactRecord1 = artifacts.get( 0 );
    assertEquals( artifactRecord1.getKey(), "com.example.app1:core" );
    assertEquals( artifactRecord1.getName( Nature.Java ), "com_example_app1__core" );

    final ArtifactRecord artifactRecord2 = artifacts.get( 1 );
    assertEquals( artifactRecord2.getKey(), "com.example.app2:core" );
    assertEquals( artifactRecord2.getName( Nature.Java ), "core" );
  }

  @Test
  public void loadWhereJ2clArtifactOmitsSourcesClassifierVariant()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example.app1:core:42.0\n" +
                     "    natures: [J2cl]\n" );
    deployTempArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );
    assertEquals( exception.getMessage(),
                  "Unable to locate the sources classifier artifact for the artifact 'com.example.app1:core:jar:42.0' but the artifact has the J2cl nature which requires that sources be present." );
  }

  @Test
  public void shouldExportDeps_perArtifactConfig()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example.app1:core:42.0\n" +
                     "    java:\n" +
                     "      exportDeps: true\n" +
                     "  - coord: com.example.app2:core:37.0\n" );
    deployArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
    deployArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

    final ApplicationRecord record = loadApplicationRecord();

    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example.app1:core,com.example.app2:core" );

    final ArtifactRecord artifactRecord1 = record.getArtifact( "com.example.app1", "core" );
    assertEquals( artifactRecord1.getKey(), "com.example.app1:core" );
    assertTrue( artifactRecord1.shouldExportDeps() );

    final ArtifactRecord artifactRecord2 = record.getArtifact( "com.example.app2", "core" );
    assertEquals( artifactRecord2.getKey(), "com.example.app2:core" );
    assertFalse( artifactRecord2.shouldExportDeps() );
  }

  @Test
  public void shouldExportDeps_perArtifactConfig_combinedWithGlobalConfig()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  java:\n" +
                     "    exportDeps: true\n" +
                     "artifacts:\n" +
                     "  - coord: com.example.app1:core:42.0\n" +
                     "    java:\n" +
                     "      exportDeps: false\n" +
                     "  - coord: com.example.app2:core:37.0\n" );
    deployArtifactToLocalRepository( dir, "com.example.app1:core:42.0" );
    deployArtifactToLocalRepository( dir, "com.example.app2:core:37.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 2 );
    assertNonSystemArtifactList( record, "com.example.app1:core,com.example.app2:core" );

    final ArtifactRecord artifactRecord1 = artifacts.get( 0 );
    assertEquals( artifactRecord1.getKey(), "com.example.app1:core" );
    assertFalse( artifactRecord1.shouldExportDeps() );

    final ArtifactRecord artifactRecord2 = artifacts.get( 1 );
    assertEquals( artifactRecord2.getKey(), "com.example.app2:core" );
    assertTrue( artifactRecord2.shouldExportDeps() );
  }

  @Test
  public void parseWhereArtifactContainsProcessors()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "react4j.processor.ReactProcessor\n" +
                     "arez.processor.ArezProcessor\n" );

    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0", jarFile );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ApplicationRecord record = loadApplicationRecord();

    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertNotNull( artifactRecord.getArtifactModel() );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getProcessors(),
                  Arrays.asList( "react4j.processor.ReactProcessor", "arez.processor.ArezProcessor" ) );
  }

  @Test
  public void artifactWithDefaultNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertEquals( artifacts.size(), 1 + record.getSource().getSystemArtifacts().size() );
    {
      final ArtifactRecord artifactRecord = record.getArtifact( "com.example", "myapp" );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
    }
    {
      final ArtifactRecord artifactRecord = record.getArtifact( "org.realityforge.bazel.depgen", "bazel-depgen" );
      assertEquals( artifactRecord.getMavenCoordinatesBazelTag(), "org.realityforge.bazel.depgen:bazel-depgen:1" );
      assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.Java ) );
    }
  }

  @Test
  public void artifactWithSpecifiedDefaultNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  defaultNature: J2cl\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.J2cl ) );
  }

  @Test
  public void artifactWithSpecifiedNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertNonSystemArtifactCount( record, 1 );
    final ArtifactRecord artifactRecord = artifacts.get( 0 );
    assertEquals( artifactRecord.getNatures(), Collections.singletonList( Nature.J2cl ) );
  }

  @Test
  public void writeTargetMacro()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeTargetMacro_where_verifySha256_false()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  verifyConfigSha256: false\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeTargetMacro_omitEnabled()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  supportDependencyOmit: true\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets(\n" +
                  "        omit_com_example__myapp = False,\n" +
                  "        omit_org_realityforge_bazel_depgen__bazel_depgen = False):\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    if not omit_com_example__myapp:\n" +
                  "        _java_import(\n" +
                  "            name = \"com_example__myapp\",\n" +
                  "            jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "            srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "            tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        )\n" +
                  "\n" +
                  "    if not omit_org_realityforge_bazel_depgen__bazel_depgen:\n" +
                  "        _java_import(\n" +
                  "            name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "            jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "            tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "        )\n" );
  }

  @Test
  public void writeTargetMacro_omitEnabled_orderOmitInDeclarationsAlphanumerically()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  supportDependencyOmit: true\n" +
                          "  nameStrategy: ArtifactId\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets(\n" +
                  "        omit_bazel_depgen = False,\n" +
                  "        omit_myapp = False):\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    if not omit_myapp:\n" +
                  "        _java_import(\n" +
                  "            name = \"myapp\",\n" +
                  "            jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "            srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "            tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        )\n" +
                  "\n" +
                  "    if not omit_bazel_depgen:\n" +
                  "        _java_import(\n" +
                  "            name = \"bazel_depgen\",\n" +
                  "            jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "            tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "        )\n" );
  }

  @Test
  public void writeTargetMacro_macroNameOverride()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  targetMacroName: generate_myapp_targets\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_myapp_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeTargetMacro_dependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        deps = [\":com_example__mylib\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__mylib\",\n" +
                  "        jars = [\"@com_example__mylib__2_0//file\"],\n" +
                  "        srcjar = \"@com_example__mylib__2_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:mylib:2.0\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeTargetMacro_replacement()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "replacements:\n" +
                          "  - coord: com.example:mylib\n" +
                          "    targets:\n" +
                          "      - target: \"@com_example//:mylib\"\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        deps = [\"@com_example//:mylib\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeTargetMacro_depgen_replacement()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "replacements:\n" +
                          "  - coord: org.realityforge.bazel.depgen:bazel-depgen\n" +
                          "    targets:\n" +
                          "      - target: \"@org_realityforge_bazel//:depgen\"\n" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeTargetMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\"@org_realityforge_bazel//:depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\"@org_realityforge_bazel//:depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeWorkspaceMacro()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeWorkspaceMacro_externalAnnotationsPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir,
                     "options:\n" +
                     "  includeExternalAnnotations: true\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:annotations:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__annotations\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-annotations.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-annotations.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeWorkspaceMacro_omitEnabled()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "options:\n" +
                          "  supportDependencyOmit: true\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "def generate_workspace_rules(\n" +
                  "        omit_com_example__myapp = False,\n" +
                  "        omit_org_realityforge_bazel_depgen__bazel_depgen = False):\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    if not omit_com_example__myapp:\n" +
                  "        _http_file(\n" +
                  "            name = \"com_example__myapp__1_0\",\n" +
                  "            downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "            sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "            urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "        )\n" +
                  "\n" +
                  "        _http_file(\n" +
                  "            name = \"com_example__myapp__1_0__sources\",\n" +
                  "            downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "            sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "            urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "        )\n" +
                  "\n" +
                  "    if not omit_org_realityforge_bazel_depgen__bazel_depgen:\n" +
                  "        _http_file(\n" +
                  "            name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "            downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "            sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "            urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "        )\n" );
  }

  @Test
  public void writeWorkspaceMacro_macroNameOverride()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "options:\n" +
                          "  workspaceMacroName: generate_myapp_workspace_rules\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "def generate_myapp_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeWorkspaceMacro_dependency()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__mylib__2_0\",\n" +
                  "        downloaded_file_path = \"com/example/mylib/2.0/mylib-2.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/2.0/mylib-2.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__mylib__2_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/mylib/2.0/mylib-2.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/2.0/mylib-2.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeWorkspaceMacro_replacement()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "replacements:\n" +
                          "  - coord: com.example:mylib\n" +
                          "    targets:\n" +
                          "      - target: \"@com_example//:mylib\"\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeWorkspaceMacro( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeDependencyGraphIfRequired()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeDependencyGraphIfRequired( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "#    \\- com.example:mylib:jar:2.0 [compile]\n" +
                  "\n" );
  }

  @Test
  public void writeDependencyGraphIfRequired_disabledInConfig()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  emitDependencyGraph: false\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:2.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:2.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeDependencyGraphIfRequired( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ), "" );
  }

  @Test
  public void writeRegenerateExtensionTarget()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeRegenerateExtensionTarget( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "_java_binary(\n" +
                  "    name = \"regenerate_depgen_extension\",\n" +
                  "    runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "    main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "    args = [\n" +
                  "        \"--config-file\",\n" +
                  "        \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "        \"--verbose\",\n" +
                  "        \"generate\",\n" +
                  "    ],\n" +
                  "    tags = [\n" +
                  "        \"local\",\n" +
                  "        \"manual\",\n" +
                  "        \"no-cache\",\n" +
                  "        \"no-remote\",\n" +
                  "        \"no-sandbox\",\n" +
                  "    ],\n" +
                  "    data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeVerifyTarget()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeVerifyTarget( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "_java_test(\n" +
                  "    name = \"verify_config_sha256\",\n" +
                  "    size = \"small\",\n" +
                  "    runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "    main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "    use_testrunner = False,\n" +
                  "    args = [\n" +
                  "        \"--config-file\",\n" +
                  "        \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "        \"--verbose\",\n" +
                  "        \"hash\",\n" +
                  "        \"--verify-sha256\",\n" +
                  "        _CONFIG_SHA256,\n" +
                  "    ],\n" +
                  "    data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeVerifyTarget_usingPrefixAndAlternativeNameStrategy()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  namePrefix: myapp\n" +
                          "  nameStrategy: ArtifactId\n" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeVerifyTarget( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "_java_test(\n" +
                  "    name = \"myapp_verify_config_sha256\",\n" +
                  "    size = \"small\",\n" +
                  "    runtime_deps = [\":myapp_bazel_depgen\"],\n" +
                  "    main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "    use_testrunner = False,\n" +
                  "    args = [\n" +
                  "        \"--config-file\",\n" +
                  "        \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "        \"--verbose\",\n" +
                  "        \"hash\",\n" +
                  "        \"--verify-sha256\",\n" +
                  "        _CONFIG_SHA256,\n" +
                  "    ],\n" +
                  "    data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeBazelExtension()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeBazelExtension_where_verifyConfigSha256_is_false()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir,
                     "options:\n" +
                     "  verifyConfigSha256: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_import = \"java_import\")\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeDefaultExtensionBuild_configFileInSameDirectory()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeDefaultExtensionBuild( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "# File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "# Contents can be edited and will not be overridden.\n" +
                  "package(default_visibility = [\"//visibility:public\"])\n" +
                  "\n" +
                  "load(\"//thirdparty:dependencies.bzl\", \"generate_targets\")\n" +
                  "\n" +
                  "generate_targets()\n" +
                  "\n" +
                  "exports_files([\"dependencies.yml\"])\n" );
  }

  @Test
  public void writeDefaultExtensionBuild_configFileInDifferentDirectory()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  extensionFile: somedir/dependencies.bzl\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeDefaultExtensionBuild( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "# File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "# Contents can be edited and will not be overridden.\n" +
                  "package(default_visibility = [\"//visibility:public\"])\n" +
                  "\n" +
                  "load(\"//thirdparty/somedir:dependencies.bzl\", \"generate_targets\")\n" +
                  "\n" +
                  "generate_targets()\n" );
  }

  @Test
  public void writeDefaultConfigBuild()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeDefaultConfigBuild( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), dir.toUri().toString() ),
                  "# File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "# Contents can be edited and will not be overridden.\n" +
                  "package(default_visibility = [\"//visibility:public\"])\n" +
                  "\n" +
                  "exports_files([\"dependencies.yml\"])\n" );
  }

  @Test
  public void writeBazelExtension_j2clArtifactPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl, Java]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "load(\"@com_google_j2cl//build_defs:rules.bzl\", _j2cl_library = \"j2cl_library\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"com_example__myapp-j2cl\",\n" +
                  "        srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeBazelExtension_j2cl_withDependencies()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "#    \\- com.example:mylib:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "load(\"@com_google_j2cl//build_defs:rules.bzl\", _j2cl_library = \"j2cl_library\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__mylib__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/mylib/1.0/mylib-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"com_example__myapp-j2cl\",\n" +
                  "        srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  "        deps = [\":com_example__mylib-j2cl\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"com_example__mylib-j2cl\",\n" +
                  "        srcs = [\"@com_example__mylib__1_0__sources//file\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeBazelExtension_j2cl_withDependenciesAndJsAssets()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );

    final Path jarFile1 = createJarFile( outputStream -> {
      createJarEntry( outputStream, "com/biz/MyFile1.js", "" );
      createJarEntry( outputStream, "com/biz/MyOtherFile.js", "" );
      createJarEntry( outputStream, "com/biz/MyBlah.js", "" );
      createJarEntry( outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "" );
      createJarEntry( outputStream, "com/biz/TheClass.native.js", "" );
      createJarEntry( outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "" );
    } );
    final Path jarFile2 = createJarFile( "foo.js", "" );
    deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0", jarFile1 );
    deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0", jarFile2 );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "#    \\- com.example:mylib:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\", _http_archive = \"http_archive\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "load(\"@com_google_j2cl//build_defs:rules.bzl\", _j2cl_library = \"j2cl_library\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_archive(\n" +
                  "        name = \"com_example__myapp__1_0__js_sources\",\n" +
                  "        sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "        build_file_content = \"\"\"\n" +
                  "filegroup(\n" +
                  "    name = \"srcs\",\n" +
                  "    visibility = [\"//visibility:public\"],\n" +
                  "    srcs = [\"foo.js\"],\n" +
                  ")\n" +
                  "\"\"\",\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__mylib__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/mylib/1.0/mylib-1.0-sources.jar\",\n" +
                  "        sha256 = \"e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_archive(\n" +
                  "        name = \"com_example__mylib__1_0__js_sources\",\n" +
                  "        sha256 = \"e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar\"],\n" +
                  "        build_file_content = \"\"\"\n" +
                  "filegroup(\n" +
                  "    name = \"srcs\",\n" +
                  "    visibility = [\"//visibility:public\"],\n" +
                  "    srcs = [\n" +
                  "        \"com/biz/MyBlah.js\",\n" +
                  "        \"com/biz/MyFile1.js\",\n" +
                  "        \"com/biz/MyOtherFile.js\",\n" +
                  "    ],\n" +
                  ")\n" +
                  "\"\"\",\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"com_example__myapp-j2cl\",\n" +
                  "        srcs = [\n" +
                  "            \"@com_example__myapp__1_0__sources//file\",\n" +
                  "            \"@com_example__myapp__1_0__js_sources//:srcs\",\n" +
                  "        ],\n" +
                  "        deps = [\":com_example__mylib-j2cl\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"com_example__mylib-j2cl\",\n" +
                  "        srcs = [\n" +
                  "            \"@com_example__mylib__1_0__sources//file\",\n" +
                  "            \"@com_example__mylib__1_0__js_sources//:srcs\",\n" +
                  "        ],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeBazelExtension_java_withDependenciesAndJsAssets()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Java]\n" );

    final Path jarFile1 = createJarFile( outputStream -> {
      createJarEntry( outputStream, "com/biz/MyFile1.js", "" );
      createJarEntry( outputStream, "com/biz/MyOtherFile.js", "" );
      createJarEntry( outputStream, "com/biz/MyBlah.js", "" );
      createJarEntry( outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "" );
      createJarEntry( outputStream, "com/biz/TheClass.native.js", "" );
      createJarEntry( outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "" );
    } );
    final Path jarFile2 = createJarFile( "foo.js", "" );
    deployTempArtifactToLocalRepository( dir, "com.example:mylib:jar:sources:1.0", jarFile1 );
    deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0", jarFile2 );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "#    \\- com.example:mylib:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__mylib__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/mylib/1.0/mylib-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/1.0/mylib-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__mylib__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/mylib/1.0/mylib-1.0-sources.jar\",\n" +
                  "        sha256 = \"e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f\",\n" +
                  "        urls = [\"MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "        deps = [\":com_example__mylib\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__mylib\",\n" +
                  "        jars = [\"@com_example__mylib__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__mylib__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:mylib:1.0\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeBazelExtension_Plugin()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_library = \"java_library\", _java_plugin = \"java_plugin\", _java_test = \"java_test\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp__plugin_library\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "    _java_plugin(\n" +
                  "        name = \"com_example__myapp__1_0__plugin\",\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "        deps = [\":com_example__myapp__plugin_library\"],\n" +
                  "    )\n" +
                  "    _java_library(\n" +
                  "        name = \"com_example__myapp\",\n" +
                  "        exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void writeBazelExtension_withNamesAndMultipleNatures()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final URI uri = dir.toUri();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [Java, J2cl, Plugin]\n" +
                     "    java:\n" +
                     "      name: myapp-java-a\n" +
                     "    j2cl:\n" +
                     "      name: myapp-j2cl-a\n" +
                     "    plugin:\n" +
                     "      name: myapp-plugin-a\n" +
                     "  - coord: com.example:myapp2:1.0\n" +
                     "    natures: [Java, J2cl, Plugin]\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp2:1.0", "com.example:myapp:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationRecord record = loadApplicationRecord();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    record.writeBazelExtension( new StarlarkOutput( outputStream ) );
    assertEquals( asCleanString( outputStream, record.getSource().getConfigSha256(), uri.toString() ),
                  "# DO NOT EDIT: File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen version 1\n" +
                  "\n" +
                  "\"\"\"\n" +
                  "    Macro rules to load dependencies.\n" +
                  "\n" +
                  "    Invoke 'generate_workspace_rules' from a WORKSPACE file.\n" +
                  "    Invoke 'generate_targets' from a BUILD.bazel file.\n" +
                  "\"\"\"\n" +
                  "# Dependency Graph Generated from the input data\n" +
                  "# +- com.example:myapp:jar:1.0 [compile]\n" +
                  "# \\- com.example:myapp2:jar:1.0 [compile]\n" +
                  "#    \\- com.example:myapp:jar:1.0 [compile]\n" +
                  "\n" +
                  "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")\n" +
                  "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_import = \"java_import\", _java_test = \"java_test\")\n" +
                  "load(\"@com_google_j2cl//build_defs:rules.bzl\", _j2cl_library = \"j2cl_library\")\n" +
                  "\n" +
                  "# SHA256 of the configuration content that generated this file\n" +
                  "_CONFIG_SHA256 = \"MYSHA\"\n" +
                  "\n" +
                  "def generate_workspace_rules():\n" +
                  "    \"\"\"\n" +
                  "        Repository rules macro to load dependencies.\n" +
                  "\n" +
                  "        Must be run from a WORKSPACE file.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp2__1_0\",\n" +
                  "        downloaded_file_path = \"com/example/myapp2/1.0/myapp2-1.0.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp2/1.0/myapp2-1.0.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"com_example__myapp2__1_0__sources\",\n" +
                  "        downloaded_file_path = \"com/example/myapp2/1.0/myapp2-1.0-sources.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/com/example/myapp2/1.0/myapp2-1.0-sources.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _http_file(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen__1\",\n" +
                  "        downloaded_file_path = \"org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\",\n" +
                  "        sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "        urls = [\"MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar\"],\n" +
                  "    )\n" +
                  "\n" +
                  "def generate_targets():\n" +
                  "    \"\"\"\n" +
                  "        Macro to define targets for dependencies.\n" +
                  "    \"\"\"\n" +
                  "\n" +
                  "    _java_test(\n" +
                  "        name = \"verify_config_sha256\",\n" +
                  "        size = \"small\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        use_testrunner = False,\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"hash\",\n" +
                  "            \"--verify-sha256\",\n" +
                  "            _CONFIG_SHA256,\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_binary(\n" +
                  "        name = \"regenerate_depgen_extension\",\n" +
                  "        runtime_deps = [\":org_realityforge_bazel_depgen__bazel_depgen\"],\n" +
                  "        main_class = \"org.realityforge.bazel.depgen.Main\",\n" +
                  "        args = [\n" +
                  "            \"--config-file\",\n" +
                  "            \"$(rootpath //thirdparty:dependencies.yml)\",\n" +
                  "            \"--verbose\",\n" +
                  "            \"generate\",\n" +
                  "        ],\n" +
                  "        tags = [\n" +
                  "            \"local\",\n" +
                  "            \"manual\",\n" +
                  "            \"no-cache\",\n" +
                  "            \"no-remote\",\n" +
                  "            \"no-sandbox\",\n" +
                  "        ],\n" +
                  "        data = [\"//thirdparty:dependencies.yml\"],\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"myapp-java-a\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"myapp-j2cl-a\",\n" +
                  "        srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"myapp-java-a__plugin_library\",\n" +
                  "        jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    )\n" +
                  "    _java_plugin(\n" +
                  "        name = \"com_example__myapp__1_0__plugin\",\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "        deps = [\":myapp-java-a__plugin_library\"],\n" +
                  "    )\n" +
                  "    _java_library(\n" +
                  "        name = \"myapp-plugin-a\",\n" +
                  "        exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp2\",\n" +
                  "        jars = [\"@com_example__myapp2__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp2__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp2:1.0\"],\n" +
                  "        deps = [\":myapp-java-a\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _j2cl_library(\n" +
                  "        name = \"com_example__myapp2-j2cl\",\n" +
                  "        srcs = [\"@com_example__myapp2__1_0__sources//file\"],\n" +
                  "        deps = [\":myapp-j2cl-a\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"com_example__myapp2__plugin_library\",\n" +
                  "        jars = [\"@com_example__myapp2__1_0//file\"],\n" +
                  "        srcjar = \"@com_example__myapp2__1_0__sources//file\",\n" +
                  "        tags = [\"maven_coordinates=com.example:myapp2:1.0\"],\n" +
                  "        deps = [\":myapp-java-a\"],\n" +
                  "    )\n" +
                  "    _java_plugin(\n" +
                  "        name = \"com_example__myapp2__1_0__plugin\",\n" +
                  "        visibility = [\"//visibility:private\"],\n" +
                  "        deps = [\":com_example__myapp2__plugin_library\"],\n" +
                  "    )\n" +
                  "    _java_library(\n" +
                  "        name = \"com_example__myapp2-plugin\",\n" +
                  "        exported_plugins = [\"com_example__myapp2__1_0__plugin\"],\n" +
                  "    )\n" +
                  "\n" +
                  "    _java_import(\n" +
                  "        name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "        jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "        tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  "    )\n" );
  }

  @Test
  public void replacement_targetMissingForNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl, Java]\n" +
                          "  - coord: com.example:base:1.0\n" +
                          "replacements:\n" +
                          "  - coord: com.example:mylib\n" +
                          "    targets:\n" +
                          "      - target: \"@com_example//:mylib\"\n" +
                          "        nature: J2cl\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:mylib:jar:1.0' is a replacement and has a nature of 'Java' but has not declared a replacement target for that nature." );
  }

  @Test
  public void replacement_targetPresentButNoSuchNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Java]\n" +
                          "  - coord: com.example:base:1.0\n" +
                          "replacements:\n" +
                          "  - coord: com.example:mylib\n" +
                          "    targets:\n" +
                          "      - target: \"@com_example//:othermylib\"\n" +
                          "        nature: Java\n" +
                          "      - target: \"@com_example//:mylib\"\n" +
                          "        nature: J2cl\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0", "com.example:base:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:base:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'com.example:mylib:jar:1.0' declared target for nature 'J2cl' but artifact does not have specified nature." );
  }

  @Test
  public void ensureDepgenArtifactReplacementHasJavaNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "replacements:\n" +
                     "  - coord: " + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "\n" +
                     "    targets:\n" +
                     "      - target: \":depgen\"\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ApplicationModel model = loadApplicationRecord().getSource();

    final List<ReplacementModel> replacements = model.getReplacements();
    assertEquals( replacements.size(), 1 );
    final ReplacementModel replacementModel = replacements.get( 0 );
    final List<ReplacementTargetModel> targets = replacementModel.getTargets();
    assertEquals( targets.size(), 1 );
    final ReplacementTargetModel replacementTarget = targets.get( 0 );
    assertEquals( replacementTarget.getNature(), Nature.Java );
    assertEquals( replacementTarget.getTarget(), ":depgen" );
  }

  @Test
  public void ensureDepgenArtifactReplacementWithoutJavaNatureGeneratesError()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "replacements:\n" +
                     "  - coord: " + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "\n" +
                     "    targets:\n" +
                     "      - target: \":depgen\"\n" +
                     "        nature: J2cl\n" );

    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'org.realityforge.bazel.depgen:bazel-depgen' declared as a replace but does not declare the Java nature which is required if verifyConfigSha256 option is set to true." );
  }

  @Test
  public void ensureDeclaredDepgenArtifactHasJavaNature()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: " + DepGenConfig.getCoord() + "\n" );

    final ApplicationRecord record = loadApplicationRecord();
    final ApplicationModel model = record.getSource();

    final List<ArtifactModel> artifacts = model.getArtifacts();
    assertEquals( artifacts.size(), 1 );
    final ArtifactModel artifactModel = artifacts.get( 0 );
    assertTrue( artifactModel.getNatures( model.getOptions().getDefaultNature() ).contains( Nature.Java ) );
  }

  @Test
  public void ensureDeclaredDepgenArtifactWithoutJavaNatureGeneratesError()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: " + DepGenConfig.getCoord() + "\n" +
                     "    natures: [J2cl]\n" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'org.realityforge.bazel.depgen:bazel-depgen' declared as a dependency but does not declare the Java nature which is required if the verifyConfigSha256 option is set to true." );
  }

  @Test
  public void ensureDeclaredDepgenArtifactWithoutAllClassifierGeneratesError()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: " + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "\n" );

    final DepgenValidationException exception =
      expectThrows( DepgenValidationException.class, this::loadApplicationRecord );

    assertEquals( exception.getMessage(),
                  "Artifact 'org.realityforge.bazel.depgen:bazel-depgen' declared as a dependency but does not specify the classifier 'all' which is required if the verifyConfigSha256 option is set to true." );
  }

  @Test
  public void repository_with_searchByDefault_false()
    throws Exception
  {
    final Path dir1 = FileUtil.createLocalTempDir();
    final Path dir2 = FileUtil.createLocalTempDir();

    deployDepGenArtifactToLocalRepository( dir1 );
    writeConfigFile( "repositories:\n" +
                     "  - name: local1\n" +
                     "    url: " + dir1.toUri() + "\n" +
                     "  - name: local2\n" +
                     "    url: " + dir2.toUri() + "\n" +
                     "    searchByDefault: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );
    deployArtifactToLocalRepository( dir2, "com.example:myapp:1.0" );

    final List<ArtifactRecord> artifacts = loadApplicationRecord().getArtifacts();
    assertTrue( artifacts.size() > 1 );

    assertEquals( artifacts.get( 0 ).getUrls(),
                  Collections.singletonList( dir1.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
  }

  @Test
  public void repository_with_searchByDefault_false_but_artifact_repositories_include()
    throws Exception
  {
    final Path dir1 = FileUtil.createLocalTempDir();
    final Path dir2 = FileUtil.createLocalTempDir();

    deployDepGenArtifactToLocalRepository( dir1 );
    writeConfigFile( "repositories:\n" +
                     "  - name: local1\n" +
                     "    url: " + dir1.toUri() + "\n" +
                     "  - name: local2\n" +
                     "    url: " + dir2.toUri() + "\n" +
                     "    searchByDefault: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    repositories: [local1, local2]\n" );
    deployArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );
    deployArtifactToLocalRepository( dir2, "com.example:myapp:1.0" );

    final List<ArtifactRecord> artifacts = loadApplicationRecord().getArtifacts();
    assertTrue( artifacts.size() > 1 );

    assertEquals( artifacts.get( 0 ).getUrls(),
                  Arrays.asList( dir1.toUri() + "com/example/myapp/1.0/myapp-1.0.jar",
                                 dir2.toUri() + "com/example/myapp/1.0/myapp-1.0.jar" ) );
  }
}
