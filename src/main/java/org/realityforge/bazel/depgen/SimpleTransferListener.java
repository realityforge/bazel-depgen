package org.realityforge.bazel.depgen;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class SimpleTransferListener extends AbstractTransferListener {
    private static final long PROGRESS_INTERVAL_MILLIS = 5000;

    @NonNull
    private final Map<TransferResource, Long> _downloads = new ConcurrentHashMap<>();

    @NonNull
    private final Environment _environment;

    @Nullable
    private final PrintWriter _writer;

    @NonNull
    private final LongSupplier _currentTimeMillis;

    private int _lastLength;

    private long _lastProgressTime;

    SimpleTransferListener(@NonNull final Environment environment) {
        this(
                environment,
                null == environment.console() ? null : environment.console().writer(),
                System::currentTimeMillis);
    }

    SimpleTransferListener(
            @NonNull final Environment environment,
            @Nullable final PrintWriter writer,
            @NonNull final LongSupplier currentTimeMillis) {
        _environment = Objects.requireNonNull(environment);
        _writer = writer;
        _currentTimeMillis = Objects.requireNonNull(currentTimeMillis);
    }

    @Override
    public synchronized void transferInitiated(@NonNull final TransferEvent event) {
        _downloads.put(event.getResource(), 0L);
        if (null != _writer && _environment.logger().isLoggable(Level.FINE)) {
            final var label = TransferEvent.RequestType.PUT == event.getRequestType() ? "Uploading" : "Downloading";
            _writer.println(label + ": " + path(event.getResource()));
        } else if (isInfoProgressEnabled()) {
            _lastProgressTime = _currentTimeMillis.getAsLong();
            writeProgressDot();
        }
    }

    @Override
    public synchronized void transferProgressed(@NonNull final TransferEvent event) {
        if (null != _writer && _environment.logger().isLoggable(Level.FINE)) {
            final var resource = event.getResource();
            _downloads.put(resource, event.getTransferredBytes());

            final var buffer = new StringBuilder(64);

            for (final var entry : _downloads.entrySet()) {
                final var total = entry.getKey().getContentLength();
                final var complete = entry.getValue();

                buffer.append(getStatus(complete, total)).append("  ");
            }

            final var pad = _lastLength - buffer.length();
            _lastLength = buffer.length();
            pad(buffer, pad);
            buffer.append('\r');

            _writer.print(buffer);
            _writer.flush();
        } else if (isInfoProgressEnabled()) {
            final var now = _currentTimeMillis.getAsLong();
            if (now - _lastProgressTime >= PROGRESS_INTERVAL_MILLIS) {
                _lastProgressTime = now;
                writeProgressDot();
            }
        }
    }

    @Override
    public synchronized void transferSucceeded(@NonNull final TransferEvent event) {
        transferCompleted(event);

        if (null != _writer && _environment.logger().isLoggable(Level.FINE)) {
            final var resource = event.getResource();
            final var contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                final var len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

                final var duration = System.currentTimeMillis() - resource.getTransferStartTime();
                final String throughput;
                if (duration > 0) {
                    final var bytes = contentLength - resource.getResumeOffset();
                    final var format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                    final var kbPerSec = (bytes / 1024.0) / (duration / 1000.0);
                    throughput = " at " + format.format(kbPerSec) + " KB/sec";
                } else {
                    throughput = "";
                }

                final var label = TransferEvent.RequestType.PUT == event.getRequestType() ? "Uploaded" : "Downloaded";
                _writer.println(label + ": " + path(resource) + " (" + len + throughput + ")");
            }
        }
    }

    @NonNull
    private String path(@NonNull final TransferResource resource) {
        return resource.getRepositoryUrl() + resource.getResourceName();
    }

    @Override
    public synchronized void transferFailed(@NonNull final TransferEvent event) {
        transferCompleted(event);

        final var exception = event.getException();
        if (!(exception instanceof MetadataNotFoundException) && !(exception instanceof ArtifactNotFoundException)) {
            final var logger = _environment.logger();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Transfer Failed: " + event.getResource().getResourceName(), exception);
            }
        }
    }

    private void transferCompleted(@NonNull final TransferEvent event) {
        _downloads.remove(event.getResource());

        if (null != _writer && (_environment.logger().isLoggable(Level.FINE) || _downloads.isEmpty())) {
            clearProgress();
        }
    }

    @Override
    public void transferCorrupted(@NonNull final TransferEvent event) {
        final var logger = _environment.logger();
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
        buffer.append(" ".repeat(Math.max(0, spaces)));
    }

    private boolean isInfoProgressEnabled() {
        return null != _writer
                && _environment.logger().isLoggable(Level.INFO)
                && !_environment.logger().isLoggable(Level.FINE);
    }

    private void writeProgressDot() {
        final var writer = Objects.requireNonNull(_writer);
        writer.print('.');
        writer.flush();
        _lastLength++;
    }

    private void clearProgress() {
        final var writer = Objects.requireNonNull(_writer);
        if (_lastLength > 0) {
            writer.print('\r');
            writer.print(" ".repeat(_lastLength));
            writer.print('\r');
            writer.flush();
            _lastLength = 0;
        }
    }
}
