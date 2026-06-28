package org.realityforge.bazel.depgen;

import org.jspecify.annotations.NonNull;

public final class DepgenValidationException extends DepgenException {
    public DepgenValidationException(@NonNull final String message) {
        super(message);
    }
}
