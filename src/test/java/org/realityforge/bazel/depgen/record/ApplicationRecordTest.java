package org.realityforge.bazel.depgen.record;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class ApplicationRecordTest extends AbstractTest {
    @Test
    public void getPathFromExtensionToConfig() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertEquals(record.getPathFromExtensionToConfig(), Paths.get(ApplicationConfig.FILENAME));
    }

    @Test
    public void getPathFromExtensionToConfig_nonStandardExtensionFile() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              extensionFile: workspaceDir/vendor/workspace.bzl
            """);

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertEquals(record.getPathFromExtensionToConfig(), Paths.get("../../" + ApplicationConfig.FILENAME));
    }

    @Test
    public void build_simple_noDeps() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNotNull(record.getNode());

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertTrue(record.getAuthenticationContexts().isEmpty());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertNotNull(artifactRecord.getArtifactModel());
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
        assertTrue(artifactRecord.generatesApi());
        assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
        assertEquals(artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
        assertEquals(
                artifactRecord.getUrls(),
                Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
        assertEquals(artifactRecord.getDeps().size(), 0);
        assertEquals(artifactRecord.getReverseDeps().size(), 0);
        assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        assertEquals(artifactRecord.getReverseRuntimeDeps().size(), 0);
    }

    @Test
    public void build_artifact_with_annotations() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeExternalAnnotations: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:annotations:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertTrue(record.getSource().getOptions().includeExternalAnnotations());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(
                artifactRecord.getExternalAnnotationSha256(),
                "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
        assertEquals(
                artifactRecord.getExternalAnnotationUrls(),
                Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0-annotations.jar"));
    }

    @Test
    public void build_artifact_without_annotations_due_to_defaults() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:annotations:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertFalse(record.getSource().getOptions().includeExternalAnnotations());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertNull(artifactRecord.getExternalAnnotationSha256());
        assertNull(artifactRecord.getExternalAnnotationUrls());
    }

    @Test
    public void build_artifact_without_annotations_due_to_GlobalOverride() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeExternalAnnotations: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:annotations:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertFalse(record.getSource().getOptions().includeExternalAnnotations());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertNull(artifactRecord.getExternalAnnotationSha256());
        assertNull(artifactRecord.getExternalAnnotationUrls());
    }

    @Test
    public void build_artifact_without_annotations_due_to_ArtifactOverride() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeExternalAnnotations: true
            artifacts:
              - coord: com.example:myapp:1.0
                includeExternalAnnotations: false
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:annotations:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertTrue(record.getSource().getOptions().includeExternalAnnotations());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertNull(artifactRecord.getExternalAnnotationSha256());
        assertNull(artifactRecord.getExternalAnnotationUrls());
    }

    @Test
    public void build_artifact_without_annotations_due_to_Missing() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeExternalAnnotations: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertTrue(record.getSource().getOptions().includeExternalAnnotations());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertNull(artifactRecord.getExternalAnnotationSha256());
        assertNull(artifactRecord.getExternalAnnotationUrls());
    }

    @Test
    public void build_artifact_with_source() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNotNull(record.getNode());

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertTrue(record.getAuthenticationContexts().isEmpty());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertNotNull(artifactRecord.getArtifactModel());
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
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
    public void build_artifact_with_source_where_localInclude_overrides_global_exclude() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
                includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                includeSource: true
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNotNull(record.getNode());

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertTrue(record.getAuthenticationContexts().isEmpty());
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(
                artifactRecord.getSourceSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
        assertEquals(
                artifactRecord.getSourceUrls(),
                Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar"));
    }

    @Test
    public void build_artifact_with_source_but_global_includeSourceFalse() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
                includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 2);
        final ArtifactRecord artifactRecord1 = artifacts.get(0);
        assertNull(artifactRecord1.getSourceSha256());
        assertNull(artifactRecord1.getSourceUrls());
        final ArtifactRecord artifactRecord2 = artifacts.get(1);
        assertNull(artifactRecord2.getSourceSha256());
        assertNull(artifactRecord2.getSourceUrls());
    }

    @Test
    public void build_artifact_with_source_but_local_includeSourceFalse() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                includeSource: false
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertNull(artifactRecord.getSourceSha256());
        assertNull(artifactRecord.getSourceUrls());
    }

    @Test
    public void build_missingDirectSourceReportsDependencyPathFromNonSystemRoot() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(exception.getMessage(), """
            Unable to locate source for artifact 'com.example:myapp:jar:1.0'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.

            Dependency path:
              com.example:myapp:jar:1.0 [compile]\
            """);
    }

    @Test
    public void build_missingTransitiveSourceReportsShortestDependencyPath() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
              - coord: com.example:altapp:1.0
            replacements:
              - coord: com.example:base
                targets:
                  - target: "@vendor//:base"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:altapp:1.0", "com.example:base:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:base:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(exception.getMessage(), """
            Unable to locate source for artifact 'com.example:base:jar:1.0'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.

            Dependency path:
              com.example:altapp:jar:1.0 [compile]
              -> com.example:base:jar:1.0 [compile] TARGET OVERRIDES @vendor//:base (J2cl)\
            """);
    }

    @Test
    public void build_namePrefixPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: myapp
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getName(Nature.Java), "myapp_com_example__myapp");
        assertTrue(artifactRecord.generatesApi());
        assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
    }

    @Test
    public void build_namePrefixPresent_with_trailing_underscore() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: myapp_
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getName(Nature.Java), "myapp_com_example__myapp");
        assertTrue(artifactRecord.generatesApi());
        assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
    }

    @Test
    public void getAuthenticationContexts() throws Exception {
        final Path settingsFile = FileUtil.getCurrentDirectory().resolve("settings.xml");
        final String settingsContent = """
            <settings xmlns="http://maven.apache.org/POM/4.0.0">
              <servers>
                <server>
                  <id>my-repo</id>
                  <username>root</username>
                  <password>secret</password>
                </server>
              </servers>
            </settings>
            """;
        Files.writeString(settingsFile, settingsContent);

        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
              - name: my-repo
                url: http://my-repo.example.com/maven2
            """);
        final ApplicationRecord record = loadApplicationRecord();

        final Map<String, AuthenticationContext> contexts = record.getAuthenticationContexts();
        assertEquals(contexts.size(), 1);
        final AuthenticationContext context = requireNonNull(contexts.get("my-repo"));
        assertEquals(context.get(AuthenticationContext.USERNAME), "root");
        assertEquals(context.get(AuthenticationContext.PASSWORD), "secret");
    }

    @Test
    public void build_manyDependencies() throws Exception {
        // Provided ignored by traversal
        // System collected but ignored at later stage
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
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

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertTrue(record.getAuthenticationContexts().isEmpty());
        assertNonSystemArtifactCount(record, 4);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib,com.example:rtA,com.example:rtB");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(
                    artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getReverseDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getRuntimeDeps().get(0).getKey(), "com.example:rtA");
            assertEquals(artifactRecord.getReverseRuntimeDeps().size(), 0);
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__mylib");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getReverseDeps().size(), 1);
            assertEquals(artifactRecord.getReverseDeps().get(0).getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getRuntimeDeps().get(0).getKey(), "com.example:rtB");
            assertEquals(artifactRecord.getReverseRuntimeDeps().size(), 0);
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "rtA"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:rtA");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__rta");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtA:33.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getReverseDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            assertEquals(artifactRecord.getReverseRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getReverseRuntimeDeps().get(0).getKey(), "com.example:myapp");
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "rtB"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:rtB");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__rtb");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtB:2.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getReverseDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            assertEquals(artifactRecord.getReverseRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getReverseRuntimeDeps().get(0).getKey(), "com.example:mylib");
        }
    }

    @Test
    public void build_singleDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final String url = dir.toUri().toString();
        final String urlEncoded = url.replaceAll(":", "\\\\:");

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(
                    artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
            assertEquals(
                    artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getSourceUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar"));
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            final Path path = artifactRecord
                    .getArtifact()
                    .getFile()
                    .getParentFile()
                    .toPath()
                    .resolve(DepgenMetadata.FILENAME);
            assertEquals(
                    loadPropertiesContent(path),
                    "<default>.local.url=" + urlEncoded + "com/example/myapp/1.0/myapp-1.0.jar\n"
                            + "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n"
                            + "processors=-\n"
                            + "sources.local.url="
                            + urlEncoded + "com/example/myapp/1.0/myapp-1.0-sources.jar\n" + "sources.present=true\n"
                            + "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n");
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__mylib");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(
                    artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/mylib/1.0/mylib-1.0.jar"));
            assertEquals(
                    artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getSourceUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/mylib/1.0/mylib-1.0-sources.jar"));
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            final Path path = artifactRecord
                    .getArtifact()
                    .getFile()
                    .getParentFile()
                    .toPath()
                    .resolve(DepgenMetadata.FILENAME);
            assertEquals(
                    loadPropertiesContent(path),
                    "<default>.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0.jar\n"
                            + "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n"
                            + "processors=-\n"
                            + "sources.local.url="
                            + urlEncoded + "com/example/mylib/1.0/mylib-1.0-sources.jar\n" + "sources.present=true\n"
                            + "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n");
        }
    }

    @Test
    public void build_singleDependency_with_Sources() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final String url = dir.toUri().toString();
        final String urlEncoded = url.replaceAll(":", "\\\\:");

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final Path cacheDir = FileUtil.createLocalTempDir();
        final ApplicationRecord record = loadApplicationRecord(cacheDir);

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(
                    artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
            assertEquals(
                    artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getSourceUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar"));
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            final Path path = artifactRecord
                    .getArtifact()
                    .getFile()
                    .getParentFile()
                    .toPath()
                    .resolve(DepgenMetadata.FILENAME);
            assertEquals(
                    loadPropertiesContent(path),
                    "<default>.local.url=" + urlEncoded + "com/example/myapp/1.0/myapp-1.0.jar\n"
                            + "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n"
                            + "processors=-\n"
                            + "sources.local.url="
                            + urlEncoded + "com/example/myapp/1.0/myapp-1.0-sources.jar\n" + "sources.present=true\n"
                            + "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n");
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__mylib");
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:mylib:1.0");
            assertNull(artifactRecord.getProcessors());
            assertEquals(
                    artifactRecord.getSha256(), "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/mylib/1.0/mylib-1.0.jar"));
            assertEquals(
                    artifactRecord.getSourceSha256(),
                    "E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4");
            assertEquals(
                    artifactRecord.getSourceUrls(),
                    Collections.singletonList(dir.toUri() + "com/example/mylib/1.0/mylib-1.0-sources.jar"));
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            final Path path = artifactRecord
                    .getArtifact()
                    .getFile()
                    .getParentFile()
                    .toPath()
                    .resolve(DepgenMetadata.FILENAME);
            assertEquals(
                    loadPropertiesContent(path),
                    "<default>.local.url=" + urlEncoded + "com/example/mylib/1.0/mylib-1.0.jar\n"
                            + "<default>.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n"
                            + "processors=-\n"
                            + "sources.local.url="
                            + urlEncoded + "com/example/mylib/1.0/mylib-1.0-sources.jar\n" + "sources.present=true\n"
                            + "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n");
        }
    }

    @Test
    public void multipleDependenciesWithSameKeyOmitsSecond() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(
                dir, "com.example:myapp:1.0", "com.example:mylib:jar:sources:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 2);

        final ArtifactRecord artifactRecord = record.getArtifact("com.example", "myapp");
        final List<ArtifactRecord> deps = artifactRecord.getDeps();
        assertEquals(deps.size(), 1);
        assertEquals(deps.get(0).getArtifact().toString(), "com.example:mylib:jar:1.0");
    }

    @Test
    public void multipleDependenciesWithSameKeyInRuntimeDeps() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(
                dir,
                "com.example:myapp:1.0",
                "com.example:mylib:jar:sources:1.0:runtime",
                "com.example:mylib:jar::1.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 2);

        final ArtifactRecord artifactRecord = record.getArtifact("com.example", "myapp");
        final List<ArtifactRecord> deps = artifactRecord.getRuntimeDeps();
        assertEquals(deps.size(), 1);
        assertEquals(deps.get(0).getArtifact().toString(), "com.example:mylib:jar:1.0");
    }

    @Test
    public void propagateNature_J2cl() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 3);

        assertEquals(record.getArtifact("com.example", "myapp").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.J2cl));
    }

    @Test
    public void propagateNature_J2cl_viaDefaultNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              defaultNature: J2cl
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 3);

        assertEquals(record.getArtifact("com.example", "myapp").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.J2cl));
    }

    @Test
    public void propagateNature_J2cl_declaredTransitivelyPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
              - coord: com.example:base:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 3);

        assertEquals(record.getArtifact("com.example", "myapp").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.J2cl));
    }

    @Test
    public void propagateNature_J2cl_replacementPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
              - coord: com.example:base:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 3);

        assertEquals(record.getArtifact("com.example", "myapp").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Collections.singletonList(Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void propagateNature_J2cl_transitiveNonJ2clDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
              - coord: com.example:base:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:base:jar:1.0' does not specify the J2cl nature but is a transitive dependency"
                    + " of 'com.example:myapp:jar:1.0' which has the J2cl nature. This is not a supported scenario.");
    }

    @Test
    public void propagateNature_J2cl_directNonJ2clDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
              - coord: com.example:mylib:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:mylib:jar:1.0' does not specify the J2cl nature but is a direct dependency of"
                        + " 'com.example:myapp:jar:1.0' which has the J2cl nature. This is not a supported scenario.");
    }

    @Test
    public void propagateNature_Java() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              defaultNature: J2cl
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 3);

        assertEquals(record.getArtifact("com.example", "myapp").getNatures(), Collections.singletonList(Nature.Java));
        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Collections.singletonList(Nature.Java));
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void propagateNature_Java_directNonJavaDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              defaultNature: J2cl
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java]
              - coord: com.example:mylib:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:mylib:jar:1.0' does not specify the Java nature but is a direct dependency of"
                        + " 'com.example:myapp:jar:1.0' which has the Java nature. This is not a supported scenario.");
    }

    @Test
    public void propagateNature_Plugin() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              defaultNature: J2cl
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 3);

        assertEquals(record.getArtifact("com.example", "myapp").getNatures(), Collections.singletonList(Nature.Plugin));
        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Collections.singletonList(Nature.Java));
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void propagateNature_Plugin_directNonJavaDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              defaultNature: J2cl
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
              - coord: com.example:mylib:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:mylib:jar:1.0' does not specify the Java nature but is a direct dependency of"
                    + " 'com.example:myapp:jar:1.0' which has the Plugin nature. This is not a supported scenario.");
    }

    @Test
    public void depsAreSorted() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(
                dir,
                "com.example:myapp:1.0",
                "com.example:mylib1:1.0",
                "com.example:mylib3:1.0",
                "com.example:mylib2:1.0",
                "com.example:mylib4:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib1:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib2:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib3:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib4:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 5);

        final ArtifactRecord artifactRecord = record.getArtifact("com.example", "myapp");
        final List<ArtifactRecord> deps = artifactRecord.getDeps();
        assertEquals(deps.size(), 4);
        assertEquals(deps.get(0).getArtifact().toString(), "com.example:mylib1:jar:1.0");
        assertEquals(deps.get(1).getArtifact().toString(), "com.example:mylib2:jar:1.0");
        assertEquals(deps.get(2).getArtifact().toString(), "com.example:mylib3:jar:1.0");
        assertEquals(deps.get(3).getArtifact().toString(), "com.example:mylib4:jar:1.0");
    }

    @Test
    public void runtimeDepsAreSorted() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(
                dir,
                "com.example:myapp:1.0",
                "com.example:mylib1:jar::1.0:runtime",
                "com.example:mylib3:jar::1.0:runtime",
                "com.example:mylib2:jar::1.0:runtime",
                "com.example:mylib4:jar::1.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:mylib1:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib2:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib3:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib4:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 5);

        final ArtifactRecord artifactRecord = record.getArtifact("com.example", "myapp");
        final List<ArtifactRecord> deps = artifactRecord.getRuntimeDeps();
        assertEquals(deps.size(), 4);
        assertEquals(deps.get(0).getArtifact().toString(), "com.example:mylib1:jar:1.0");
        assertEquals(deps.get(1).getArtifact().toString(), "com.example:mylib2:jar:1.0");
        assertEquals(deps.get(2).getArtifact().toString(), "com.example:mylib3:jar:1.0");
        assertEquals(deps.get(3).getArtifact().toString(), "com.example:mylib4:jar:1.0");
    }

    @Test
    public void multipleDependenciesWithSameKeyOmitsSecondIfBothHaveClassifiers() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(
                dir,
                "com.example:myapp:1.0",
                "com.example:mylib:jar:stripped:1.0",
                "com.example:mylib:jar:sources:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:jar:stripped:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 2);

        final ArtifactRecord artifactRecord = record.getArtifact("com.example", "myapp");
        final List<ArtifactRecord> deps = artifactRecord.getDeps();
        assertEquals(deps.size(), 1);
        assertEquals(deps.get(0).getArtifact().toString(), "com.example:mylib:jar:stripped:1.0");
    }

    @Test
    public void build_singleRuntimeDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:rtA:jar::33.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:rtA:jar:33.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:rtA");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
            assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getRuntimeDeps().get(0).getKey(), "com.example:rtA");
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "rtA"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:rtA");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__rta");
            assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
            assertTrue(artifactRecord.generatesApi());
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:rtA:33.0");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }
    }

    @Test
    public void build_versionlessDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
              - coord: com.example:mylib
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }
    }

    @Test
    public void build_conflicts() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(
                dir, "com.example:myapp:1.0", "com.example:mylib:1.0", "com.example:rtA:jar::33.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:rtA:jar::32.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:rtA:32.0");
        deployArtifactToLocalRepository(dir, "com.example:rtA:33.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 3);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib,com.example:rtA");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getRuntimeDeps().get(0).getKey(), "com.example:rtA");
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 1);
            assertEquals(artifactRecord.getRuntimeDeps().get(0).getKey(), "com.example:rtA");
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "rtA"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:rtA");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
            assertEquals(artifactRecord.getNode().getDependency().getArtifact().getVersion(), "33.0");
        }
    }

    @Test
    public void build_replacement() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertNull(artifactRecord.getReplacementModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getName(Nature.Java), "com_example__myapp");
            assertEquals(artifactRecord.getLabel(Nature.Java), ":com_example__myapp");
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertNotNull(artifactRecord.getDeps().get(0).getReplacementModel());
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNull(artifactRecord.getArtifactModel());
            assertNotNull(artifactRecord.getReplacementModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getLabel(Nature.Java), "@com_example//:mylib");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }
    }

    @Test
    public void build_declaredArtifactWithReplacementOverlay() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java, J2cl]
              - coord: com.example:mylib:1.0
              - coord: com.example:base:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final ArtifactRecord artifactRecord = record.getArtifact("com.example", "mylib");
        assertNotNull(artifactRecord.getArtifactModel());
        assertNotNull(artifactRecord.getReplacementModel());
        assertEquals(artifactRecord.getNatures(), Arrays.asList(Nature.Java, Nature.J2cl));
        assertEquals(artifactRecord.getLabel(Nature.Java), ":com_example__mylib");
        assertEquals(artifactRecord.getLabel(Nature.J2cl), "@com_example//:mylib");
        assertTrue(artifactRecord.shouldEmitNatureTarget(Nature.Java));
        assertFalse(artifactRecord.shouldEmitNatureTarget(Nature.J2cl));
        assertTrue(artifactRecord.emitsRepositoryRules());
        assertEquals(artifactRecord.getDeps().size(), 1);
        assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:base");
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void build_exclude() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            excludes:
              - coord: com.example:mylib
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 1);
        assertNonSystemArtifactList(record, "com.example:myapp");

        final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
        assertNotNull(artifactRecord.getArtifactModel());
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getDeps().size(), 0);
        assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
    }

    @Test
    public void build_whereRuntimeDependencyExcluded() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            excludes:
              - coord: com.example:mylib
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:runtime");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 1);
        assertNonSystemArtifactList(record, "com.example:myapp");

        final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
        assertNotNull(artifactRecord.getArtifactModel());
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getDeps().size(), 0);
        assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
    }

    @Test
    public void build_singleOptionalDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                includeOptional: true
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:jar::1.0:compile:optional");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(
                record.getSource().getConfigLocation(),
                getDefaultConfigFile().toAbsolutePath().normalize());
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example:myapp,com.example:mylib");

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "myapp"));
            assertNotNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getDeps().size(), 1);
            assertEquals(artifactRecord.getDeps().get(0).getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }

        {
            final ArtifactRecord artifactRecord = requireNonNull(record.findArtifact("com.example", "mylib"));
            assertNull(artifactRecord.getArtifactModel());
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getDeps().size(), 0);
            assertEquals(artifactRecord.getRuntimeDeps().size(), 0);
        }
    }

    @Test
    public void findArtifact() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final ArtifactRecord artifact1 = requireNonNull(record.findArtifact("com.example", "myapp"));
        assertEquals(artifact1.getKey(), "com.example:myapp");

        assertNull(record.findArtifact("com.example", "other-no-exist"));

        // Also finds system artifacts
        final ArtifactRecord artifact2 =
                requireNonNull(record.findArtifact(DepGenConfig.getGroupId(), DepGenConfig.getArtifactId()));
        assertEquals(artifact2.getKey(), DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId());
    }

    @Test
    public void getArtifact() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final ArtifactRecord artifact1 = record.getArtifact("com.example", "myapp");
        assertNotNull(artifact1);
        assertEquals(artifact1.getKey(), "com.example:myapp");

        assertThrows(NullPointerException.class, () -> record.getArtifact("com.example", "other-no-exist"));
    }

    @Test
    public void getNature_withDefaultJavaNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void getNature_withDefaultPluginNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            react4j.processor.ReactProcessor
            arez.processor.ArezProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Plugin));
        assertTrue(artifactRecord.generatesApi());
    }

    @Test
    public void getNature_withExplicitNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Plugin));
        assertTrue(artifactRecord.generatesApi());
    }

    @Test
    public void getNature_of_transitiveDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 2);
        {
            final ArtifactRecord artifactRecord = artifacts.get(0);
            assertEquals(artifactRecord.getKey(), "com.example:myapp");
            assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
            assertTrue(artifactRecord.generatesApi());
        }
        {
            final ArtifactRecord artifactRecord = artifacts.get(1);
            assertEquals(artifactRecord.getKey(), "com.example:mylib");
            assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
            assertTrue(artifactRecord.generatesApi());
        }
    }

    @Test
    public void defaultGeneratesApi() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            react4j.processor.ReactProcessor
            arez.processor.ArezProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Plugin));
        assertTrue(artifactRecord.generatesApi());
    }

    @Test
    public void explicitTrueGeneratesApi() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                plugin:
                  generatesApi: true
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            react4j.processor.ReactProcessor
            arez.processor.ArezProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Plugin));
        assertTrue(artifactRecord.generatesApi());
    }

    @Test
    public void explicitGeneratesApiForNonPlugin() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                plugin:
                  generatesApi: false
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' has specified 'plugin' configuration but does not specify the"
                        + " Plugin nature nor does it contain any annotation processors.");
    }

    @Test
    public void explicitGeneratesApiWhereNoProcessors() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
                plugin:
                  generatesApi: false
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' has specified 'plugin.generatesApi' configuration but does not"
                        + " contain any annotation processors.");
    }

    @Test
    public void explicitFalseGeneratesApi() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                plugin:
                  generatesApi: false
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            react4j.processor.ReactProcessor
            arez.processor.ArezProcessor
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Plugin));
        assertFalse(artifactRecord.generatesApi());
    }

    @Test
    public void j2clImportWithoutSources() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  name: custom-j2cl
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifact = loadApplicationRecord().getArtifact("com.example", "myapp");
        assertNull(artifact.getSourceSha256());
        assertTrue(artifact.emitsBinaryRepositoryRule());
        assertEquals(artifact.getEmittedRepositoryNames().size(), 1);
        assertEquals(artifact.getEmittedPrivateTargetNames().keySet(), Set.of("custom-j2cl__java_import"));
    }

    @Test
    public void j2clImportWithSources() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifact = loadApplicationRecord().getArtifact("com.example", "myapp");
        assertNotNull(artifact.getSourceSha256());
        assertTrue(artifact.emitsBinaryRepositoryRule());
        assertEquals(artifact.getEmittedRepositoryNames().size(), 2);
        assertEquals(artifact.getEmittedPrivateTargetNames().keySet(), Set.of("com_example__myapp-j2cl__java_import"));
    }

    @Test
    public void j2clImportWithRequestedSourcesMissing() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(exception.getMessage(), """
            Unable to locate source for artifact 'com.example:myapp:jar:1.0'. Specify the 'includeSource' configuration property as 'false' in the artifacts configuration.

            Dependency path:
              com.example:myapp:jar:1.0 [compile]\
            """);
    }

    @Test
    public void javaAndJ2clImportWithoutSources() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java, J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifact = loadApplicationRecord().getArtifact("com.example", "myapp");
        assertEquals(artifact.getNatures(), Arrays.asList(Nature.Java, Nature.J2cl));
        assertNull(artifact.getSourceSha256());
        assertTrue(artifact.shouldEmitNatureTarget(Nature.Java));
        assertTrue(artifact.shouldEmitNatureTarget(Nature.J2cl));
        assertEquals(artifact.getEmittedRepositoryNames().size(), 1);
        assertTrue(artifact.getEmittedPrivateTargetNames().isEmpty());
    }

    @Test
    public void j2clLibraryRejectsIncludeSourceFalse() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' has specified J2cl nature but the 'includeSource' configuration"
                        + " resolves to false.");
    }

    @Test
    public void replacedJ2clImportDoesNotEmitRepositories() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
                  jspecifyMode: Enable
            replacements:
              - coord: com.example:myapp
                targets:
                  - target: "@vendor//:myapp"
                    nature: J2cl
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifact = loadApplicationRecord().getArtifact("com.example", "myapp");
        assertFalse(artifact.shouldEmitNatureTarget(Nature.J2cl));
        assertFalse(artifact.emitsRepositoryRules());
        assertTrue(artifact.getEmittedPrivateTargetNames().isEmpty());
    }

    @Test
    public void j2clImportWithSuppress() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
                  suppress: ["checkDebuggerStatement"]
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' has specified 'j2cl.suppress' configuration but specified"
                        + " 'j2cl.mode = Import' which is incompatible with 'j2cl.suppress'.");
    }

    @Test
    public void j2clImportAutodetectsJspecifyAsDisabled() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
              j2cl:
                jspecifyMode: Autodetect
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
              - coord: org.jspecify:jspecify:1.0.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", "org.jspecify:jspecify:1.0.0");
        deployTempArtifactToLocalRepository(dir, "org.jspecify:jspecify:1.0.0");

        final ArtifactRecord artifact = loadApplicationRecord().getArtifact("com.example", "myapp");
        assertTrue(artifact.shouldEmitNatureTarget(Nature.J2cl));
    }

    @Test
    public void j2clImportRejectsExplicitJspecifyEnable() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
                  jspecifyMode: Enable
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' resolves 'j2cl.jspecifyMode' to 'Enable' but specifies"
                        + " 'j2cl.mode = Import'. JSpecify support is only available for J2cl libraries.");
    }

    @Test
    public void j2clImportRejectsInheritedJspecifyEnable() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
              j2cl:
                jspecifyMode: Enable
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' resolves 'j2cl.jspecifyMode' to 'Enable' but specifies"
                        + " 'j2cl.mode = Import'. JSpecify support is only available for J2cl libraries.");
    }

    @Test
    public void j2clImportJspecifyDisableOverridesGlobalEnable() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
              j2cl:
                jspecifyMode: Enable
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
                  jspecifyMode: Disable
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ArtifactRecord artifact = loadApplicationRecord().getArtifact("com.example", "myapp");
        assertTrue(artifact.shouldEmitNatureTarget(Nature.J2cl));
    }

    @Test
    public void j2clLibraryWithJspecifyEnableAndMissingDependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  jspecifyMode: Enable
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' resolves 'j2cl.jspecifyMode' to 'Enable' but does not have a"
                        + " direct compile dependency on 'org.jspecify:jspecify:jar' with no classifier.");
    }

    @Test
    public void replacedJ2clLibraryIgnoresJspecifyEnable() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  jspecifyMode: Enable
            replacements:
              - coord: com.example:myapp
                targets:
                  - target: "@com_example//:myapp-j2cl"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final ArtifactRecord artifact = record.getArtifact("com.example", "myapp");
        assertEquals(artifact.getLabel(Nature.J2cl), "@com_example//:myapp-j2cl");
        assertFalse(artifact.shouldEmitNatureTarget(Nature.J2cl));
    }

    @Test
    public void j2clConfigWithoutJ2clNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                j2cl:
                  suppress: ["checkDebuggerStatement"]
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' has specified 'j2cl' configuration but does not specify the J2cl"
                        + " nature.");
    }

    @Test
    public void javaConfigWithoutJavaNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                java:
                  exportDeps: true
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp:jar:1.0' has specified 'java' configuration but does not specify the Java"
                        + " nature.");
    }

    @Test
    public void getName_withNameStrategy() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getName(Nature.Java), "myapp");
    }

    @Test
    public void getName_withNameStrategyAndPrefix() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: gwt_
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(artifactRecord.getName(Nature.Java), "gwt_myapp");
    }

    @Test
    public void loadWhereDuplicateNamesExist() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example.app1:core:42.0
              - coord: com.example.app2:core:37.0
            """);
        deployArtifactToLocalRepository(dir, "com.example.app1:core:42.0");
        deployArtifactToLocalRepository(dir, "com.example.app2:core:37.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Multiple emitted targets have the same name 'core' which is not supported. Adjust naming"
                        + " configuration or explicit names for artifact 'com.example.app1:core:jar:42.0' public Java"
                        + " target and artifact 'com.example.app2:core:jar:37.0' public Java target.");
    }

    @Test
    public void loadWhereDuplicateNamesWorkedAroundViaExplicitNames() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example.app1:core:42.0
                nameStrategy: GroupIdAndArtifactId
              - coord: com.example.app2:core:37.0
            """);
        deployArtifactToLocalRepository(dir, "com.example.app1:core:42.0");
        deployArtifactToLocalRepository(dir, "com.example.app2:core:37.0");

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example.app1:core,com.example.app2:core");

        final ArtifactRecord artifactRecord1 = artifacts.get(0);
        assertEquals(artifactRecord1.getKey(), "com.example.app1:core");
        assertEquals(artifactRecord1.getName(Nature.Java), "com_example_app1__core");

        final ArtifactRecord artifactRecord2 = artifacts.get(1);
        assertEquals(artifactRecord2.getKey(), "com.example.app2:core");
        assertEquals(artifactRecord2.getName(Nature.Java), "core");
    }

    @Test
    public void loadWhereJ2clImportJavaTargetNameCollides() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example.app1:myapp:1.0
                natures: [J2cl]
                j2cl:
                  name: shared
                  mode: Import
              - coord: shared:java-import:1.0
                natures: [Java]
            """);
        deployTempArtifactToLocalRepository(dir, "com.example.app1:myapp:1.0");
        deployTempArtifactToLocalRepository(dir, "shared:java-import:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Multiple emitted targets have the same name 'shared__java_import' which is not supported. Adjust"
                        + " naming configuration or explicit names for artifact"
                        + " 'com.example.app1:myapp:jar:1.0' private J2cl Import Java target and artifact"
                        + " 'shared:java-import:jar:1.0' public Java target.");
    }

    @Test
    public void loadWhereTargetNameCollidesWithHelperTarget() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example:verify-config-sha256:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:verify-config-sha256:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Multiple emitted targets have the same name 'verify_config_sha256' which is not supported. Adjust"
                    + " naming configuration or explicit names for built-in helper target 'verify_config_sha256' and"
                    + " artifact 'com.example:verify-config-sha256:jar:1.0' public Java target.");
    }

    @Test
    public void loadWherePrefixedTargetNameCollidesWithHelperTarget() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: myapp
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example:verify-config-sha256:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:verify-config-sha256:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Multiple emitted targets have the same name 'myapp_verify_config_sha256' which is not supported."
                        + " Adjust naming configuration or explicit names for built-in helper target"
                        + " 'myapp_verify_config_sha256' and artifact 'com.example:verify-config-sha256:jar:1.0' public"
                        + " Java target.");
    }

    @Test
    public void loadWhereRepositoryCollisionIgnoresNonEmittedFamilyMember() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: GroupIdAndArtifactIdAndVersion
              repositoryNameStrategy: GroupIdAndArtifactId
            artifacts:
              - coord: foo.bar:baz:1.0
                includeSource: false
              - coord: foo_bar:baz:sources
                repositoryNameStrategy: GroupIdAndArtifactIdAndVersion
            """);
        deployArtifactToLocalRepository(dir, "foo.bar:baz:1.0");
        deployArtifactToLocalRepository(dir, "foo_bar:baz:sources");

        final ApplicationRecord record = loadApplicationRecord();
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "foo.bar:baz,foo_bar:baz");
    }

    @Test
    public void loadWhereRepositoryCollisionExists() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: GroupIdAndArtifactIdAndVersion
              repositoryNameStrategy: GroupIdAndArtifactId
            artifacts:
              - coord: foo.bar:baz:1.0
              - coord: foo_bar:baz:sources
                repositoryNameStrategy: GroupIdAndArtifactIdAndVersion
            """);
        deployArtifactToLocalRepository(dir, "foo.bar:baz:1.0");
        deployArtifactToLocalRepository(dir, "foo_bar:baz:sources");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Multiple emitted repositories have the same name 'foo_bar__baz__sources' which is not supported."
                    + " Adjust repository naming configuration for artifact 'foo.bar:baz:jar:1.0' sources repository"
                    + " and artifact 'foo_bar:baz:jar:sources' binary repository.");
    }

    @Test
    public void loadWhereReplacementDoesNotTriggerEmittedNameCollision() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example.app1:core:42.0
              - coord: com.example.app2:user:1.0
            replacements:
              - coord: com.example.app2:core
                targets:
                  - target: "@vendor//:core"
            """);
        deployArtifactToLocalRepository(dir, "com.example.app1:core:42.0");
        deployArtifactToLocalRepository(dir, "com.example.app2:user:1.0", "com.example.app2:core:37.0");
        deployArtifactToLocalRepository(dir, "com.example.app2:core:37.0");

        final ApplicationRecord record = loadApplicationRecord();
        assertNonSystemArtifactCount(record, 3);
        assertNonSystemArtifactList(record, "com.example.app1:core,com.example.app2:core,com.example.app2:user");
        assertNotNull(record.getArtifact("com.example.app2", "core").getReplacementModel());
    }

    @Test
    public void loadWhereJ2clArtifactOmitsSourcesClassifierVariant() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example.app1:core:42.0
                natures: [J2cl]
            """);
        deployTempArtifactToLocalRepository(dir, "com.example.app1:core:42.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Unable to locate the sources classifier artifact for the artifact 'com.example.app1:core:jar:42.0'"
                        + " but the artifact has the J2cl nature which requires that sources be present.\n\n"
                        + "Dependency path:\n"
                        + "  com.example.app1:core:jar:42.0 [compile]");
    }

    @Test
    public void shouldExportDeps_perArtifactConfig() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example.app1:core:42.0
                java:
                  exportDeps: true
              - coord: com.example.app2:core:37.0
            """);
        deployArtifactToLocalRepository(dir, "com.example.app1:core:42.0");
        deployArtifactToLocalRepository(dir, "com.example.app2:core:37.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example.app1:core,com.example.app2:core");

        final ArtifactRecord artifactRecord1 = record.getArtifact("com.example.app1", "core");
        assertEquals(artifactRecord1.getKey(), "com.example.app1:core");
        assertTrue(artifactRecord1.shouldExportDeps());

        final ArtifactRecord artifactRecord2 = record.getArtifact("com.example.app2", "core");
        assertEquals(artifactRecord2.getKey(), "com.example.app2:core");
        assertFalse(artifactRecord2.shouldExportDeps());
    }

    @Test
    public void shouldExportDeps_perArtifactConfig_combinedWithGlobalConfig() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              java:
                exportDeps: true
            artifacts:
              - coord: com.example.app1:core:42.0
                java:
                  exportDeps: false
              - coord: com.example.app2:core:37.0
            """);
        deployArtifactToLocalRepository(dir, "com.example.app1:core:42.0");
        deployArtifactToLocalRepository(dir, "com.example.app2:core:37.0");

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 2);
        assertNonSystemArtifactList(record, "com.example.app1:core,com.example.app2:core");

        final ArtifactRecord artifactRecord1 = artifacts.get(0);
        assertEquals(artifactRecord1.getKey(), "com.example.app1:core");
        assertFalse(artifactRecord1.shouldExportDeps());

        final ArtifactRecord artifactRecord2 = artifacts.get(1);
        assertEquals(artifactRecord2.getKey(), "com.example.app2:core");
        assertTrue(artifactRecord2.shouldExportDeps());
    }

    @Test
    public void parseWhereArtifactContainsProcessors() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        final Path jarFile = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
            react4j.processor.ReactProcessor
            arez.processor.ArezProcessor
            """);

        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", jarFile);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", jarFile);

        final ApplicationRecord record = loadApplicationRecord();

        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertNotNull(artifactRecord.getArtifactModel());
        assertEquals(artifactRecord.getKey(), "com.example:myapp");
        assertEquals(
                artifactRecord.getProcessors(),
                Arrays.asList("react4j.processor.ReactProcessor", "arez.processor.ArezProcessor"));
    }

    @Test
    public void artifactWithDefaultNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertEquals(
                artifacts.size(), 1 + record.getSource().getSystemArtifacts().size());
        {
            final ArtifactRecord artifactRecord = record.getArtifact("com.example", "myapp");
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "com.example:myapp:1.0");
            assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
        }
        {
            final ArtifactRecord artifactRecord = record.getArtifact("org.realityforge.bazel.depgen", "bazel-depgen");
            assertEquals(artifactRecord.getMavenCoordinatesBazelTag(), "org.realityforge.bazel.depgen:bazel-depgen:1");
            assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.Java));
        }
    }

    @Test
    public void artifactWithSpecifiedDefaultNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              defaultNature: J2cl
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.J2cl));
    }

    @Test
    public void artifactWithSpecifiedNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final List<ArtifactRecord> artifacts = record.getArtifacts();
        assertNonSystemArtifactCount(record, 1);
        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(artifactRecord.getNatures(), Collections.singletonList(Nature.J2cl));
    }

    @Test
    public void writeTargetMacro() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                    )

                    _java_import(
                        name = "org_realityforge_bazel_depgen__bazel_depgen",
                        jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                        tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                    )
                """);
    }

    @Test
    public void writeTargetMacro_where_verifySha256_false() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              verifyConfigSha256: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                    )
                """);
    }

    @Test
    public void writeTargetMacro_omitEnabled() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              supportDependencyOmit: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets(
                        omit_com_example__myapp = False,
                        omit_org_realityforge_bazel_depgen__bazel_depgen = False):
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    if not omit_com_example__myapp:
                        _java_import(
                            name = "com_example__myapp",
                            jars = ["@com_example__myapp__1_0//file"],
                            srcjar = "@com_example__myapp__1_0__sources//file",
                            tags = ["maven_coordinates=com.example:myapp:1.0"],
                        )

                    if not omit_org_realityforge_bazel_depgen__bazel_depgen:
                        _java_import(
                            name = "org_realityforge_bazel_depgen__bazel_depgen",
                            jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                            tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                        )
                """);
    }

    @Test
    public void writeTargetMacro_omitEnabled_orderOmitInDeclarationsAlphanumerically() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              supportDependencyOmit: true
              nameStrategy: ArtifactId
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets(
                        omit_bazel_depgen = False,
                        omit_myapp = False):
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":bazel_depgen"],
                    )

                    if not omit_myapp:
                        _java_import(
                            name = "myapp",
                            jars = ["@com_example__myapp__1_0//file"],
                            srcjar = "@com_example__myapp__1_0__sources//file",
                            tags = ["maven_coordinates=com.example:myapp:1.0"],
                        )

                    if not omit_bazel_depgen:
                        _java_import(
                            name = "bazel_depgen",
                            jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                            tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                        )
                """);
    }

    @Test
    public void writeTargetMacro_macroNameOverride() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              targetMacroName: generate_myapp_targets
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_myapp_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                    )

                    _java_import(
                        name = "org_realityforge_bazel_depgen__bazel_depgen",
                        jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                        tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                    )
                """);
    }

    @Test
    public void writeTargetMacro_dependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:2.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:2.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                        deps = [":com_example__mylib"],
                    )

                    _java_import(
                        name = "com_example__mylib",
                        jars = ["@com_example__mylib__2_0//file"],
                        srcjar = "@com_example__mylib__2_0__sources//file",
                        tags = ["maven_coordinates=com.example:mylib:2.0"],
                        visibility = ["//visibility:private"],
                    )

                    _java_import(
                        name = "org_realityforge_bazel_depgen__bazel_depgen",
                        jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                        tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                    )
                """);
    }

    @Test
    public void writeTargetMacro_replacement() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:2.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:2.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    )

                    _java_import(
                        name = "com_example__myapp",
                        jars = ["@com_example__myapp__1_0//file"],
                        srcjar = "@com_example__myapp__1_0__sources//file",
                        tags = ["maven_coordinates=com.example:myapp:1.0"],
                        deps = ["@com_example//:mylib"],
                    )

                    _java_import(
                        name = "org_realityforge_bazel_depgen__bazel_depgen",
                        jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                        tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                    )
                """);
    }

    @Test
    public void writeTargetMacro_replacementOverlay() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java, J2cl]
              - coord: com.example:mylib:1.0
              - coord: com.example:base:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertOutputContains(output, """
                _j2cl_library(
                    name = "com_example__myapp-j2cl",
                    srcs = ["@com_example__myapp__1_0__sources//file"],
                    deps = ["@com_example//:mylib"],
                )
            """);
        assertOutputContains(output, """
                _java_import(
                    name = "com_example__myapp",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                    deps = [":com_example__mylib"],
                )
            """);
        assertOutputContains(output, """
                _java_import(
                    name = "com_example__mylib",
                    jars = ["@com_example__mylib__1_0//file"],
                    srcjar = "@com_example__mylib__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:mylib:1.0"],
                    deps = [":com_example__base"],
                )
            """);
        assertOutputDoesNotContain(output, "        name = \"com_example__mylib-j2cl\",\n");
        assertOutputDoesNotContain(output, ":com_example__base-j2cl");
    }

    @Test
    public void writeTargetMacro_depgen_replacement() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            replacements:
              - coord: org.realityforge.bazel.depgen:bazel-depgen
                targets:
                  - target: "@org_realityforge_bazel//:depgen"
            """);

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeTargetMacro(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                def generate_targets():
                    ""\"
                        Macro to define targets for dependencies.
                    ""\"

                    _java_test(
                        name = "verify_config_sha256",
                        size = "small",
                        runtime_deps = ["@org_realityforge_bazel//:depgen"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        use_testrunner = False,
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "hash",
                            "--verify-sha256",
                            _CONFIG_SHA256,
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        visibility = ["//visibility:private"],
                    )

                    _java_binary(
                        name = "update_depgen_generated_outputs",
                        args = [
                            "--config-file",
                            "$(rootpath //thirdparty:dependencies.yml)",
                            "--quiet",
                            "generate",
                        ],
                        data = ["//thirdparty:dependencies.yml"],
                        main_class = "org.realityforge.bazel.depgen.Main",
                        tags = [
                            "local",
                            "manual",
                            "no-cache",
                            "no-remote",
                            "no-sandbox",
                        ],
                        visibility = ["//visibility:private"],
                        runtime_deps = ["@org_realityforge_bazel//:depgen"],
                    )
                """);
    }

    @Test
    public void writeWorkspaceMacro() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )
            """);
    }

    @Test
    public void writeWorkspaceMacro_externalAnnotationsPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            options:
              includeExternalAnnotations: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:annotations:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__annotations",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-annotations.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-annotations.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )
            """);
    }

    @Test
    public void writeWorkspaceMacro_omitEnabled() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            options:
              supportDependencyOmit: true
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            def generate_workspace_rules(
                    omit_com_example__myapp = False,
                    omit_org_realityforge_bazel_depgen__bazel_depgen = False):
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                if not omit_com_example__myapp:
                    _http_file(
                        name = "com_example__myapp__1_0",
                        downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                    )

                    _http_file(
                        name = "com_example__myapp__1_0__sources",
                        downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                    )

                if not omit_org_realityforge_bazel_depgen__bazel_depgen:
                    _http_file(
                        name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                        downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                        sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                        urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                    )
            """);
    }

    @Test
    public void writeWorkspaceMacro_macroNameOverride() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            options:
              workspaceMacroName: generate_myapp_workspace_rules
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            def generate_myapp_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )
            """);
    }

    @Test
    public void writeWorkspaceMacro_dependency() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:2.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:2.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "com_example__mylib__2_0",
                    downloaded_file_path = "com/example/mylib/2.0/mylib-2.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/mylib/2.0/mylib-2.0.jar"],
                )

                _http_file(
                    name = "com_example__mylib__2_0__sources",
                    downloaded_file_path = "com/example/mylib/2.0/mylib-2.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/mylib/2.0/mylib-2.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )
            """);
    }

    @Test
    public void writeWorkspaceMacro_replacement() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:2.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:2.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )
            """);
    }

    @Test
    public void writeWorkspaceMacro_replacementOverlay() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java, J2cl]
              - coord: com.example:mylib:1.0
              - coord: com.example:base:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
                    nature: J2cl
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeWorkspaceMacro(new StarlarkOutput(outputStream));
        final String output = asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString());
        assertOutputContains(output, "        name = \"com_example__mylib__1_0\",\n");
        assertOutputContains(output, "        name = \"com_example__mylib__1_0__sources\",\n");
        assertOutputContains(output, "        name = \"com_example__base__1_0\",\n");
    }

    @Test
    public void writeDependencyGraphIfRequired() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:2.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:2.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeDependencyGraphIfRequired(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                # Dependency Graph Generated from the input data
                # \\- com.example:myapp:jar:1.0 [compile]
                #    \\- com.example:mylib:jar:2.0 [compile]

                """);
    }

    @Test
    public void writeDependencyGraphIfRequired_disabledInConfig() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              emitDependencyGraph: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:2.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:2.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeDependencyGraphIfRequired(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                "");
    }

    @Test
    public void writeRegenerateExtensionTarget() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeRegenerateExtensionTarget(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )
                """);
    }

    @Test
    public void writeVerifyTarget() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeVerifyTarget(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )
                """);
    }

    @Test
    public void writeVerifyTarget_usingPrefixAndAlternativeNameStrategy() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              namePrefix: myapp
              nameStrategy: ArtifactId
            """);

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeVerifyTarget(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                _java_test(
                    name = "myapp_verify_config_sha256",
                    size = "small",
                    runtime_deps = [":myapp_bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )
                """);
    }

    @Test
    public void writeBazelExtension() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_test = "java_test")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

                _java_import(
                    name = "com_example__myapp",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void writeBazelExtension_where_verifyConfigSha256_is_false() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            options:
              verifyConfigSha256: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_import = "java_import")

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_import(
                    name = "com_example__myapp",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )
            """);
    }

    @Test
    public void writeDefaultExtensionBuild_configFileInSameDirectory() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeDefaultExtensionBuild(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                # File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen\
                 version 1
                # Contents can be edited and will not be overridden.
                package(default_visibility = ["//visibility:public"])

                load("//thirdparty:dependencies.bzl", "generate_targets")

                generate_targets()

                exports_files(["dependencies.yml"])
                """);
    }

    @Test
    public void writeDefaultExtensionBuild_configFileInDifferentDirectory() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              extensionFile: somedir/dependencies.bzl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeDefaultExtensionBuild(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                # File is auto-generated from ../dependencies.yml by https://github.com/realityforge/bazel-depgen\
                 version 1
                # Contents can be edited and will not be overridden.
                package(default_visibility = ["//visibility:public"])

                load("//thirdparty/somedir:dependencies.bzl", "generate_targets")

                generate_targets()
                """);
    }

    @Test
    public void writeDefaultConfigBuild() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeDefaultConfigBuild(new StarlarkOutput(outputStream));
        assertEquals(
                asCleanString(
                        outputStream,
                        record.getSource().getConfigSha256(),
                        dir.toUri().toString()),
                """
                # File is auto-generated from dependencies.yml by https://github.com/realityforge/bazel-depgen\
                 version 1
                # Contents can be edited and will not be overridden.
                package(default_visibility = ["//visibility:public"])

                exports_files(["dependencies.yml"])
                """);
    }

    @Test
    public void writeBazelExtension_j2clImportWithoutSources() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  name: custom-j2cl
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());

        assertTrue(output.contains("_java_import = \"java_import\""));
        assertTrue(output.contains("load(\"@j2cl//build_defs:rules.bzl\", _j2cl_import = \"j2cl_import\")"));
        assertTrue(output.contains("name = \"com_example__myapp__1_0\""));
        assertFalse(output.contains("com_example__myapp__1_0__sources"));
        assertTrue(output.contains("name = \"custom-j2cl__java_import\""));
        assertTrue(output.contains("jars = [\"@com_example__myapp__1_0//file\"]"));
        assertTrue(output.contains("_j2cl_import("));
        assertTrue(output.contains("name = \"custom-j2cl\""));
        assertTrue(output.contains("jar = \":custom-j2cl__java_import\""));
        assertFalse(output.contains("_j2cl_library("));
    }

    @Test
    public void writeDirectSections_j2clImportWithoutSources() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
              repositoryRuleGenerationStrategy: module
              targetGenerationStrategy: build
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final var moduleOutputStream = new ByteArrayOutputStream();
        record.writeBazelModuleSection(new StarlarkOutput(moduleOutputStream));
        final String moduleOutput = asCleanString(
                moduleOutputStream,
                record.getSource().getConfigSha256(),
                dir.toUri().toString());
        assertTrue(moduleOutput.contains("name = \"com_example__myapp__1_0\""));
        assertFalse(moduleOutput.contains("com_example__myapp__1_0__sources"));

        final var buildOutputStream = new ByteArrayOutputStream();
        record.writeBazelBuildSection(new StarlarkOutput(buildOutputStream));
        final String buildOutput = asCleanString(
                buildOutputStream,
                record.getSource().getConfigSha256(),
                dir.toUri().toString());
        assertTrue(buildOutput.contains("_java_import = \"java_import\""));
        assertTrue(buildOutput.contains("load(\"@j2cl//build_defs:rules.bzl\", _j2cl_import = \"j2cl_import\")"));
        assertTrue(buildOutput.contains("_java_import("));
        assertTrue(buildOutput.contains("name = \"com_example__myapp-j2cl__java_import\""));
        assertTrue(buildOutput.contains("_j2cl_import("));
        assertTrue(buildOutput.contains("jar = \":com_example__myapp-j2cl__java_import\""));
    }

    @Test
    public void writeBazelExtension_j2clArtifactPresent() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl, Java]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_test = "java_test")
            load("@j2cl//build_defs:rules.bzl", _j2cl_library = "j2cl_library")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

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

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void writeBazelExtension_j2cl_withDependencies() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]
            #    \\- com.example:mylib:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_test = "java_test")
            load("@j2cl//build_defs:rules.bzl", _j2cl_library = "j2cl_library")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "com_example__mylib__1_0__sources",
                    downloaded_file_path = "com/example/mylib/1.0/mylib-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

                _j2cl_library(
                    name = "com_example__myapp-j2cl",
                    srcs = ["@com_example__myapp__1_0__sources//file"],
                    deps = [":com_example__mylib-j2cl"],
                )

                _j2cl_library(
                    name = "com_example__mylib-j2cl",
                    srcs = ["@com_example__mylib__1_0__sources//file"],
                    visibility = ["//visibility:private"],
                )

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void writeBazelExtension_j2cl_withJavaScriptInSourceArchives() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);

        final Path jarFile1 = createJarFile(outputStream -> {
            createJarEntry(outputStream, "com/biz/MyFile1.js", "");
            createJarEntry(outputStream, "com/biz/MyOtherFile.js", "");
            createJarEntry(outputStream, "com/biz/MyBlah.js", "");
            createJarEntry(outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "");
            createJarEntry(outputStream, "com/biz/TheClass.native.js", "");
            createJarEntry(outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "");
        });
        final Path jarFile2 = createJarFile("foo.js", "");
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0", jarFile1);
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", jarFile2);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]
            #    \\- com.example:mylib:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_test = "java_test")
            load("@j2cl//build_defs:rules.bzl", _j2cl_library = "j2cl_library")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "com_example__mylib__1_0__sources",
                    downloaded_file_path = "com/example/mylib/1.0/mylib-1.0-sources.jar",
                    sha256 = "e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f",
                    urls = ["MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

                _j2cl_library(
                    name = "com_example__myapp-j2cl",
                    srcs = ["@com_example__myapp__1_0__sources//file"],
                    deps = [":com_example__mylib-j2cl"],
                )

                _j2cl_library(
                    name = "com_example__mylib-j2cl",
                    srcs = ["@com_example__mylib__1_0__sources//file"],
                    visibility = ["//visibility:private"],
                )

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void writeBazelModuleSection_j2cl_withJavaScriptInSourceArchives() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            options:
              repositoryRuleGenerationStrategy: module
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);

        final Path jarFile1 = createJarFile(outputStream -> {
            createJarEntry(outputStream, "com/biz/MyFile1.js", "");
            createJarEntry(outputStream, "com/biz/MyOtherFile.js", "");
            createJarEntry(outputStream, "com/biz/MyBlah.js", "");
            createJarEntry(outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "");
            createJarEntry(outputStream, "com/biz/TheClass.native.js", "");
            createJarEntry(outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "");
        });
        final Path jarFile2 = createJarFile("foo.js", "");
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0", jarFile1);
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", jarFile2);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelModuleSection(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: Content is auto-generated from //thirdparty:dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            _http_file = use_repo_rule("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

            _http_file(
                name = "com_example__myapp__1_0__sources",
                downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                sha256 = "94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d",
                urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
            )

            _http_file(
                name = "com_example__mylib__1_0__sources",
                downloaded_file_path = "com/example/mylib/1.0/mylib-1.0-sources.jar",
                sha256 = "e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f",
                urls = ["MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar"],
            )

            _http_file(
                name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                urls = ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
            )
            """);
    }

    @Test
    public void writeBazelModuleSection_repositoryRuleLoadSymbolsSuppressesHttpFileBindingOnly() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              repositoryRuleGenerationStrategy: module
              repositoryRuleLoadSymbols:
                http_file: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelModuleSection(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertFalse(output.contains("_http_file = use_repo_rule"));
        assertTrue(output.contains("_http_file("));
    }

    @Test
    public void writeBazelBuildSection_targetRuleLoadSymbolsSuppressesJavaImportLoadOnly() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              targetGenerationStrategy: build
              targetRuleLoadSymbols:
                java_import: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelBuildSection(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertTrue(output.contains(
                "load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", _java_test = \"java_test\")"));
        assertFalse(output.contains("_java_import = \"java_import\""));
        assertTrue(output.contains("_java_import("));
    }

    @Test
    public void writeBazelBuildSection_targetRuleLoadSymbolsSuppressesJ2clLibraryLoadOnly() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              targetGenerationStrategy: build
              targetRuleLoadSymbols:
                j2cl_library: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
            """);
        final Path sourceJar = createJarFile(outputStream -> {
            createJarEntry(outputStream, "com/example/MyApp.java", "");
            createJarEntry(outputStream, "com/example/MyApp.js", "");
            createJarEntry(outputStream, "com/example/MyApp.native.js", "");
            createJarEntry(outputStream, "com/example/public/asset.js", "");
        });
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", sourceJar);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelBuildSection(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertFalse(output.contains("load(\"@j2cl//build_defs:rules.bzl\""));
        assertTrue(output.contains("_j2cl_library("));
        assertTrue(output.contains("srcs = [\"@com_example__myapp__1_0__sources//file\"]"));
        assertFalse(output.contains("__js_sources"));
    }

    @Test
    public void writeBazelBuildSection_targetRuleLoadSymbolsSuppressesJ2clImportLoadOnly() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
              targetGenerationStrategy: build
              targetRuleLoadSymbols:
                j2cl_import: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelBuildSection(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertFalse(output.contains("load(\"@j2cl//build_defs:rules.bzl\""));
        assertTrue(output.contains("_java_import = \"java_import\""));
        assertTrue(output.contains("_j2cl_import("));
    }

    @Test
    public void writeBazelBuildSection_targetRuleLoadSymbolsSuppressesJ2clImportJavaTargetLoadOnly() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              includeSource: false
              targetGenerationStrategy: build
              targetRuleLoadSymbols:
                java_import: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelBuildSection(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertFalse(output.contains("_java_import = \"java_import\""));
        assertTrue(output.contains("load(\"@j2cl//build_defs:rules.bzl\", _j2cl_import = \"j2cl_import\")"));
        assertTrue(output.contains("_java_import("));
        assertTrue(output.contains("_j2cl_import("));
    }

    @Test
    public void writeBazelExtension_mixedStrategyTargetRuleLoadSymbolsDoNotFilterRepositoryRuleLoads()
            throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              targetGenerationStrategy: build
              targetRuleLoadSymbols:
                java_binary: false
                java_import: false
                java_test: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertTrue(
                output.contains("load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", _http_file = \"http_file\")"));
        assertTrue(output.contains("def generate_workspace_rules():"));
        assertFalse(output.contains("def generate_targets():"));
    }

    @Test
    public void writeBazelExtension_mixedStrategyRepositoryRuleLoadSymbolsDoNotFilterTargetRuleLoads()
            throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            options:
              repositoryRuleGenerationStrategy: module
              repositoryRuleLoadSymbols:
                http_file: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        final String output = asCleanString(
                outputStream, record.getSource().getConfigSha256(), dir.toUri().toString());
        assertTrue(output.contains("load(\"@rules_java//java:defs.bzl\", _java_binary = \"java_binary\", "
                + "_java_import = \"java_import\", _java_test = \"java_test\")"));
        assertFalse(output.contains("def generate_workspace_rules():"));
        assertTrue(output.contains("def generate_targets():"));
    }

    @Test
    public void writeBazelExtension_java_withJavaScriptInSourceArchives() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java]
            """);

        final Path jarFile1 = createJarFile(outputStream -> {
            createJarEntry(outputStream, "com/biz/MyFile1.js", "");
            createJarEntry(outputStream, "com/biz/MyOtherFile.js", "");
            createJarEntry(outputStream, "com/biz/MyBlah.js", "");
            createJarEntry(outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "");
            createJarEntry(outputStream, "com/biz/TheClass.native.js", "");
            createJarEntry(outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "");
        });
        final Path jarFile2 = createJarFile("foo.js", "");
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:jar:sources:1.0", jarFile1);
        deployTempArtifactToLocalRepository(dir, "com.example:mylib:1.0");
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:jar:sources:1.0", jarFile2);
        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]
            #    \\- com.example:mylib:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_test = "java_test")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "94a269c384942133603eeb46ec01b5c7b0f9fdf387ce5d6d6014d57d3ba4f66d",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "com_example__mylib__1_0",
                    downloaded_file_path = "com/example/mylib/1.0/mylib-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/mylib/1.0/mylib-1.0.jar"],
                )

                _http_file(
                    name = "com_example__mylib__1_0__sources",
                    downloaded_file_path = "com/example/mylib/1.0/mylib-1.0-sources.jar",
                    sha256 = "e4730e06a8517a909250daa9cb33764d058cd806ffc36b067bfc5c1a36b8728f",
                    urls = ["MYURI/com/example/mylib/1.0/mylib-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

                _java_import(
                    name = "com_example__myapp",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                    deps = [":com_example__mylib"],
                )

                _java_import(
                    name = "com_example__mylib",
                    jars = ["@com_example__mylib__1_0//file"],
                    srcjar = "@com_example__mylib__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:mylib:1.0"],
                    visibility = ["//visibility:private"],
                )

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void writeBazelExtension_Plugin() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Plugin]
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # \\- com.example:myapp:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_library = "java_library", _java_plugin = "java_plugin", _java_test\
             = "java_test")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

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

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void writeBazelExtension_withNamesAndMultipleNatures() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();
        final URI uri = dir.toUri();

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
              - coord: com.example:myapp2:1.0
                natures: [Java, J2cl, Plugin]
            """);

        deployArtifactToLocalRepository(dir, "com.example:myapp2:1.0", "com.example:myapp:1.0");
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        final var outputStream = new ByteArrayOutputStream();
        record.writeBazelExtension(new StarlarkOutput(outputStream));
        assertEquals(asCleanString(outputStream, record.getSource().getConfigSha256(), uri.toString()), """
            # DO NOT EDIT: File is auto-generated from dependencies.yml by\
             https://github.com/realityforge/bazel-depgen version 1

            ""\"
                Macro rules to load dependencies.

                Invoke 'generate_workspace_rules' from a WORKSPACE file.
                Invoke 'generate_targets' from a BUILD.bazel file.
            ""\"
            # Dependency Graph Generated from the input data
            # +- com.example:myapp:jar:1.0 [compile]
            # \\- com.example:myapp2:jar:1.0 [compile]
            #    \\- com.example:myapp:jar:1.0 [compile]

            load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_file = "http_file")
            load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_import =\
             "java_import", _java_library = "java_library", _java_plugin = "java_plugin", _java_test\
             = "java_test")
            load("@j2cl//build_defs:rules.bzl", _j2cl_library = "j2cl_library")

            # SHA256 of the configuration content that generated this file
            _CONFIG_SHA256 = "MYSHA"

            def generate_workspace_rules():
                ""\"
                    Repository rules macro to load dependencies.

                    Must be run from a WORKSPACE file.
                ""\"

                _http_file(
                    name = "com_example__myapp__1_0",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp__1_0__sources",
                    downloaded_file_path = "com/example/myapp/1.0/myapp-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp/1.0/myapp-1.0-sources.jar"],
                )

                _http_file(
                    name = "com_example__myapp2__1_0",
                    downloaded_file_path = "com/example/myapp2/1.0/myapp2-1.0.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp2/1.0/myapp2-1.0.jar"],
                )

                _http_file(
                    name = "com_example__myapp2__1_0__sources",
                    downloaded_file_path = "com/example/myapp2/1.0/myapp2-1.0-sources.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls = ["MYURI/com/example/myapp2/1.0/myapp2-1.0-sources.jar"],
                )

                _http_file(
                    name = "org_realityforge_bazel_depgen__bazel_depgen__1",
                    downloaded_file_path =\
             "org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar",
                    sha256 = "e424b659cf9c9c4adf4c19a1cacdb13c0cbd78a79070817f433dbc2dade3c6d4",
                    urls =\
             ["MYURI/org/realityforge/bazel/depgen/bazel-depgen/1/bazel-depgen-1-all.jar"],
                )

            def generate_targets():
                ""\"
                    Macro to define targets for dependencies.
                ""\"

                _java_test(
                    name = "verify_config_sha256",
                    size = "small",
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    use_testrunner = False,
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "hash",
                        "--verify-sha256",
                        _CONFIG_SHA256,
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    visibility = ["//visibility:private"],
                )

                _java_binary(
                    name = "update_depgen_generated_outputs",
                    args = [
                        "--config-file",
                        "$(rootpath //thirdparty:dependencies.yml)",
                        "--quiet",
                        "generate",
                    ],
                    data = ["//thirdparty:dependencies.yml"],
                    main_class = "org.realityforge.bazel.depgen.Main",
                    tags = [
                        "local",
                        "manual",
                        "no-cache",
                        "no-remote",
                        "no-sandbox",
                    ],
                    visibility = ["//visibility:private"],
                    runtime_deps = [":org_realityforge_bazel_depgen__bazel_depgen"],
                )

                _java_import(
                    name = "myapp-java-a",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )

                _j2cl_library(
                    name = "myapp-j2cl-a",
                    srcs = ["@com_example__myapp__1_0__sources//file"],
                )

                _java_import(
                    name = "myapp-java-a__plugin_library",
                    jars = ["@com_example__myapp__1_0//file"],
                    srcjar = "@com_example__myapp__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp:1.0"],
                )
                _java_plugin(
                    name = "myapp-java-a__plugin",
                    visibility = ["//visibility:private"],
                    deps = [":myapp-java-a__plugin_library"],
                )
                _java_library(
                    name = "myapp-plugin-a",
                    exported_plugins = ["myapp-java-a__plugin"],
                )

                _java_import(
                    name = "com_example__myapp2",
                    jars = ["@com_example__myapp2__1_0//file"],
                    srcjar = "@com_example__myapp2__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp2:1.0"],
                    deps = [":myapp-java-a"],
                )

                _j2cl_library(
                    name = "com_example__myapp2-j2cl",
                    srcs = ["@com_example__myapp2__1_0__sources//file"],
                    deps = [":myapp-j2cl-a"],
                )

                _java_import(
                    name = "com_example__myapp2__plugin_library",
                    jars = ["@com_example__myapp2__1_0//file"],
                    srcjar = "@com_example__myapp2__1_0__sources//file",
                    tags = ["maven_coordinates=com.example:myapp2:1.0"],
                    deps = [":myapp-java-a"],
                )
                _java_plugin(
                    name = "com_example__myapp2__plugin",
                    visibility = ["//visibility:private"],
                    deps = [":com_example__myapp2__plugin_library"],
                )
                _java_library(
                    name = "com_example__myapp2-plugin",
                    exported_plugins = ["com_example__myapp2__plugin"],
                )

                _java_import(
                    name = "org_realityforge_bazel_depgen__bazel_depgen",
                    jars = ["@org_realityforge_bazel_depgen__bazel_depgen__1//file"],
                    tags = ["maven_coordinates=org.realityforge.bazel.depgen:bazel-depgen:1"],
                )
            """);
    }

    @Test
    public void replacement_targetMissingForNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl, Java]
              - coord: com.example:base:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:mylib"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Arrays.asList(Nature.J2cl, Nature.Java));
        assertEquals(record.getArtifact("com.example", "mylib").getLabel(Nature.J2cl), "@com_example//:mylib");
        assertEquals(record.getArtifact("com.example", "mylib").getLabel(Nature.Java), ":com_example__mylib");
        assertEquals(record.getArtifact("com.example", "mylib").getDeps().size(), 1);
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void replacement_targetPresentButNoSuchNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, """
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [Java]
              - coord: com.example:base:1.0
            replacements:
              - coord: com.example:mylib
                targets:
                  - target: "@com_example//:othermylib"
                    nature: Java
                  - target: "@com_example//:mylib"
                    nature: J2cl
            """);
        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir, "com.example:mylib:1.0", "com.example:base:1.0");
        deployArtifactToLocalRepository(dir, "com.example:base:1.0");

        final ApplicationRecord record = loadApplicationRecord();

        assertEquals(record.getArtifact("com.example", "mylib").getNatures(), Arrays.asList(Nature.Java, Nature.J2cl));
        assertEquals(record.getArtifact("com.example", "mylib").getLabel(Nature.Java), "@com_example//:othermylib");
        assertEquals(record.getArtifact("com.example", "mylib").getLabel(Nature.J2cl), "@com_example//:mylib");
        assertEquals(record.getArtifact("com.example", "base").getNatures(), Collections.singletonList(Nature.Java));
    }

    @Test
    public void ensureDepgenArtifactReplacementHasJavaNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(
                dir,
                "artifacts:\n" + "  - coord: com.example:myapp:1.0\n"
                        + "replacements:\n"
                        + "  - coord: "
                        + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "\n" + "    targets:\n"
                        + "      - target: \":depgen\"\n");

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationModel model = loadApplicationRecord().getSource();

        final List<ReplacementModel> replacements = model.getReplacements();
        assertEquals(replacements.size(), 1);
        final ReplacementModel replacementModel = replacements.get(0);
        final List<ReplacementTargetModel> targets = replacementModel.getTargets();
        assertEquals(targets.size(), 1);
        final ReplacementTargetModel replacementTarget = targets.get(0);
        assertEquals(replacementTarget.getNature(), Nature.Java);
        assertEquals(replacementTarget.getTarget(), ":depgen");
    }

    @Test
    public void ensureDepgenArtifactReplacementWithoutJavaNatureFallsBackToUnderlyingArtifact() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(
                dir,
                "artifacts:\n" + "  - coord: com.example:myapp:1.0\n"
                        + "replacements:\n"
                        + "  - coord: "
                        + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "\n" + "    targets:\n"
                        + "      - target: \":depgen\"\n"
                        + "        nature: J2cl\n");

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final ApplicationRecord record = loadApplicationRecord();
        final ArtifactRecord artifact = record.getArtifact(DepGenConfig.getGroupId(), DepGenConfig.getArtifactId());

        assertEquals(artifact.getNatures(), Arrays.asList(Nature.Java, Nature.J2cl));
        assertEquals(artifact.getLabel(Nature.J2cl), ":depgen");
        assertEquals(artifact.getLabel(Nature.Java), ":" + artifact.getName(Nature.Java));
    }

    @Test
    public void ensureDeclaredDepgenArtifactHasJavaNature() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n" + "  - coord: " + DepGenConfig.getCoord() + "\n");

        final ApplicationRecord record = loadApplicationRecord();
        final ApplicationModel model = record.getSource();

        final List<ArtifactModel> artifacts = model.getArtifacts();
        assertEquals(artifacts.size(), 1);
        final ArtifactModel artifactModel = artifacts.get(0);
        assertTrue(
                artifactModel.getNatures(model.getOptions().getDefaultNature()).contains(Nature.Java));
    }

    @Test
    public void ensureDeclaredDepgenArtifactWithoutJavaNatureGeneratesError() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(dir, "artifacts:\n" + "  - coord: " + DepGenConfig.getCoord() + "\n" + "    natures: [J2cl]\n");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(
                exception.getMessage(),
                "Artifact 'org.realityforge.bazel.depgen:bazel-depgen' declared as a dependency but does not declare"
                        + " the Java nature which is required if the verifyConfigSha256 option is set to true.");
    }

    @Test
    public void ensureDeclaredDepgenArtifactWithoutAllClassifierGeneratesError() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        writeConfigFile(
                dir,
                "artifacts:\n" + "  - coord: " + DepGenConfig.getGroupId() + ":" + DepGenConfig.getArtifactId() + "\n");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);

        assertEquals(
                exception.getMessage(),
                "Artifact 'org.realityforge.bazel.depgen:bazel-depgen' declared as a dependency but does not specify"
                        + " the classifier 'all' which is required if the verifyConfigSha256 option is set to true.");
    }

    @Test
    public void repository_with_searchByDefault_false() throws Exception {
        final Path dir1 = FileUtil.createLocalTempDir();
        final Path dir2 = FileUtil.createLocalTempDir();

        deployDepGenArtifactToLocalRepository(dir1);
        writeConfigFile("repositories:\n" + "  - name: local1\n"
                + "    url: "
                + dir1.toUri() + "\n" + "  - name: local2\n"
                + "    url: "
                + dir2.toUri() + "\n" + "    searchByDefault: false\n"
                + "artifacts:\n"
                + "  - coord: com.example:myapp:1.0\n");
        deployArtifactToLocalRepository(dir1, "com.example:myapp:1.0");
        deployArtifactToLocalRepository(dir2, "com.example:myapp:1.0");

        final List<ArtifactRecord> artifacts = loadApplicationRecord().getArtifacts();
        assertTrue(artifacts.size() > 1);

        assertEquals(
                artifacts.get(0).getUrls(),
                Collections.singletonList(dir1.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
    }

    @Test
    public void repository_with_searchByDefault_false_but_artifact_repositories_include() throws Exception {
        final Path dir1 = FileUtil.createLocalTempDir();
        final Path dir2 = FileUtil.createLocalTempDir();

        deployDepGenArtifactToLocalRepository(dir1);
        writeConfigFile("repositories:\n" + "  - name: local1\n"
                + "    url: "
                + dir1.toUri() + "\n" + "  - name: local2\n"
                + "    url: "
                + dir2.toUri() + "\n" + "    searchByDefault: false\n"
                + "artifacts:\n"
                + "  - coord: com.example:myapp:1.0\n"
                + "    repositories: [local1, local2]\n");
        deployArtifactToLocalRepository(dir1, "com.example:myapp:1.0");
        deployArtifactToLocalRepository(dir2, "com.example:myapp:1.0");

        final List<ArtifactRecord> artifacts = loadApplicationRecord().getArtifacts();
        assertTrue(artifacts.size() > 1);

        assertEquals(
                artifacts.get(0).getUrls(),
                Arrays.asList(
                        dir1.toUri() + "com/example/myapp/1.0/myapp-1.0.jar",
                        dir2.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
    }

    @Test
    public void repository_with_searchByDefault_false_and_explicit_repository_contains_transitive_dependency()
            throws Exception {
        final Path dir1 = FileUtil.createLocalTempDir();
        final Path dir2 = FileUtil.createLocalTempDir();

        deployDepGenArtifactToLocalRepository(dir1);
        writeConfigFile("repositories:\n" + "  - name: local1\n"
                + "    url: "
                + dir1.toUri() + "\n" + "  - name: local2\n"
                + "    url: "
                + dir2.toUri() + "\n" + "    searchByDefault: false\n"
                + "artifacts:\n"
                + "  - coord: com.example:myapp:1.0\n"
                + "    repositories: [local2]\n");
        deployArtifactToLocalRepository(dir2, "com.example:myapp:1.0", "com.example:mylib:1.0");
        deployArtifactToLocalRepository(dir2, "com.example:mylib:1.0");

        final List<ArtifactRecord> artifacts = loadApplicationRecord().getArtifacts();
        assertTrue(artifacts.size() > 2);

        final ArtifactRecord myapp = artifacts.stream()
                .filter(a -> a.getKey().equals("com.example:myapp"))
                .findAny()
                .orElseThrow();
        final ArtifactRecord mylib = artifacts.stream()
                .filter(a -> a.getKey().equals("com.example:mylib"))
                .findAny()
                .orElseThrow();
        assertEquals(myapp.getUrls(), Collections.singletonList(dir2.toUri() + "com/example/myapp/1.0/myapp-1.0.jar"));
        assertEquals(mylib.getUrls(), Collections.singletonList(dir2.toUri() + "com/example/mylib/1.0/mylib-1.0.jar"));
    }

    @Test
    public void repository_with_searchByDefault_false_does_not_download_sources_by_default() throws Exception {
        final Path dir1 = FileUtil.createLocalTempDir();
        final Path dir2 = FileUtil.createLocalTempDir();

        deployDepGenArtifactToLocalRepository(dir1);
        writeConfigFile("repositories:\n" + "  - name: local1\n"
                + "    url: "
                + dir1.toUri() + "\n" + "  - name: local2\n"
                + "    url: "
                + dir2.toUri() + "\n" + "    searchByDefault: false\n"
                + "artifacts:\n"
                + "  - coord: com.example:myapp:1.0\n");
        deployTempArtifactToLocalRepository(dir1, "com.example:myapp:1.0");
        deployTempArtifactToLocalRepository(dir2, "com.example:myapp:jar:sources:1.0");

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, this::loadApplicationRecord);
        assertEquals(
                exception.getMessage(),
                "Unable to locate source for artifact 'com.example:myapp:jar:1.0'. Specify the "
                        + "'includeSource' configuration property as 'false' in the artifacts configuration.\n\n"
                        + "Dependency path:\n"
                        + "  com.example:myapp:jar:1.0 [compile]");
    }

    @Test
    public void repository_with_searchByDefault_false_but_artifact_repositories_include_download_sources()
            throws Exception {
        final Path dir1 = FileUtil.createLocalTempDir();
        final Path dir2 = FileUtil.createLocalTempDir();

        deployDepGenArtifactToLocalRepository(dir1);
        writeConfigFile("repositories:\n" + "  - name: local1\n"
                + "    url: "
                + dir1.toUri() + "\n" + "  - name: local2\n"
                + "    url: "
                + dir2.toUri() + "\n" + "    searchByDefault: false\n"
                + "artifacts:\n"
                + "  - coord: com.example:myapp:1.0\n"
                + "    repositories: [local1, local2]\n");
        deployTempArtifactToLocalRepository(dir1, "com.example:myapp:1.0");
        deployTempArtifactToLocalRepository(dir2, "com.example:myapp:jar:sources:1.0");

        final List<ArtifactRecord> artifacts = loadApplicationRecord().getArtifacts();
        assertTrue(artifacts.size() > 1);

        final ArtifactRecord artifactRecord = artifacts.get(0);
        assertEquals(
                artifactRecord.getSourceUrls(),
                Collections.singletonList(dir2.toUri() + "com/example/myapp/1.0/myapp-1.0-sources.jar"));
    }
}
