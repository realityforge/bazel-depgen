package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

public class InfoCommandTest extends AbstractTest {
    @Test
    public void info() throws Exception {
        writeWorkspace();
        writeConfigFile("");

        final var handler = new TestHandler();
        final var command = new InfoCommand();
        final Environment environment = newEnvironment(handler);
        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String output = handler.toString();
        assertOutputContains(output, "config-file=" + environment.getConfigFile() + "\n");
        assertOutputContains(output, "settings-file=" + environment.getSettingsFile() + "\n");
        assertOutputContains(output, "cache-directory=" + environment.getCacheDir() + "\n");
        assertOutputContains(output, "reset-cached-metadata=false\n");
        assertOutputContains(output, "bazel-repository-cache=");
    }

    @Test
    public void info_single_value() throws Exception {
        writeWorkspace();
        writeConfigFile("");

        final var handler = new TestHandler();
        final var command = new InfoCommand();
        final Environment environment = newEnvironment(handler);
        command.processOptions(environment, "config-file");
        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String output = handler.toString();
        assertEquals(output, "config-file=" + environment.getConfigFile());
    }

    @Test
    public void info_multiple_values() throws Exception {
        writeWorkspace();
        writeConfigFile("");

        final var handler = new TestHandler();
        final var command = new InfoCommand();
        final Environment environment = newEnvironment(handler);
        command.processOptions(environment, "config-file", "settings-file");
        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String output = handler.toString();
        assertEquals(
                output,
                "config-file=" + environment.getConfigFile() + "\n" + "settings-file=" + environment.getSettingsFile());
    }

    @Test
    public void info_zero_values() throws Exception {
        writeWorkspace();
        writeConfigFile("");

        final var handler = new TestHandler();
        final var command = new InfoCommand();
        final Environment environment = newEnvironment(handler);
        command.processOptions(environment, "XXXXX");
        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        final String output = handler.toString();
        assertEquals(output, "");
    }

    @Test
    public void info_skipsOutputWhenWarningsAreDisabled() throws Exception {
        writeWorkspace();
        writeConfigFile("");

        final var handler = new TestHandler();
        final Logger logger = createLogger(handler);
        logger.setLevel(Level.SEVERE);
        final var command = new InfoCommand();
        final Environment environment = newEnvironment(logger);
        final int exitCode = command.run(new CommandContextImpl(environment));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(handler.toString(), "");
    }

    @Test
    public void info_badArgs() throws Exception {
        writeWorkspace();
        writeConfigFile("");

        final var handler = new TestHandler();
        final var command = new InfoCommand();
        final Environment environment = newEnvironment(handler);
        final boolean parsed = command.processOptions(environment, "--something-something");
        assertFalse(parsed);
        assertEquals(handler.toString(), "Error: Unknown option --something-something");
    }

    @Test
    public void info_help() throws Exception {
        final var handler = new TestHandler();
        final var command = new InfoCommand();
        final boolean parsed = command.processOptions(newEnvironment(handler), "--help");
        assertFalse(parsed);
        final String output = handler.toString();
        assertOutputContains(output, "info Options:");
        assertOutputContains(output, "--help");
    }
}
