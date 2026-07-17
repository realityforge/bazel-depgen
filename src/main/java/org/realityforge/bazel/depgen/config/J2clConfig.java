package org.realityforge.bazel.depgen.config;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class J2clConfig {
    @Nullable
    private List<String> suppress;

    @Nullable
    private J2clMode mode;

    @Nullable
    private String name;

    @Nullable
    private JspecifyMode jspecifyMode;

    @Nullable
    public List<String> getSuppress() {
        return suppress;
    }

    public void setSuppress(final List<String> suppress) {
        this.suppress = Objects.requireNonNull(suppress);
    }

    @Nullable
    public J2clMode getMode() {
        return mode;
    }

    public void setMode(final J2clMode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }

    @Nullable
    public JspecifyMode getJspecifyMode() {
        return jspecifyMode;
    }

    public void setJspecifyMode(final JspecifyMode jspecifyMode) {
        this.jspecifyMode = Objects.requireNonNull(jspecifyMode);
    }
}
