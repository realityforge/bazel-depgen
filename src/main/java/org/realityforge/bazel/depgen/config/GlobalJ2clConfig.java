package org.realityforge.bazel.depgen.config;

import org.jspecify.annotations.Nullable;

public final class GlobalJ2clConfig {
    @Nullable
    private JspecifyMode jspecifyMode;

    @Nullable
    public JspecifyMode getJspecifyMode() {
        return jspecifyMode;
    }

    public void setJspecifyMode(final JspecifyMode jspecifyMode) {
        this.jspecifyMode = jspecifyMode;
    }
}
