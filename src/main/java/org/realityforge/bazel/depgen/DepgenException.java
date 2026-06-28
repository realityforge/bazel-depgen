package org.realityforge.bazel.depgen;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

public class DepgenException extends RuntimeException {
    public DepgenException(@NonNull final String message) {
        super(message);
    }

    public DepgenException(@NonNull final String message, @NonNull final Throwable cause) {
        super(Objects.requireNonNull(message), Objects.requireNonNull(cause));
    }
}
