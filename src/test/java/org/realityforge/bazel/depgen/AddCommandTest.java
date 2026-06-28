package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;

public class AddCommandTest extends AbstractTest {
    @Test
    public void add_noArtifactsSection() throws Exception {
        writeConfigFile("options:\n" + "  verifyConfigSha256: false\n");

        final var handler = new TestHandler();
        final var command = new AddCommand();
        final Environment environment = newEnvironment(handler);
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(
                loadAsString(environment.getConfigFile()),
                "options:\n" + "  verifyConfigSha256: false\n" + "artifacts:\n" + "  - coord: com.example:myapp:1.0\n");
        assertOutputContains(
                handler.toString(),
                "Added dependency 'com.example:myapp:jar:1.0' to configuration file " + environment.getConfigFile());
        assertNoTempFiles(environment.getConfigFile().getParent());
    }

    @Test
    public void add_ignoresCommentedTemplateArtifactsSection() throws Exception {
        writeConfigFile("#artifacts:\n" + "  #- coord: com.example:sample:1.0\n");

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(
                loadAsString(environment.getConfigFile()),
                "#artifacts:\n" + "  #- coord: com.example:sample:1.0\n"
                        + "artifacts:\n"
                        + "  - coord: com.example:myapp:1.0\n");
    }

    @Test
    public void add_existingArtifactsSection() throws Exception {
        writeConfigFile("artifacts:\n" + "  - coord: com.example:old:1.0\n"
                + "\n"
                + "excludes:\n"
                + "  - coord: com.example:unused\n");

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(
                loadAsString(environment.getConfigFile()),
                "artifacts:\n" + "  - coord: com.example:old:1.0\n"
                        + "  - coord: com.example:myapp:1.0\n"
                        + "\n"
                        + "excludes:\n"
                        + "  - coord: com.example:unused\n");
    }

    @Test
    public void add_allOptions() throws Exception {
        writeConfigFile("repositories:\n" + "  - name: central\n"
                + "    url: https://repo.maven.apache.org/maven2/\n"
                + "options:\n"
                + "  includeSource: false\n"
                + "artifacts:\n"
                + "  - coord: org.example:old:1.0\n");

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
        assertEquals(
                loadAsString(environment.getConfigFile()),
                "repositories:\n" + "  - name: central\n"
                        + "    url: https://repo.maven.apache.org/maven2/\n"
                        + "options:\n"
                        + "  includeSource: false\n"
                        + "artifacts:\n"
                        + "  - coord: org.example:old:1.0\n"
                        + "  - coord: org.example:lib:1.2\n"
                        + "    nameStrategy: ArtifactId\n"
                        + "    repositoryName: lib_repo\n"
                        + "    includeOptional: true\n"
                        + "    includeSource: true\n"
                        + "    includeExternalAnnotations: true\n"
                        + "    repositories: [\"central\"]\n"
                        + "    excludes: [\"com.example:base\"]\n"
                        + "    visibility: [\"//visibility:public\"]\n"
                        + "    natures: [Java, Plugin, J2cl]\n"
                        + "    java:\n"
                        + "      name: lib_java\n"
                        + "      exportDeps: true\n"
                        + "    j2cl:\n"
                        + "      name: lib_j2cl\n"
                        + "      mode: Library\n"
                        + "      suppress: [\"checkDebuggerStatement\"]\n"
                        + "    plugin:\n"
                        + "      name: lib_plugin\n"
                        + "      generatesApi: false\n");
    }

    @Test
    public void add_repositoryNameStrategy() throws Exception {
        writeConfigFile("options:\n" + "  verifyConfigSha256: false\n");

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(
                environment, "org.example:lib:1.2", "--repository-name-strategy", "GroupIdAndArtifactId"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(
                loadAsString(environment.getConfigFile()),
                "options:\n" + "  verifyConfigSha256: false\n"
                        + "artifacts:\n"
                        + "  - coord: org.example:lib:1.2\n"
                        + "    repositoryNameStrategy: GroupIdAndArtifactId\n");
    }

    @Test
    public void add_noIncludeSource() throws Exception {
        writeConfigFile("options:\n" + "  verifyConfigSha256: false\n");

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "org.example:lib:1.2", "--no-include-source"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(
                loadAsString(environment.getConfigFile()),
                "options:\n" + "  verifyConfigSha256: false\n"
                        + "artifacts:\n"
                        + "  - coord: org.example:lib:1.2\n"
                        + "    includeSource: false\n");
    }

    @Test
    public void add_j2clNatureRequiresIncludeSource() throws Exception {
        final String original = "options:\n" + "  includeSource: false\n";
        writeConfigFile(original);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0", "--nature", "J2cl"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(
                exception.getMessage(), "Dependencies with the J2cl nature require includeSource to resolve to true.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
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
        final String original = "options:\n" + "  verifyConfigSha256: false\n";
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
        final String original = "options:\n" + "  defaultNature: Java\n";
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
    public void add_unsupportedInlineArtifactsSection() throws Exception {
        final String original = "artifacts: []\n";
        writeConfigFile(original);

        final var command = new AddCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(exception.getMessage(), "The add command only supports block-style artifacts sections.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
        assertNoTempFiles(environment.getConfigFile().getParent());
    }

    @Test
    public void add_candidateValidationFailureLeavesFileUnchangedAndCleansTempFile() throws Exception {
        final String original = "artifacts:\n" + "  - coord: com.example:old:1.0\n";
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
        assertNoTempFiles(environment.getConfigFile().getParent());
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

    private void assertNoTempFiles(@Nonnull final Path configDirectory) throws Exception {
        try (var stream = Files.list(configDirectory)) {
            assertFalse(stream.anyMatch(p -> p.getFileName().toString().startsWith(".dependencies")));
        }
    }
}
