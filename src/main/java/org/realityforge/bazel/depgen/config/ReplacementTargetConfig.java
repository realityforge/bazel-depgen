package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
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

    public void setNature(@NonNull final Nature nature) {
        this.nature = Objects.requireNonNull(nature);
    }

    @Nullable
    public String getTarget() {
        return target;
    }

    public void setTarget(@NonNull final String target) {
        this.target = Objects.requireNonNull(target);
    }
}
