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
import org.realityforge.bazel.depgen.gen.StarlarkOutput;
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "__library" );
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
        artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
        artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
        artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
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
      artifactRecord.emitAlias( new StarlarkOutput( outputStream ) );
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
      artifactRecord.emitAlias( new StarlarkOutput( outputStream ) );
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
      artifactRecord.emitAlias( new StarlarkOutput( outputStream ) );
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
      artifactRecord.emitAlias( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__mylib\",\n" +
                    "    actual = \":com_example__mylib__1_0\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaPlugin_nullProcessor()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaPlugin( new StarlarkOutput( outputStream ), null );
      assertEquals( asString( outputStream ),
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__plugin\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaPlugin_withProcessor()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaPlugin( new StarlarkOutput( outputStream ), "arez.processor.ArezProcessor" );
      assertEquals( asString( outputStream ),
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                    "    generates_api = True,\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaPlugin_withProcessorNoGeneratesApi()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" +
                              "    generatesApi: false\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "arez.processor.ArezProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaPlugin( new StarlarkOutput( outputStream ), "arez.processor.ArezProcessor" );
      assertEquals( asString( outputStream ),
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void pluginName()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      assertEquals( artifactRecord.pluginName( "arez.processor.ArezProcessor" ),
                    "com_example__myapp__1_0__arez_processor_arezprocessor__plugin" );
      assertEquals( artifactRecord.pluginName( null ), "com_example__myapp__1_0__plugin" );
    } );
  }

  @Test
  public void emitPluginLibrary_withProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "arez.processor.ArezProcessor\n" +
                       "react4j.processor.ReactProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitPluginLibrary( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0__plugin_library\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                    "    generates_api = True,\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                    "    processor_class = \"react4j.processor.ReactProcessor\",\n" +
                    "    generates_api = True,\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exported_plugins = [\n" +
                    "        \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "        \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                    "    ],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitPluginLibrary_withNoProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitPluginLibrary( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0__plugin_library\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__plugin\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitPluginLibrary_withSuffix()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitPluginLibrary( new StarlarkOutput( outputStream ), "__plugins" );
      assertEquals( asString( outputStream ),
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0__plugin_library\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__plugin\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0__plugins\",\n" +
                    "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaPluginLibrary_withProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "arez.processor.ArezProcessor\n" +
                       "react4j.processor.ReactProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaPluginLibrary( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exported_plugins = [\n" +
                    "        \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "        \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                    "    ],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaPluginLibrary_withNoProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaPluginLibrary( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaPluginLibrary_withSuffix()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: Plugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaPluginLibrary( new StarlarkOutput( outputStream ), "__plugins" );
      assertEquals( asString( outputStream ),
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0__plugins\",\n" +
                    "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitJavaLibraryAndPlugin()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: LibraryAndPlugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaLibraryAndPlugin( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exports = [\n" +
                    "        \"com_example__myapp__1_0__plugin_library\",\n" +
                    "        \"com_example__myapp__1_0__plugins\",\n" +
                    "    ],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitArtifactHttpFileRule()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
      final List<String> urls = artifactRecord.getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitArtifactHttpFileRule( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "http_file(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                    "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "    urls = [\"" + urls.get( 0 ) + "\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitArtifactSourcesHttpFileRule()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
      final List<String> urls = artifactRecord.getUrls();
      assertNotNull( urls );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitArtifactSourcesHttpFileRule( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "http_file(\n" +
                    "    name = \"com_example__myapp__1_0__sources\",\n" +
                    "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                    "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                    "    urls = [\"" + urls.get( 0 ).replace( ".jar", "-sources.jar" ) + "\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitArtifactTargets_Library()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitArtifactTargets( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__myapp\",\n" +
                    "    actual = \":com_example__myapp__1_0\",\n" +
                    ")\n" +
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
  public void emitArtifactTargets_Plugin()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" );
      final Path jarFile =
        createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                       "arez.processor.ArezProcessor\n" +
                       "react4j.processor.ReactProcessor\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitArtifactTargets( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__myapp\",\n" +
                    "    actual = \":com_example__myapp__1_0\",\n" +
                    ")\n" +
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0__plugin_library\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                    "    generates_api = True,\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                    "    processor_class = \"react4j.processor.ReactProcessor\",\n" +
                    "    generates_api = True,\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exported_plugins = [\n" +
                    "        \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                    "        \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                    "    ],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Test
  public void emitArtifactTargets_LibraryAndPlugin_noProcessors()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      writeDependencies( dir, "artifacts:\n" +
                              "  - coord: com.example:myapp:1.0\n" +
                              "    nature: LibraryAndPlugin\n" );
      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitArtifactTargets( new StarlarkOutput( outputStream ) );
      assertEquals( asString( outputStream ),
                    "native.alias(\n" +
                    "    name = \"com_example__myapp\",\n" +
                    "    actual = \":com_example__myapp__1_0\",\n" +
                    ")\n" +
                    "native.java_import(\n" +
                    "    name = \"com_example__myapp__1_0__plugin_library\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    licenses = [\"notice\"],\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" +
                    "native.java_plugin(\n" +
                    "    name = \"com_example__myapp__1_0__plugin\",\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    "    deps = [\":com_example__myapp__1_0__plugin_library\"],\n" +
                    ")\n" +
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0__plugins\",\n" +
                    "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" +
                    "native.java_library(\n" +
                    "    name = \"com_example__myapp__1_0\",\n" +
                    "    exports = [\n" +
                    "        \"com_example__myapp__1_0__plugin_library\",\n" +
                    "        \"com_example__myapp__1_0__plugins\",\n" +
                    "    ],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    } );
  }

  @Nonnull
  private ArtifactRecord getArtifactAt( @Nonnull final ApplicationRecord record, final int index )
  {
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertTrue( artifacts.size() > index, "At least " + ( index + 1 ) + " artifacts present." );
    return artifacts.get( index );
  }
}
