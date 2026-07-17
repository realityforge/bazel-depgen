package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public class AddCommandTest extends AbstractTest {
    @Test
    public void add_noArtifactsSection() throws Exception {
        writeConfigFile("""
            options:
              verifyConfigSha256: false
            """);

        final var handler = new TestHandler();
        final var command = new AddCommand();
        final Environment environment = newEnvironment(handler);
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            options:
              verifyConfigSha256: false
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        assertOutputContains(
                handler.toString(),
                "Added dependency 'com.example:myapp:jar:1.0' to configuration file " + environment.getConfigFile());
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void add_emptyConfig() throws Exception {
        writeConfigFile("");

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
    }

    @Test
    public void add_ignoresCommentedTemplateArtifactsSection() throws Exception {
        writeConfigFile("""
            #artifacts:
              #- coord: com.example:sample:1.0
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            #artifacts:
            #- coord: com.example:sample:1.0
            artifacts:
              - coord: com.example:myapp:1.0
            """);
    }

    @Test
    public void add_existingArtifactsSection() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:old:1.0

            excludes:
              - coord: com.example:unused
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:old:1.0
              - coord: com.example:myapp:1.0

            excludes:
              - coord: com.example:unused
            """);
    }

    @Test
    public void add_existingArtifactsSectionWithComments() throws Exception {
        writeConfigFile("""
            # Dependencies
            artifacts:
              # Existing dependency
              - coord: com.example:old:1.0
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            # Dependencies
            artifacts:
              - # Existing dependency
                coord: com.example:old:1.0
              - coord: com.example:myapp:1.0
            """);
    }

    @Test
    public void add_allOptions() throws Exception {
        writeConfigFile("""
            repositories:
              - name: central
                url: https://repo.maven.apache.org/maven2/
            options:
              includeSource: false
            artifacts:
              - coord: org.example:old:1.0
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(
                environment,
                "org.example:lib:1.2",
                "--nature",
                "java",
                "--nature",
                "plugin",
                "--nature",
                "j2cl",
                "--name-strategy",
                "ArtifactId",
                "--repository-name",
                "lib_repo",
                "--include-optional",
                "--include-source",
                "--include-external-annotations",
                "--repository",
                "central",
                "--exclude",
                "com.example:base",
                "--visibility",
                "//visibility:public",
                "--java-name",
                "lib_java",
                "--java-export-deps",
                "--j2cl-name",
                "lib_j2cl",
                "--j2cl-mode",
                "Library",
                "--j2cl-suppress",
                "checkDebuggerStatement",
                "--plugin-name",
                "lib_plugin",
                "--plugin-generates-api",
                "false"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            repositories:
              - name: central
                url: https://repo.maven.apache.org/maven2/
            options:
              includeSource: false
            artifacts:
              - coord: org.example:old:1.0
              - coord: org.example:lib:1.2
                nameStrategy: ArtifactId
                repositoryName: lib_repo
                includeOptional: true
                includeSource: true
                includeExternalAnnotations: true
                repositories: ["central"]
                excludes: ["com.example:base"]
                visibility: ["//visibility:public"]
                natures: [Java, Plugin, J2cl]
                java:
                  name: lib_java
                  exportDeps: true
                j2cl:
                  name: lib_j2cl
                  mode: Library
                  suppress: ["checkDebuggerStatement"]
                plugin:
                  name: lib_plugin
                  generatesApi: false
            """);
    }

    @Test
    public void add_repositoryNameStrategy() throws Exception {
        writeConfigFile("""
            options:
              verifyConfigSha256: false
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(
                environment, "org.example:lib:1.2", "--repository-name-strategy", "GroupIdAndArtifactId"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            options:
              verifyConfigSha256: false
            artifacts:
              - coord: org.example:lib:1.2
                repositoryNameStrategy: GroupIdAndArtifactId
            """);
    }

    @Test
    public void add_noIncludeSource() throws Exception {
        writeConfigFile("""
            options:
              verifyConfigSha256: false
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "org.example:lib:1.2", "--no-include-source"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            options:
              verifyConfigSha256: false
            artifacts:
              - coord: org.example:lib:1.2
                includeSource: false
            """);
    }

    @Test
    public void add_j2clImportWithoutSources() throws Exception {
        writeConfigFile("""
            options:
              includeSource: false
            """);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(
                environment, "com.example:myapp:1.0", "--nature", "J2cl", "--j2cl-mode", "Import"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            options:
              includeSource: false
            artifacts:
              - coord: com.example:myapp:1.0
                natures: [J2cl]
                j2cl:
                  mode: Import
            """);
    }

    @Test
    public void add_conflictingIncludeSourceOptions() throws Exception {
        final var handler = new TestHandler();
        final var command = new AddCommand();
        assertFalse(command.processOptions(
                newEnvironment(handler), "com.example:myapp:1.0", "--include-source", "--no-include-source"));
        assertEquals(
                handler.toString(), "Error: Only one of --include-source or --no-include-source may be specified.");
    }

    @Test
    public void add_j2clSuppressRequiresLibraryMode() throws Exception {
        final String original = """
            options:
              verifyConfigSha256: false
            """;
        writeConfigFile(original);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(
                environment,
                "com.example:myapp:1.0",
                "--nature",
                "J2cl",
                "--j2cl-mode",
                "Import",
                "--j2cl-suppress",
                "checkDebuggerStatement"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(exception.getMessage(), "The --j2cl-suppress option is incompatible with --j2cl-mode Import.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void add_duplicate() throws Exception {
        for (final String existingCoord : Arrays.asList(
                "com.example:myapp",
                "com.example:myapp:2.0",
                "com.example:myapp:aar:2.0",
                "com.example:myapp:jar:sources:2.0")) {
            final String original = "artifacts:\n" + "  - coord: " + existingCoord + "\n";
            writeConfigFile(original);

            final var command = new AddCommand();
            final Environment environment = newEnvironment();
            assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

            final DepgenValidationException exception = expectThrows(
                    DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
            assertEquals(exception.getMessage(), "Dependency 'com.example:myapp' already exists in configuration.");
            assertEquals(loadAsString(environment.getConfigFile()), original);
        }
    }

    @Test
    public void add_natureSpecificOptionWithoutMatchingNature() throws Exception {
        final String original = """
            options:
              defaultNature: Java
            """;
        writeConfigFile(original);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0", "--j2cl-name", "myapp_j2cl"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(
                exception.getMessage(), "The J2CL-specific options require the dependency to have the J2cl nature.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void add_inlineArtifactsSection() throws Exception {
        writeConfigFile("artifacts: []\n");

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:myapp:1.0
            """);
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void add_candidateValidationFailureLeavesFileUnchangedAndCleansTempFile() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:old:1.0
            """;
        writeConfigFile(original);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0", "--repository", "missing"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(
                exception.getMessage(),
                "Artifact 'com.example:myapp' declared a repository named 'missing' but no such repository is declared"
                        + " in the repository section. Known repositories include: central");
        assertEquals(loadAsString(environment.getConfigFile()), original);
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void add_missingCoord() throws Exception {
        final var handler = new TestHandler();
        final var command = new AddCommand();
        assertFalse(command.processOptions(newEnvironment(handler)));
        assertEquals(handler.toString(), "Error: Missing dependency coordinate.");
    }

    @Test
    public void add_invalidEnum() throws Exception {
        final var handler = new TestHandler();
        final var command = new AddCommand();
        assertFalse(command.processOptions(newEnvironment(handler), "com.example:myapp:1.0", "--nature", "Nope"));
        assertEquals(handler.toString(), "Error: Invalid value for --nature: Nope");
    }

    private void assertNoTempFiles(@NonNull final Path configDirectory) throws Exception {
        try (var stream = Files.list(configDirectory)) {
            assertFalse(stream.anyMatch(p -> p.getFileName().toString().startsWith(".dependencies")));
        }
    }
}
