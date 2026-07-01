package org.realityforge.bazel.depgen.record;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.DepGenConfig;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.util.StarlarkOutput;
import org.testng.annotations.Test;

public class ArtifactRecordTest extends AbstractTest {
    @Test
    public void parseSimpleArtifact() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        assertNotNull(artifactRecord.getArtifactModel());
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getBaseName(), "com_example__myapp__1_0");
        assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
        assertTrue(artifactRecord.generatesApi());
        assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
        assertEquals(artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
        assertEquals(
                artifactRecord.getUrls(),
                Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
        assertEquals(
                artifactRecord.getSourceSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
        assertEquals(
                artifactRecord.getSourceUrls(),
                Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar"));
        assertEquals(artifactRecord.getDeps().size(), 0);
        assertEquals(artifactRecord.getReverseDeps().size(), 0);
        assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        assertEquals(artifactRecord.getReverseRuntimeDeps().size(), 0);
    }

    @Test
    public void emitJavaImport_simpleArtifact() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_visibilitySpecified() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                visibility: ['//some/package:__pkg__', '//other/package:__subpackages__']
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
                visibility = [
                    "//some/package:__pkg__",
                    "//other/package:__subpackages__",
                ],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withNamePrefix() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: zeapp
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "zeapp_com_example__myapp",
                jars = ["@zeapp_com_example__myapp__1_0//file"],
                srcjar = "@zeapp_com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withNameSuffix() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "__library");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp__library",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withSourceJar() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withDep() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
                deps = [":com_example__mylib"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withExportDeps() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              java:
                exportDeps: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
                deps = [":com_example__mylib"],
                exports = [":com_example__mylib"],
            )
            """);
    }

    @Test
    public void emitJavaImport_declaredDepgenArtifact() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: " + DepGenConfig.getCoord() + "\n");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        // Output does not declare data with verify task included
        assertEquals(asString(outputStream), """
            _java_import(
                name = "org_realityforge_bazel_depgen__bazel_depgen",
                jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                srcjar = "@org_realityforge_bazel_depgen__bazel_depgen__1__sources//file",
                tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
            )
            """);
    }

    @Test
    public void emitJavaImport_shouldExportDeps() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                java:
                  exportDeps: true
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
                deps = [":com_example__mylib"],
                exports = [":com_example__mylib"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withRuntimeDep() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
                runtime_deps = [":com_example__mylib"],
            )
            """);
    }

    @Test
    public void emitJavaImport_simpleArtifact_withMultipleDeps() throws Exception {
        // Provided ignored by traversal
        // System collected but ignored at later stage
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(
                dir, "com.example:myapp:1.0", "com.example:mylib:1.0", "com.example:rtA:jar::33.0:runtime");
        deployArtifactToLocalRepository(
                dir, "com.example:mylib:1.0", "com.example:rtB:jar::2.0:runtime", "org.test4j:core:jar::44.0:test");
        deployArtifactToLocalRepository(dir, "com.example:rtA:33.0");
        deployArtifactToLocalRepository(
                dir,
                "com.example:rtB:2.0",
                // Provided ignored by traversal
                "com.example:container:jar::4.0:provided",
                // System collected but ignored at later stage
                "com.example:kernel:jar::4.0:system");

        final ApplicationRecord record = loadApplicationRecord();

        {
            final ArtifactRecord artifactRecord = getArtifactAt(record, 0);
            final var outputStream = new ByteArrayOutputStream();
            artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
            assertEquals(asString(outputStream), """
                _java_import(
                    name = "com_example__myapp",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                    runtime_deps = [":com_example__rta"],
                    deps = [":com_example__mylib"],
                )
                """);
        }
        {
            final ArtifactRecord artifactRecord = getArtifactAt(record, 1);
            final var outputStream = new ByteArrayOutputStream();
            artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
            assertEquals(asString(outputStream), """
                _java_import(
                    name = "com_example__mylib",
                    jars = ["@com_example__mylib__1_0//file"],
                    srcjar = "@com_example__mylib__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:mylib:1.0"],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":com_example__rtb"],
                )
                """);
        }
        {
            final ArtifactRecord artifactRecord = getArtifactAt(record, 2);
            final var outputStream = new ByteArrayOutputStream();
            artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
            assertEquals(asString(outputStream), """
                _java_import(
                    name = "com_example__rta",
                    jars = ["@com_example__rta__33_0//file"],
                    srcjar = "@com_example__rta__33_0__sources//file",
                    tags = ["maven_coordinates=com.example:rtA:33.0"],
                    visibility = ["//visibility:private"],
                )
                """);
        }
    }

    @Test
    public void getNameStrategy_implicit() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        assertEquals(artifactRecord.getNameStrategy(), NameStrategy.GroupIdAndArtifactId);
        assertEquals(artifactRecord.getRepositoryNameStrategy(), NameStrategy.GroupIdAndArtifactIdAndVersion);
        assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
        assertEquals(artifactRecord.getRepositoryBaseName(), "com_example__myapp__1_0");
    }

    @Test
    public void getNameStrategy_locallySpecified_GroupIdAndArtifactId() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                nameStrategy: GroupIdAndArtifactId
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        assertEquals(artifactRecord.getNameStrategy(), NameStrategy.GroupIdAndArtifactId);
        assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
    }

    @Test
    public void getNameStrategy_locallySpecified_ArtifactId() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        assertEquals(artifactRecord.getNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(artifactRecord.getName(Nature.Java), "myapp");
        assertEquals(artifactRecord.getRepositoryBaseName(), "com_example__myapp__1_0");
    }

    @Test
    public void getNameStrategy_locallySpecified_GroupIdAndArtifactIdAndVersion() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                nameStrategy: GroupIdAndArtifactIdAndVersion
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        assertEquals(artifactRecord.getNameStrategy(), NameStrategy.GroupIdAndArtifactIdAndVersion);
        assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp__1_0");
    }

    @Test
    public void getRepositoryNameStrategy_globallySpecified_ArtifactId() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              repositoryNameStrategy: ArtifactId
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        assertEquals(artifactRecord.getNameStrategy(), NameStrategy.GroupIdAndArtifactId);
        assertEquals(artifactRecord.getRepositoryNameStrategy(), NameStrategy.ArtifactId);
        assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
        assertEquals(artifactRecord.getRepositoryBaseName(), "myapp");
    }

    @Test
    public void getRepositoryBaseName_explicitRepositoryNameBypassesPrefix() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: myapp
            artifacts:
              - coord: com.example:myapp:1.0
                repositoryName: custom_repo
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        assertEquals(artifactRecord.getName(Nature.Java), "myapp_com_example__myapp");
        assertEquals(artifactRecord.getRepositoryBaseName(), "custom_repo");
    }

    @Test
    public void emitJavaImport_nameOverrides() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java, J2cl, Plugin]
                java:
                  name: myapp-java-a
                j2cl:
                  name: myapp-j2cl-a
                plugin:
                  name: myapp-plugin-a
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        {
            final var outputStream = new ByteArrayOutputStream();
            artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
            assertEquals(asString(outputStream), """
                _java_import(
                    name = "myapp-java-a",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )
                """);
        }
        {
            final var outputStream = new ByteArrayOutputStream();
            artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
            assertEquals(asString(outputStream), """
                _java_import(
                    name = "myapp-java-a",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )
                """);
        }
        {
            final var outputStream = new ByteArrayOutputStream();
            artifactRecord.emitJavaImport(new StarlarkOutput(outputStream), "");
            assertEquals(asString(outputStream), """
                _java_import(
                    name = "myapp-java-a",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )
                """);
        }
    }

    @Test
    public void emitJavaPlugin_nullProcessor() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaPlugin(new StarlarkOutput(outputStream), null);
        assertEquals(asString(outputStream), """
            _java_plugin(
                name = "com_example__myapp__plugin",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            """);
    }

    @Test
    public void emitJavaPlugin_withProcessor() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        final Path jarFile = createJarFile(
                "META-INF/services/javax.annotation.processing.Processor", "arez.processor.ArezProcessor\n");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaPlugin(new StarlarkOutput(outputStream), "arez.processor.ArezProcessor");
        assertEquals(asString(outputStream), """
            _java_plugin(
                name = "com_example__myapp__arez_processor_arezprocessor__plugin",
                generates_api = True,
                processor_class = "arez.processor.ArezProcessor",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            """);
    }

    @Test
    public void emitJavaPlugin_withProcessorNoGeneratesApi() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
                plugin:
                  generatesApi: false
            """);
        final Path jarFile = createJarFile(
                "META-INF/services/javax.annotation.processing.Processor", "arez.processor.ArezProcessor\n");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.emitJavaPlugin(new StarlarkOutput(outputStream), "arez.processor.ArezProcessor");
        assertEquals(asString(outputStream), """
            _java_plugin(
                name = "com_example__myapp__arez_processor_arezprocessor__plugin",
                processor_class = "arez.processor.ArezProcessor",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            """);
    }

    @Test
    public void writeJ2clLibrary() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJ2clLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
            )
            """);
    }

    @Test
    public void writeJ2clLibrary_suppressPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  suppress: ["checkDebuggerStatement"]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJ2clLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
                js_suppress = ["checkDebuggerStatement"],
            )
            """);
    }

    @Test
    public void writeJ2clLibrary_modeImport() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJ2clLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_import(
                name = "com_example__myapp-j2cl",
                jar = "@com_example__myapp__1_0//file",
            )
            """);
    }

    @Test
    public void writeJ2clLibrary_singleDepsPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJ2clLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
                deps = [":com_example__mylib-j2cl"],
            )
            """);
    }

    @Test
    public void writeJ2clLibrary_multipleDepsPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(
                dir, "com.example:myapp:1.0", "com.example:mylib:1.0", "com.example:mylib2:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib2:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJ2clLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
                deps = [
                    ":com_example__mylib-j2cl",
                    ":com_example__mylib2-j2cl",
                ],
            )
            """);
    }

    @Test
    public void pluginName() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        assertEquals(
                artifactRecord.pluginName("arez.processor.ArezProcessor"),
                "com_example__myapp__arez_processor_arezprocessor__plugin");
        assertEquals(artifactRecord.pluginName(null), "com_example__myapp__plugin");
    }

    @Test
    public void writePluginLibrary_withProcessors() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            arez.processor.ArezProcessor
            react4j.processor.ReactProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writePluginLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp__plugin_library",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            _java_plugin(
                name = "com_example__myapp__arez_processor_arezprocessor__plugin",
                generates_api = True,
                processor_class = "arez.processor.ArezProcessor",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            _java_plugin(
                name = "com_example__myapp__react4j_processor_reactprocessor__plugin",
                generates_api = True,
                processor_class = "react4j.processor.ReactProcessor",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            _java_library(
                name = "com_example__myapp",
                exported_plugins = [
                    "com_example__myapp__arez_processor_arezprocessor__plugin",
                    "com_example__myapp__react4j_processor_reactprocessor__plugin",
                ],
            )
            """);
    }

    @Test
    public void writePluginLibrary_withNoProcessors() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writePluginLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp__plugin_library",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            _java_plugin(
                name = "com_example__myapp__plugin",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            _java_library(
                name = "com_example__myapp",
                exported_plugins = ["com_example__myapp__plugin"],
            )
            """);
    }

    @Test
    public void writePluginLibrary_withMultipleNatures() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin, Java]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writePluginLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp__plugin_library",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            _java_plugin(
                name = "com_example__myapp__plugin",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            _java_library(
                name = "com_example__myapp-plugin",
                exported_plugins = ["com_example__myapp__plugin"],
            )
            """);
    }

    @Test
    public void writeJavaPluginLibrary_withProcessors() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            arez.processor.ArezProcessor
            react4j.processor.ReactProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJavaPluginLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_library(
                name = "com_example__myapp",
                exported_plugins = [
                    "com_example__myapp__arez_processor_arezprocessor__plugin",
                    "com_example__myapp__react4j_processor_reactprocessor__plugin",
                ],
            )
            """);
    }

    @Test
    public void writeJavaPluginLibrary_withNoProcessors() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJavaPluginLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_library(
                name = "com_example__myapp",
                exported_plugins = ["com_example__myapp__plugin"],
            )
            """);
    }

    @Test
    public void writeJavaPluginLibrary_withMultipleNatures() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin, Java]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeJavaPluginLibrary(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_library(
                name = "com_example__myapp-plugin",
                exported_plugins = ["com_example__myapp__plugin"],
            )
            """);
    }

    @Test
    public void writeArtifactHttpFileRule() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        final List<String> urls = requireNonNull(artifactRecord.getUrls());

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactHttpFileRule(new StarlarkOutput(outputStream));
        assertEquals(
                asString(outputStream),
                "_http_file(\n" + "    name = \"com_example__myapp__1_0\",\n"
                        + "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0.jar\",\n"
                        + "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                        + "    urls = [\""
                        + urls.get(0) + "\"],\n" + ")\n");
    }

    @Test
    public void writeArtifactSourcesHttpFileRule() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        final List<String> urls = requireNonNull(artifactRecord.getUrls());

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactSourcesHttpFileRule(new StarlarkOutput(outputStream));
        assertEquals(
                asString(outputStream),
                "_http_file(\n" + "    name = \"com_example__myapp__1_0__sources\",\n"
                        + "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-sources.jar\",\n"
                        + "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                        + "    urls = [\""
                        + urls.get(0).replace(".jar", "-sources.jar") + "\"],\n" + ")\n");
    }

    @Test
    public void writeArtifactAnnotationsHttpFileRule() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeExternalAnnotations: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:annotations:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        final List<String> urls = requireNonNull(artifactRecord.getUrls());

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactAnnotationsHttpFileRule(new StarlarkOutput(outputStream));
        assertEquals(
                asString(outputStream),
                "_http_file(\n" + "    name = \"com_example__myapp__1_0__annotations\",\n"
                        + "    downloaded_file_path = \"com/example/myapp/1.0/myapp-1.0-annotations.jar\",\n"
                        + "    sha256 = \"e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4\",\n"
                        + "    urls = [\""
                        + urls.get(0).replace(".jar", "-annotations.jar") + "\"],\n" + ")\n");
    }

    @Test
    public void writeArtifactJsSourcesHttpArchiveRule_extensionStyle() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        final Path sourceJar = createJarFile("foo.js", "");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", sourceJar);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        final List<String> sourceUrls = requireNonNull(artifactRecord.getSourceUrls());

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactJsSourcesHttpArchiveRule(new StarlarkOutput(outputStream));
        assertEquals(
                asString(outputStream),
                "_http_archive(\n" + "    name = \"com_example__myapp__1_0__js_sources\",\n"
                        + "    sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n"
                        + "    urls = [\""
                        + sourceUrls.get(0) + "\"],\n" + "    build_file_content = \"\"\"\n"
                        + "filegroup(\n"
                        + "    name = \"srcs\",\n"
                        + "    visibility = [\"//visibility:public\"],\n"
                        + "    srcs = [\"foo.js\"],\n"
                        + ")\n"
                        + "\"\"\",\n"
                        + ")\n");
    }

    @Test
    public void writeArtifactJsSourcesHttpArchiveRule_moduleStyle() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        final Path sourceJar = createJarFile("foo.js", "");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", sourceJar);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);
        final List<String> sourceUrls = requireNonNull(artifactRecord.getSourceUrls());

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactJsSourcesHttpArchiveRule(new StarlarkOutput(outputStream));
        assertEquals(
                asString(outputStream),
                "_http_archive(\n" + "    name = \"com_example__myapp__1_0__js_sources\",\n"
                        + "    sha256 = \"94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d\",\n"
                        + "    urls = [\""
                        + sourceUrls.get(0) + "\"],\n" + "    build_file_content = \"\"\"\n"
                        + "filegroup(\n"
                        + "    name = \"srcs\",\n"
                        + "    visibility = [\"//visibility:public\"],\n"
                        + "    srcs = [\"foo.js\"],\n"
                        + ")\n"
                        + "\"\"\",\n"
                        + ")\n");
    }

    @Test
    public void writeArtifactTargets_Library() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactTargets(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            """);
    }

    @Test
    public void writeArtifactTargets_Plugin() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            arez.processor.ArezProcessor
            react4j.processor.ReactProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactTargets(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _java_import(
                name = "com_example__myapp__plugin_library",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            _java_plugin(
                name = "com_example__myapp__arez_processor_arezprocessor__plugin",
                generates_api = True,
                processor_class = "arez.processor.ArezProcessor",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            _java_plugin(
                name = "com_example__myapp__react4j_processor_reactprocessor__plugin",
                generates_api = True,
                processor_class = "react4j.processor.ReactProcessor",
                visibility = ["//visibility:private"],
                deps = [":com_example__myapp__plugin_library"],
            )
            _java_library(
                name = "com_example__myapp",
                exported_plugins = [
                    "com_example__myapp__arez_processor_arezprocessor__plugin",
                    "com_example__myapp__react4j_processor_reactprocessor__plugin",
                ],
            )
            """);
    }

    @Test
    public void writeArtifactTargets_J2cl() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactTargets(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
            )
            """);
    }

    @Test
    public void writeArtifactTargets_J2cl_no_verify_config_sha256() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              verifyConfigSha256: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactTargets(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
            )
            """);
    }

    @Test
    public void writeArtifactTargets_multipleNatures() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl, Java]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifactRecord = getArtifactAt(loadApplicationRecord(), 0);

        final var outputStream = new ByteArrayOutputStream();
        artifactRecord.writeArtifactTargets(new StarlarkOutput(outputStream));
        assertEquals(asString(outputStream), """
            _j2cl_library(
                name = "com_example__myapp-j2cl",
                srcs = ["@com_example__myapp__1_0__sources//file"],
            )

            _java_import(
                name = "com_example__myapp",
                jars = ["@com_example__myapp__1_0//file"],
                srcjar = "@com_example__myapp__1_0__sources//file",
                tags = ["maven_coordinates=com.example:myapp:1.0"],
            )
            """);
    }

    @NonNull
    private ArtifactRecord getArtifactAt(@NonNull final ApplicationRecord record, final int index) {
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertTrue(artifacts.size() > index, "At least " + (index + 1) + " artifacts present.");
        return artifacts.get(index);
    }
}
