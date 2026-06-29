package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

public class PrintGraphCommandTest extends AbstractTest {
    @Test
    public void processOptions_acceptsNoArguments() throws Exception {
        assertTrue(new PrintGraphCommand().processOptions(newEnvironment()));
    }

    @Test
    public void processOptions_rejectsUnknownArguments() throws Exception {
        final var handler = new TestHandler();
        final var command = new PrintGraphCommand();
        final boolean parsed = command.processOptions(newEnvironment(handler), "unexpected");
        assertFalse(parsed);
        assertEquals(handler.toString(), "Error: Unknown arguments to print-graph command. Arguments: [unexpected]");
    }

    @Test
    public void run_printGraph() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        writeWorkspace();
        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");

        final var handler = new TestHandler();
        handler.setLevel(Level.INFO);
        final var command = new PrintGraphCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment(handler)));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(handler.toString(), "Dependency Graph:\n" + "\\- com.example:myapp:jar:1.0 [compile]");
    }

    @Test
    public void run_skipsOutputWhenWarningsAreDisabled() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        deployArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        writeWorkspace();
        writeConfigFile(dir, "artifacts:\n  - coord: com.example:myapp:1.0\n");

        final var handler = new TestHandler();
        final Logger logger = createLogger(handler);
        logger.setLevel(Level.SEVERE);
        final var command = new PrintGraphCommand();
        final int exitCode = command.run(new CommandContextImpl(newEnvironment(logger)));
        assertEquals(exitCode, ExitCodes.SUCCESS_EXIT_CODE);
        assertEquals(handler.toString(), "");
    }
}
