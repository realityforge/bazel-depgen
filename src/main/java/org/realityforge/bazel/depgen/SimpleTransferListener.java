package org.realityforge.bazel.depgen;

import java.io.Console;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.jspecify.annotations.NonNull;

final class SimpleTransferListener extends AbstractTransferListener {
    @NonNull
    private final Map<TransferResource, Long> _downloads = new ConcurrentHashMap<>();

    @NonNull
    private final Environment _environment;

    private int lastLength;

    SimpleTransferListener(@NonNull final Environment environment) {
        _environment = Objects.requireNonNull(environment);
    }

    @Override
    public void transferInitiated(@NonNull final TransferEvent event) {
        final Console console = _environment.console();
        if (null != console && _environment.logger().isLoggable(Level.INFO)) {
            final String label = TransferEvent.RequestType.PUT == event.getRequestType() ? "Uploading" : "Downloading";
            console.writer().println(label + ": " + path(event.getResource()));
        }
    }

    @Override
    public void transferProgressed(@NonNull final TransferEvent event) {
        final Console console = _environment.console();
        if (null != console && _environment.logger().isLoggable(Level.INFO)) {
            final TransferResource resource = event.getResource();
            _downloads.put(resource, event.getTransferredBytes());

            final var buffer = new StringBuilder(64);

            for (final Map.Entry<TransferResource, Long> entry : _downloads.entrySet()) {
                final long total = entry.getKey().getContentLength();
                final long complete = entry.getValue();

                buffer.append(getStatus(complete, total)).append("  ");
            }

            final int pad = lastLength - buffer.length();
            lastLength = buffer.length();
            pad(buffer, pad);
            buffer.append('\r');

            console.writer().print(buffer);
        }
    }

    @Override
    public void transferSucceeded(@NonNull final TransferEvent event) {
        transferCompleted(event);

        final Console console = _environment.console();
        if (null != console && _environment.logger().isLoggable(Level.INFO)) {
            final TransferResource resource = event.getResource();
            final long contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                final String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

                final long duration = System.currentTimeMillis() - resource.getTransferStartTime();
                final String throughput;
                if (duration > 0) {
                    final long bytes = contentLength - resource.getResumeOffset();
                    final var format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                    final double kbPerSec = (bytes / 1024.0) / (duration / 1000.0);
                    throughput = " at " + format.format(kbPerSec) + " KB/sec";
                } else {
                    throughput = "";
                }

                final String label =
                        TransferEvent.RequestType.PUT == event.getRequestType() ? "Uploaded" : "Downloaded";
                console.writer().println(label + ": " + path(resource) + " (" + len + throughput + ")");
            }
        }
    }

    @NonNull
    private String path(@NonNull final TransferResource resource) {
        return resource.getRepositoryUrl() + resource.getResourceName();
    }

    @Override
    public void transferFailed(@NonNull final TransferEvent event) {
        transferCompleted(event);

        final Exception exception = event.getException();
        if (!(exception instanceof MetadataNotFoundException) && !(exception instanceof ArtifactNotFoundException)) {
            final Logger logger = _environment.logger();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Transfer Failed: " + event.getResource().getResourceName(), exception);
            }
        }
    }

    private void transferCompleted(@NonNull final TransferEvent event) {
        _downloads.remove(event.getResource());

        final Console console = _environment.console();
        if (null != console && _environment.logger().isLoggable(Level.INFO)) {
            final var buffer = new StringBuilder(64);
            pad(buffer, lastLength);
            buffer.append('\r');
            console.writer().print(buffer);
        }
    }

    @Override
    public void transferCorrupted(@NonNull final TransferEvent event) {
        final Logger logger = _environment.logger();
        if (logger.isLoggable(Level.WARNING)) {
            logger.log(
                    Level.WARNING,
                    "Transfer Corrupted: " + event.getResource().getResourceName() + " due to " + event.getException());
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, null, event.getException());
        }
    }

    @NonNull
    private String getStatus(final long complete, final long total) {
        if (total >= 1024) {
            return toKB(complete) + "/" + toKB(total) + " KB ";
        } else if (total >= 0) {
            return complete + "/" + total + " B ";
        } else if (complete >= 1024) {
            return toKB(complete) + " KB ";
        } else {
            return complete + " B ";
        }
    }

    private long toKB(final long bytes) {
        return (bytes + 1023) / 1024;
    }

    private void pad(@NonNull final StringBuilder buffer, final int spaces) {
        for (int i = 0; i < spaces; i++) {
            buffer.append(' ');
        }
    }
}
