package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PluginConfig {
    @Nullable
    private Boolean generatesApi;

    @Nullable
    private String name;

    @Nullable
    public Boolean getGeneratesApi() {
        return generatesApi;
    }

    public void setGeneratesApi(@NonNull final Boolean generatesApi) {
        this.generatesApi = Objects.requireNonNull(generatesApi);
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }
}
