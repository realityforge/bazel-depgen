package org.realityforge.bazel.depgen.record;

import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.DepGenConfig;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.util.StarlarkOutput;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArtifactRecordTest
  extends AbstractTest
{
  @Test
  public void parseSimpleArtifact()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    assertNotNull( artifactRecord.getArtifactModel() );
    assertEquals( artifactRecord.getKey(), "com.example:myapp" );
    assertEquals( artifactRecord.getBaseName(), "com_example__myapp__1_0" );
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
  public void emitJavaImport_simpleArtifact()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" );
  }


  @Test
  public void emitJavaImport_simpleArtifact_visibilitySpecified()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    visibility: ['//some/package:__pkg__', '//other/package:__subpackages__']\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    visibility = [\n" +
                  "        \"//some/package:__pkg__\",\n" +
                  "        \"//other/package:__subpackages__\",\n" +
                  "    ],\n" +
                  ")\n" );
  }
  @Test
  public void emitJavaImport_simpleArtifact_withNamePrefix()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  namePrefix: zeapp\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"zeapp_com_example__myapp\",\n" +
                  "    jars = [\"@zeapp_com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@zeapp_com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withNameSuffix()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "__library" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp__library\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withSourceJar()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withDep()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    deps = [\":com_example__mylib\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withExportDeps()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "options:\n" +
                          "  java:\n" +
                          "    exportDeps: true\n" +
                          "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    deps = [\":com_example__mylib\"],\n" +
                  "    exports = [\":com_example__mylib\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_declaredDepgenArtifact()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: " + DepGenConfig.getCoord() + "\n" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    // Output does not declare data with verify task included
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"org_realityforge_bazel_depgen__bazel_depgen\",\n" +
                  "    jars = [\"@org_realityforge_bazel_depgen__bazel_depgen__1//file\"],\n" +
                  "    srcjar = \"@org_realityforge_bazel_depgen__bazel_depgen__1__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_shouldExportDeps()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    java:\n" +
                     "      exportDeps: true\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    deps = [\":com_example__mylib\"],\n" +
                  "    exports = [\":com_example__mylib\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withRuntimeDep()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:runtime" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  "    runtime_deps = [\":com_example__mylib\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaImport_simpleArtifact_withMultipleDeps()
    throws Exception
  {
    // Provided ignored by traversal
    // System collected but ignored at later stage
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
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

    {
      final ArtifactRecord artifactRecord = getArtifactAt( record, 0 );
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "_java_import(\n" +
                    "    name = \"com_example__myapp\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    "    deps = [\":com_example__mylib\"],\n" +
                    "    runtime_deps = [\":com_example__rta\"],\n" +
                    ")\n" );
    }
    {
      final ArtifactRecord artifactRecord = getArtifactAt( record, 1 );
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "_java_import(\n" +
                    "    name = \"com_example__mylib\",\n" +
                    "    jars = [\"@com_example__mylib__1_0//file\"],\n" +
                    "    srcjar = \"@com_example__mylib__1_0__sources//file\",\n" +
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
                    "_java_import(\n" +
                    "    name = \"com_example__rta\",\n" +
                    "    jars = [\"@com_example__rta__33_0//file\"],\n" +
                    "    srcjar = \"@com_example__rta__33_0__sources//file\",\n" +
                    "    tags = [\"maven_coordinates=com.example:rtA:33.0\"],\n" +
                    "    visibility = [\"//visibility:private\"],\n" +
                    ")\n" );
    }
  }

  @Test
  public void getNameStrategy_implicit()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    assertEquals( artifactRecord.getNameStrategy(), NameStrategy.GroupIdAndArtifactId );
    assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
  }

  @Test
  public void getNameStrategy_locallySpecified_GroupIdAndArtifactId()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    nameStrategy: GroupIdAndArtifactId\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    assertEquals( artifactRecord.getNameStrategy(), NameStrategy.GroupIdAndArtifactId );
    assertEquals( artifactRecord.getName( Nature.Java ), "com_example__myapp" );
  }

  @Test
  public void getNameStrategy_locallySpecified_ArtifactId()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  nameStrategy: ArtifactId\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    assertEquals( artifactRecord.getNameStrategy(), NameStrategy.ArtifactId );
    assertEquals( artifactRecord.getName( Nature.Java ), "myapp" );
  }

  @Test
  public void getNameStrategy_globallySpecified_ArtifactId()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    nameStrategy: ArtifactId\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    assertEquals( artifactRecord.getNameStrategy(), NameStrategy.ArtifactId );
    assertEquals( artifactRecord.getName( Nature.Java ), "myapp" );
  }

  @Test
  public void emitJavaImport_nameOverrides()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [Java, J2cl, Plugin]\n" +
                     "    java:\n" +
                     "      name: myapp-java-a\n" +
                     "    j2cl:\n" +
                     "      name: myapp-j2cl-a\n" +
                     "    plugin:\n" +
                     "      name: myapp-plugin-a\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "_java_import(\n" +
                    "    name = \"myapp-java-a\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    ")\n" );
    }
    {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "_java_import(\n" +
                    "    name = \"myapp-java-a\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    ")\n" );
    }
    {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      artifactRecord.emitJavaImport( new StarlarkOutput( outputStream ), "" );
      assertEquals( asString( outputStream ),
                    "_java_import(\n" +
                    "    name = \"myapp-java-a\",\n" +
                    "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                    "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                    "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                    ")\n" );
    }
  }

  @Test
  public void emitJavaPlugin_nullProcessor()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaPlugin( new StarlarkOutput( outputStream ), null );
    assertEquals( asString( outputStream ),
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__plugin\",\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaPlugin_withProcessor()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "arez.processor.ArezProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaPlugin( new StarlarkOutput( outputStream ), "arez.processor.ArezProcessor" );
    assertEquals( asString( outputStream ),
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                  "    generates_api = True,\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" );
  }

  @Test
  public void emitJavaPlugin_withProcessorNoGeneratesApi()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" +
                          "    plugin:\n" +
                          "      generatesApi: false\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "arez.processor.ArezProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.emitJavaPlugin( new StarlarkOutput( outputStream ), "arez.processor.ArezProcessor" );
    assertEquals( asString( outputStream ),
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeJ2clLibrary()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJ2clLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeJ2clLibrary_suppressPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" +
                          "    j2cl:\n" +
                          "      suppress: [\"checkDebuggerStatement\"]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJ2clLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  "    js_suppress = [\"checkDebuggerStatement\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeJ2clLibrary_modeImport()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" +
                          "    j2cl:\n" +
                          "      mode: Import\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJ2clLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_import(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    jar = \"@com_example__myapp__1_0//file\",\n" +
                  ")\n" );
  }

  @Test
  public void writeJ2clLibrary_singleDepsPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0", "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJ2clLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  "    deps = [\":com_example__mylib-j2cl\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeJ2clLibrary_multipleDepsPresent()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir,
                                     "com.example:myapp:1.0",
                                     "com.example:mylib:1.0",
                                     "com.example:mylib2:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib:1.0" );
    deployArtifactToLocalRepository( dir, "com.example:mylib2:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJ2clLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  "    deps = [\n" +
                  "        \":com_example__mylib-j2cl\",\n" +
                  "        \":com_example__mylib2-j2cl\",\n" +
                  "    ],\n" +
                  ")\n" );
  }

  @Test
  public void pluginName()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    assertEquals( artifactRecord.pluginName( "arez.processor.ArezProcessor" ),
                  "com_example__myapp__1_0__arez_processor_arezprocessor__plugin" );
    assertEquals( artifactRecord.pluginName( null ), "com_example__myapp__1_0__plugin" );
  }

  @Test
  public void writePluginLibrary_withProcessors()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "arez.processor.ArezProcessor\n" +
                     "react4j.processor.ReactProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writePluginLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp__plugin_library\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" +
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                  "    generates_api = True,\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" +
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                  "    processor_class = \"react4j.processor.ReactProcessor\",\n" +
                  "    generates_api = True,\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" +
                  "_java_library(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    exported_plugins = [\n" +
                  "        \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "        \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                  "    ],\n" +
                  ")\n" );
  }

  @Test
  public void writePluginLibrary_withNoProcessors()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writePluginLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp__plugin_library\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" +
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__plugin\",\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" +
                  "_java_library(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                  ")\n" );
  }

  @Test
  public void writePluginLibrary_withMultipleNatures()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin, Java]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writePluginLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp__plugin_library\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" +
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__plugin\",\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" +
                  "_java_library(\n" +
                  "    name = \"com_example__myapp-plugin\",\n" +
                  "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeJavaPluginLibrary_withProcessors()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "arez.processor.ArezProcessor\n" +
                     "react4j.processor.ReactProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJavaPluginLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_library(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    exported_plugins = [\n" +
                  "        \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "        \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                  "    ],\n" +
                  ")\n" );
  }

  @Test
  public void writeJavaPluginLibrary_withNoProcessors()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJavaPluginLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_library(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeJavaPluginLibrary_withMultipleNatures()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [Plugin, Java]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeJavaPluginLibrary( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_library(\n" +
                  "    name = \"com_example__myapp-plugin\",\n" +
                  "    exported_plugins = [\"com_example__myapp__1_0__plugin\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactHttpFileRule()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    final List<String> urls = artifactRecord.getUrls();
    assertNotNull( urls );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactHttpFileRule( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_http_file(\n" +
                  "    name = \"com_example__myapp__1_0\",\n" +
                  "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n" +
                  "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "    urls = [\"" + urls.get( 0 ) + "\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactSourcesHttpFileRule()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    final List<String> urls = artifactRecord.getUrls();
    assertNotNull( urls );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactSourcesHttpFileRule( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_http_file(\n" +
                  "    name = \"com_example__myapp__1_0__sources\",\n" +
                  "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n" +
                  "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "    urls = [\"" + urls.get( 0 ).replace( ".jar", "-sources.jar" ) + "\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactAnnotationsHttpFileRule()
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

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );
    final List<String> urls = artifactRecord.getUrls();
    assertNotNull( urls );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactAnnotationsHttpFileRule( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_http_file(\n" +
                  "    name = \"com_example__myapp__1_0__annotations\",\n" +
                  "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-annotations.jar\",\n" +
                  "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n" +
                  "    urls = [\"" + urls.get( 0 ).replace( ".jar", "-annotations.jar" ) + "\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactTargets_Library()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactTargets( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactTargets_Plugin()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" );
    final Path jarFile =
      createJarFile( "META-INF/services/javax.annotation.processing.Processor",
                     "arez.processor.ArezProcessor\n" +
                     "react4j.processor.ReactProcessor\n" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:jar:sources:1.0" );
    deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0", jarFile );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactTargets( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_java_import(\n" +
                  "    name = \"com_example__myapp__plugin_library\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" +
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "    processor_class = \"arez.processor.ArezProcessor\",\n" +
                  "    generates_api = True,\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" +
                  "_java_plugin(\n" +
                  "    name = \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                  "    processor_class = \"react4j.processor.ReactProcessor\",\n" +
                  "    generates_api = True,\n" +
                  "    visibility = [\"//visibility:private\"],\n" +
                  "    deps = [\":com_example__myapp__plugin_library\"],\n" +
                  ")\n" +
                  "_java_library(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    exported_plugins = [\n" +
                  "        \"com_example__myapp__1_0__arez_processor_arezprocessor__plugin\",\n" +
                  "        \"com_example__myapp__1_0__react4j_processor_reactprocessor__plugin\",\n" +
                  "    ],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactTargets_J2cl()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactTargets( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactTargets_J2cl_no_verify_config_sha256()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir,
                     "options:\n" +
                     "  verifyConfigSha256: false\n" +
                     "artifacts:\n" +
                     "  - coord: com.example:myapp:1.0\n" +
                     "    natures: [J2cl]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactTargets( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  ")\n" );
  }

  @Test
  public void writeArtifactTargets_multipleNatures()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();

    writeConfigFile( dir, "artifacts:\n" +
                          "  - coord: com.example:myapp:1.0\n" +
                          "    natures: [J2cl, Java]\n" );
    deployArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

    final ArtifactRecord artifactRecord = getArtifactAt( loadApplicationRecord(), 0 );

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    artifactRecord.writeArtifactTargets( new StarlarkOutput( outputStream ) );
    assertEquals( asString( outputStream ),
                  "_j2cl_library(\n" +
                  "    name = \"com_example__myapp-j2cl\",\n" +
                  "    srcs = [\"@com_example__myapp__1_0__sources//file\"],\n" +
                  ")\n" +
                  "\n" +
                  "_java_import(\n" +
                  "    name = \"com_example__myapp\",\n" +
                  "    jars = [\"@com_example__myapp__1_0//file\"],\n" +
                  "    srcjar = \"@com_example__myapp__1_0__sources//file\",\n" +
                  "    tags = [\"maven_coordinates=com.example:myapp:1.0\"],\n" +
                  ")\n" );
  }

  @Nonnull
  private ArtifactRecord getArtifactAt( @Nonnull final ApplicationRecord record, final int index )
  {
    final List<ArtifactRecord> artifacts = record.getArtifacts();
    assertTrue( artifacts.size() > index, "At least " + ( index + 1 ) + " artifacts present." );
    return artifacts.get( index );
  }
}
