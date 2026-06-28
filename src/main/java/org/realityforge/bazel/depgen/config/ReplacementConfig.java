package org.realityforge.bazel.depgen.config;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ReplacementConfig {
    @Nullable
    private String coord;

    @Nullable
    private List<ReplacementTargetConfig> targets;

    @Nullable
    public String getCoord() {
        return coord;
    }

    public void setCoord(@NonNull final String coord) {
        this.coord = Objects.requireNonNull(coord);
    }

    @Nullable
    public List<ReplacementTargetConfig> getTargets() {
        return targets;
    }

    public void setTargets(@NonNull final List<ReplacementTargetConfig> targets) {
        this.targets = Objects.requireNonNull(targets);
    }
}
