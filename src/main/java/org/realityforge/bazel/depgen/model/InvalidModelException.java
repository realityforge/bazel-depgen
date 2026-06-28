package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class InvalidModelException extends RuntimeException {
    @NonNull
    private final Object _model;

    public InvalidModelException(@Nullable final String message, @NonNull final Object model) {
        super(message);
        _model = Objects.requireNonNull(model);
    }

    @NonNull
    public Object getModel() {
        return _model;
    }
}
