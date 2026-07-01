package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public class RemoveCommandTest extends AbstractTest {
    @Test
    public void remove_existingArtifact() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:old:1.0
              - coord: com.example:myapp:1.0
              - coord: com.example:other:jar:sources:2.0
            """);

        final var handler = new TestHandler();
        final var command = new RemoveCommand();
        final Environment environment = newEnvironment(handler);
        assertTrue(command.processOptions(environment, "com.example:myapp:2.0"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), """
            artifacts:
              - coord: com.example:old:1.0
              - coord: com.example:other:jar:sources:2.0
            """);
        assertOutputContains(
                handler.toString(),
                "Removed dependency 'com.example:myapp:jar:1.0' from configuration file "
                        + environment.getConfigFile());
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void remove_lastArtifactLeavesEmptySequence() throws Exception {
        writeConfigFile("""
            artifacts:
              - coord: com.example:myapp:1.0
            """);

        final var command = new RemoveCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(loadAsString(environment.getConfigFile()), "artifacts: []\n");
    }

    @Test
    public void remove_inlineArtifactsSection() throws Exception {
        writeConfigFile("artifacts: [{coord: com.example:myapp:1.0}, {coord: com.example:other:1.0}]\n");

        final var command = new RemoveCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String content = loadAsString(environment.getConfigFile());
        assertFalse(content.contains("com.example:myapp"));
        assertTrue(content.contains("com.example:other:1.0"));
        assertEquals(loadApplicationModel().getArtifacts().size(), 1);
        assertEquals(loadApplicationModel().getArtifacts().get(0).getGroup(), "com.example");
        assertEquals(loadApplicationModel().getArtifacts().get(0).getId(), "other");
    }

    @Test
    public void remove_preservesUnrelatedComments() throws Exception {
        writeConfigFile("""
            # Dependencies
            artifacts:
              # Legacy dependency
              - coord: com.example:old:1.0
              # Remove me
              - coord: com.example:myapp:1.0
              # Keep me
              - coord: com.example:other:1.0
            """);

        final var command = new RemoveCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp"));

        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String content = loadAsString(environment.getConfigFile());
        assertTrue(content.contains("# Dependencies"));
        assertTrue(content.contains("Legacy dependency"));
        assertTrue(content.contains("Keep me"));
        assertFalse(content.contains("Remove me"));
        assertFalse(content.contains("com.example:myapp"));
        assertTrue(content.contains("com.example:old:1.0"));
        assertTrue(content.contains("com.example:other:1.0"));
    }

    @Test
    public void remove_noMatch() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:old:1.0
            """;
        writeConfigFile(original);

        final var command = new RemoveCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(exception.getMessage(), "Dependency 'com.example:myapp' does not exist in configuration.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
        assertNoTempFiles(requireNonNull(environment.getConfigFile().getParent()));
    }

    @Test
    public void remove_ambiguousMatch() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:myapp:1.0
              - coord: com.example:myapp:jar:sources:1.0
            """;
        writeConfigFile(original);

        final var command = new RemoveCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp"));

        final DepgenValidationException exception =
                expectThrows(DepgenValidationException.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(
                exception.getMessage(), "Dependency 'com.example:myapp' is declared multiple times in configuration.");
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void remove_requiresCurrentConfigToLoad() throws Exception {
        final String original = """
            artifacts:
              - coord: com.example:myapp:1.0
              - natures: [Java]
            """;
        writeConfigFile(original);

        final var command = new RemoveCommand();
        final Environment environment = newEnvironment();
        assertTrue(command.processOptions(environment, "com.example:myapp"));

        expectThrows(Exception.class, () -> command.run(new CommandContextImpl(environment)));
        assertEquals(loadAsString(environment.getConfigFile()), original);
    }

    @Test
    public void remove_missingCoord() throws Exception {
        final var handler = new TestHandler();
        final var command = new RemoveCommand();
        assertFalse(command.processOptions(newEnvironment(handler)));
        assertEquals(handler.toString(), "Error: Missing dependency coordinate.");
    }

    @Test
    public void remove_extraCoord() throws Exception {
        final var handler = new TestHandler();
        final var command = new RemoveCommand();
        assertFalse(command.processOptions(newEnvironment(handler), "com.example:myapp", "com.example:other"));
        assertEquals(handler.toString(), "Error: Invalid argument: com.example:other");
    }

    private void assertNoTempFiles(@NonNull final Path configDirectory) throws Exception {
        try (var stream = Files.list(configDirectory)) {
            assertFalse(stream.anyMatch(p -> p.getFileName().toString().startsWith(".dependencies")));
        }
    }
}
