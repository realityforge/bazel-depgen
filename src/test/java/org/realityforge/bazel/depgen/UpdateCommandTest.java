package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public class UpdateCommandTest extends AbstractTest {
    @Test
    public void update_existingArtifact() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: org.example:old:1.0
              - coord: com.example:myapp:1.0
              - coord: com.example:other:jar:sources:2.0
            """);

        final var handler = new TestHandler();
        final var command = new UpdateCommand();
        final Environment environment = newEnvironment(handler);
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: org.example:old:1.0
              - coord: com.example:myapp:2.0
              - coord: com.example:other:jar:sources:2.0
            """);
        assertOutputContains(
                handler.toString(),
                "Updated dependency 'com.example:myapp:jar:1.0' to 'com.example:myapp:jar:2.0' "
                        + "in configuration file " + environment.getConfigFile());
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void update_existingArtifactWhenInfoNotLoggable() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        final var handler = new TestHandler();
        final var command = new UpdateCommand();
        final Environment environment = newEnvironment(handler);
        environment.logger().setLevel(java.util.logging.Level.WARNING);
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:myapp:2.0
            """);
        assertEquals(handler.toString(), "");
    }

    @Test
    public void update_unversionedArtifact() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:myapp
            """);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:myapp:2.0
            """);
        assertEquals(loadApplicationModel().getArtifacts().get(0).getVersion(), "2.0");
    }

    @Test
    public void update_preservesType() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:myapp:test-jar:1.0
            """);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:myapp:test-jar:2.0
            """);
    }

    @Test
    public void update_preservesTypeAndClassifier() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:myapp:test-jar:sources:1.0
            """);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:myapp:test-jar:sources:2.0
            """);
    }

    @Test
    public void update_preservesQuotedCoordStyle() throws Exception {
        writeConfigFile("""
            artifacts:
              - nameStrategy: ArtifactId
                coord: "com.example:myapp:1.0"
            """);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - nameStrategy: ArtifactId
                coord: "com.example:myapp:2.0"
            """);
    }

    @Test
    public void update_inlineArtifactsSection() throws Exception {
        writeConfigFile("artifacts: [{coord: com.example:myapp:1.0}, {coord: com.example:other:1.0}]\n");

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String content = loadAsString(environment.getConfigFile());
        assertFalse(content.contains("com.example:myapp:1.0"));
        assertTrue(content.contains("com.example:myapp:2.0"));
        assertTrue(content.contains("com.example:other:1.0"));
        assertEquals(loadApplicationModel().getArtifacts().get(0).getVersion(), "2.0");
    }

    @Test
    public void update_preservesUnrelatedComments() throws Exception {
        writeConfigFile("""
            # Dependencies
            artifacts:
              # Existing dependency
              - coord: com.example:myapp:1.0
              # Keep me
              - coord: com.example:other:1.0
            """);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String content = loadAsString(environment.getConfigFile());
        assertTrue(content.contains("# Dependencies"));
        assertTrue(content.contains("Existing dependency"));
        assertTrue(content.contains("Keep me"));
        assertTrue(content.contains("com.example:myapp:2.0"));
        assertFalse(content.contains("com.example:myapp:1.0"));
        assertTrue(content.contains("com.example:other:1.0"));
    }

    @Test
    public void update_noMatch() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:old:1.0
            """;
        writeConfigFile(original);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(exception.getMessage(), "Dependency 'com.example:myapp' does not exist in configuration.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void update_ambiguousMatch() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:myapp:1.0
              - coord: com.example:myapp:jar:sources:1.0
            """;
        writeConfigFile(original);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(
                exception.getMessage(), "Dependency 'com.example:myapp' is declared multiple times in configuration.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void update_requiresCurrentConfigToLoad() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:myapp:1.0
              - natures: [Java]
            """;
        writeConfigFile(original);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0"));

        expectThrows(Exception.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void update_requiresTwoPartCoord() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:myapp:1.0
            """;
        writeConfigFile(original);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp:1.0", "2.0"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(
                exception.getMessage(), "The update command dependency coordinate must be in the form 'group:id'.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void update_rejectsColonInVersion() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:myapp:1.0
            """;
        writeConfigFile(original);

        final var command = new UpdateCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp", "2.0:broken"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(exception.getMessage(), "Dependency version must not contain ':'.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void update_missingCoord() throws Exception {
        final var handler = new TestHandler();
        final var command = new UpdateCommand();
        assertFalse(command.processOptions(newEnvironment(handler)));
        assertEquals(handler.toString(), "Error: Missing dependency coordinate.");
    }

    @Test
    public void update_missingVersion() throws Exception {
        final var handler = new TestHandler();
        final var command = new UpdateCommand();
        assertFalse(command.processOptions(newEnvironment(handler), "com.example:myapp"));
        assertEquals(handler.toString(), "Error: Missing dependency version.");
    }

    @Test
    public void update_extraArgument() throws Exception {
        final var handler = new TestHandler();
        final var command = new UpdateCommand();
        assertFalse(command.processOptions(newEnvironment(handler), "com.example:myapp", "2.0", "extra"));
        assertEquals(handler.toString(), "Error: Invalid argument: extra");
    }

    private void assertNoTempFiles(@NonNull final Path configDirectory) throws Exception {
        try (var stream = Files.list(configDirectory)) {
            assertFalse(stream.anyMatch(p -> p.getFileName().toString().startsWith(".dependencies")));
        }
    }
}
