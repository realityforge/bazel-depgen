package org.realityforge.bazel.depgen.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.DepgenException;

/**
 * Utility methods for invoking native commands.
 */
final class Exec {
    private static final int BUFFER_SIZE = 2 * 1024;

    private Exec() {}

    /**
     * Execute a command and capture the output.
     *
     * @param action the callback responsible for setting up ProcessBuilder.
     * @return the output of the command.
     */
    @SuppressWarnings("SameParameterValue")
    @NonNull
    static String capture(@NonNull final Consumer<ProcessBuilder> action, @Nullable final Integer expectedExitCode) {
        final var baos = new ByteArrayOutputStream();
        exec(action, process -> copy(process.getInputStream(), new BufferedOutputStream(baos)), expectedExitCode);
        return baos.toString();
    }

    /**
     * Low level utility for executing a process.
     * This method will return when the process completes.
     *
     * @param action           the callback responsible for setting up ProcessBuilder.
     * @param processHandler   the callback passed a process.
     * @param expectedExitCode the expected exitCode of the process.
     */
    private static void exec(
            @NonNull final Consumer<ProcessBuilder> action,
            @Nullable final Consumer<Process> processHandler,
            @Nullable final Integer expectedExitCode) {
        final var builder = new ProcessBuilder();
        action.accept(builder);
        try {
            final var process = builder.start();
            if (null != processHandler) {
                processHandler.accept(process);
            }
            final var exitCode = process.waitFor();
            if (null != expectedExitCode && exitCode != expectedExitCode) {
                throw new DepgenException("Unexpected exit code for command " + builder.command() + ". " + "Actual: "
                        + exitCode + " Expected: " + expectedExitCode);
            }
        } catch (final IOException ioe) {
            throw new DepgenException("Error starting command " + builder.command(), ioe);
        } catch (final InterruptedException ie) {
            throw new DepgenException("Error waiting for command " + builder.command(), ie);
        }
    }

    private static void copy(@NonNull final InputStream input, @NonNull final OutputStream output) {
        try {
            // Java9 can use input.transferTo(output)
            try (final var in = input;
                    final var out = output) {
                final var buffer = new byte[BUFFER_SIZE];
                var bytesRead = in.read(buffer);
                while (-1 != bytesRead) {
                    out.write(buffer, 0, bytesRead);
                    bytesRead = in.read(buffer);
                }
            }
        } catch (final IOException ioe) {
            throw new DepgenException("Error copying process output", ioe);
        }
    }
}
