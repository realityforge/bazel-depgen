package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.testng.annotations.Test;

public class RawFormatterTest {
    @Test
    public void format_withoutThrowable() {
        final var record = new LogRecord(Level.INFO, "Hello");

        assertEquals(new RawFormatter().format(record), "Hello\n");
    }

    @Test
    public void format_withThrowable() {
        final var record = new LogRecord(Level.INFO, "Hello");
        record.setThrown(new IllegalStateException("Boom"));

        final String output = new RawFormatter().format(record);
        assertTrue(output.startsWith("Hello\njava.lang.IllegalStateException: Boom\n"));
        assertTrue(output.contains("RawFormatterTest.format_withThrowable"));
    }
}
