package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ExcludeConfig {
    @Nullable
    private String coord;

    @Nullable
    public String getCoord() {
        return coord;
    }

    public void setCoord(@NonNull final String coord) {
        this.coord = Objects.requireNonNull(coord);
    }
}
