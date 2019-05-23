package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.gen.StarlarkFileOutput;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArtifactRecordTest
  extends AbstractTest
{
  @Test
  public void parseSimpleArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      assertNotNull( artifactRecord.getArtifactModel() );
      assertEquals( artifactRecord.getKey(), "com.example:myapp" );
      assertEquals( artifactRecord.getName(), "com_example__myapp__1_0" );
      assertEquals( artifactRecord.getAlias(), "com_example__myapp" );
      assertEquals( artifactRecord.getNature(), Nature.Library );
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
  public void emitJavaImport_simpleArtifact()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withNamePrefix()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "options:\n" +
                         "  namePrefix: zeapp\n" +
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"zeapp_com_example__myapp__1_0\",\n" +
                    "    jars = [\"@zeapp_com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withNameSuffix()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "__library" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0__library\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withSourceJar()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withDep()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__mylib\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_shouldExportDeps()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n    exportDeps: true\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__mylib\"],\n" +
                    "    exports = [\":com_example__mylib\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withRuntimeDep()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:runtime" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    runtime_deps = [\":com_example__mylib\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withMultipleDeps()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
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

      {
        final ArtifactRecord artifactRecord = getArtifactAt( record, 0 );
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
        assertEquals( asString( outputStream ),
                      "native.java_import(\n" +
                      "    name = \"com_example__myapp__1_0\",\n" +
                      "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                      "    licenses = [\"notice\"],\n" +
                      "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                      "    visibility = [\"//visibility:private\"],\n" +
                      "    deps = [\":com_example__mylib\"],\n" +
                      "    runtime_deps = [\":com_example__rta\"],\n" +
                      ")\n" );
      }
      {
        final ArtifactRecord artifactRecord = getArtifactAt( record, 1 );
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
        assertEquals( asString( outputStream ),
                      "native.java_import(\n" +
                      "    name = \"com_example__mylib__1_0\",\n" +
                      "    jars = [\"@com_example__mylib__1_0//file\"],\n" +
                      "    licenses = [\"notice\"],\n" +
                      "    tags = [\"maven_coordinates=com.example:mylib:1.0\"],\n" +
                      "    visibility = [\"//visibility:private\"],\n" +
                      "    runtime_deps = [\":com_example__rtb\"],\n" +
                      ")\n" );
      }
      {
        final ArtifactRecord artifactRecord = getArtifactAt( record, 2 );
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport( new StarlarkFileOutput( outputStream ), "" );
        assertEquals( asString( outputStream ),
                      "native.java_import(\n" +
                      "    name = \"com_example__rta__33_0\",\n" +
                      "    jars = [\"@com_example__rta__33_0//file\"],\n" +
                      "    licenses = [\"notice\"],\n" +
                      "    tags = [\"maven_coordinates=com.example:rtA:33.0\"],\n" +
                      "    visibility = [\"//visibility:private\"],\n" +
                      ")\n" );
      }
    } );
  }

  @Test
  public void emitAlias()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitAlias( new StarlarkFileOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__myapp\",\n" +
                    "    actual = \":com_example__myapp__1_0\",\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitAlias_VisibilitySpecified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    visibility: ['//some/package:__pkg__', '//other/package:__subpackages__']\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitAlias( new StarlarkFileOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__myapp\",\n" +
                    "    actual = \":com_example__myapp__1_0\",\n" +
                    "    visibility = [\n" +
                    "        \"//some/package:__pkg__\",\n" +
                    "        \"//other/package:__subpackages__\",\n" +
                    "    ],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitAlias_AliasSpecified()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    alias: my_super_dooper_app\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitAlias( new StarlarkFileOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"my_super_dooper_app\",\n" +
                    "    actual = \":com_example__myapp__1_0\",\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitAlias_forUndeclaredDependency()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 1 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitAlias( new StarlarkFileOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__mylib\",\n" +
                    "    actual = \":com_example__mylib__1_0\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Nonnull
  private String asString( @Nonnull final ByteArrayOutputStream outputStream )
  {
    return new String( outputStream.toByteArray(), StandardCharsets.US_ASCII );
  }

  @Nonnull
  private ArtifactRecord getArtifactAt( @Nonnull final ApplicationRecord record, final int index )
  {
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertTrue( artifacts.size() > index, "At least " + ( index + 1 ) + " artifacts present." );
    return artifacts.get( index );
  }
}
