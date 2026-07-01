package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class ReplacementTargetConfig {
    @Nullable
    private Nature nature;

    @Nullable
    private String target;

    @Nullable
    public Nature getNature() {
        return nature;
    }

    public void setNature(final Nature nature) {
        this.nature = Objects.requireNonNull(nature);
    }

    @Nullable
    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = Objects.requireNonNull(target);
    }
}
