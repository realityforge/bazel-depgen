package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.config.Nature;

public final class ReplacementTargetModel {
    @NonNull
    private final Nature _nature;

    @NonNull
    private final String _target;

    public ReplacementTargetModel(@NonNull final Nature nature, @NonNull final String target) {
        _nature = Objects.requireNonNull(nature);
        _target = Objects.requireNonNull(target);
    }

    @NonNull
    public Nature getNature() {
        return _nature;
    }

    @NonNull
    public String getTarget() {
        return _target;
    }
}
