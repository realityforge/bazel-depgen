package org.realityforge.bazel.depgen;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class DepgenConfigurationException extends DepgenException {
    public DepgenConfigurationException(@NonNull final String message) {
        super(Objects.requireNonNull(message));
    }

    public DepgenConfigurationException(@NonNull final String message, @NonNull final Throwable cause) {
        super(message, cause);
    }
}
