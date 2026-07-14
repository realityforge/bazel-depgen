package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.testng.annotations.Test;

public final class SimpleTransferListenerTest {
    @Test
    public void infoProgressIsThrottledAndTransient() {
        final var logger = newLogger(Level.INFO);
        final var output = new StringWriter();
        final var now = new AtomicLong(100);
        final var listener = new SimpleTransferListener(
                new Environment(null, Path.of(""), logger), new PrintWriter(output), now::get);
        final var resource = newResource();

        listener.transferInitiated(event(resource, 0));
        now.set(5099);
        listener.transferProgressed(event(resource, 1024));
        now.set(5100);
        listener.transferProgressed(event(resource, 2048));
        listener.transferSucceeded(event(resource, 2048));

        assertEquals(output.toString(), "..\r  \r");
    }

    @Test
    public void verboseOutputIncludesTransferDetails() {
        final var logger = newLogger(Level.FINE);
        final var output = new StringWriter();
        final var listener = new SimpleTransferListener(
                new Environment(null, Path.of(""), logger), new PrintWriter(output), () -> 0);
        final var resource = newResource().setContentLength(2048);

        listener.transferInitiated(event(resource, 0));
        listener.transferProgressed(event(resource, 1024));
        listener.transferSucceeded(event(resource, 2048));

        assertTrue(output.toString().startsWith("Downloading: https://example.com/artifact.jar\n"));
        assertTrue(output.toString().contains("1/2 KB"));
        assertTrue(output.toString().contains("Downloaded: https://example.com/artifact.jar (2 KB"));
    }

    @Test
    public void quietOutputSuppressesSuccessfulTransfers() {
        final var logger = newLogger(Level.WARNING);
        final var output = new StringWriter();
        final var listener = new SimpleTransferListener(
                new Environment(null, Path.of(""), logger), new PrintWriter(output), () -> 0);
        final var resource = newResource();

        listener.transferInitiated(event(resource, 0));
        listener.transferProgressed(event(resource, 1024));
        listener.transferSucceeded(event(resource, 1024));

        assertEquals(output.toString(), "");
    }

    @Test
    public void transferFailureIsVerboseOnly() {
        final var handler = new TestHandler();
        handler.setLevel(Level.ALL);
        final var logger = newLogger(Level.INFO);
        logger.addHandler(handler);
        final var listener = new SimpleTransferListener(
                new Environment(null, Path.of(""), logger), new PrintWriter(new StringWriter()), () -> 0);
        final var resource = newResource();
        final var event = new TransferEvent.Builder(new DefaultRepositorySystemSession(), resource)
                .setRequestType(TransferEvent.RequestType.GET)
                .setException(new IOException("Failed"))
                .build();

        listener.transferFailed(event);
        assertEquals(handler.toString(), "");

        logger.setLevel(Level.FINE);
        listener.transferFailed(event);
        assertEquals(handler.toString(), "Transfer Failed: artifact.jar");
    }

    private static Logger newLogger(final Level level) {
        final var logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(level);
        return logger;
    }

    private static TransferResource newResource() {
        return new TransferResource("central", "https://example.com/", "artifact.jar", new File("artifact.jar"), null);
    }

    private static TransferEvent event(final TransferResource resource, final long transferredBytes) {
        return new TransferEvent.Builder(new DefaultRepositorySystemSession(), resource)
                .setRequestType(TransferEvent.RequestType.GET)
                .setTransferredBytes(transferredBytes)
                .build();
    }
}
