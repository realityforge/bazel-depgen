package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.realityforge.bazel.depgen.config.Nature;

public final class ReplacementTargetModel {
    private final Nature _nature;

    private final String _target;

    public ReplacementTargetModel(final Nature nature, final String target) {
        _nature = Objects.requireNonNull(nature);
        _target = Objects.requireNonNull(target);
    }

    public Nature getNature() {
        return _nature;
    }

    public String getTarget() {
        return _target;
    }
}
