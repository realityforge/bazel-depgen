package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class InvalidModelException extends RuntimeException {
    private final Object _model;

    public InvalidModelException(@Nullable final String message, final Object model) {
        super(message);
        _model = Objects.requireNonNull(model);
    }

    public Object getModel() {
        return _model;
    }
}
