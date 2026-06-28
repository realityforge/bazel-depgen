package org.realityforge.bazel.depgen.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum TargetGenerationStrategy {
    ExtensionFile("extensionFile"),
    Build("build");

    @Nonnull
    private final String _id;

    TargetGenerationStrategy(@Nonnull final String id) {
        _id = id;
    }

    @Nonnull
    public String getId() {
        return _id;
    }

    @Nullable
    public static TargetGenerationStrategy findById(@Nullable final String id) {
        if (null != id) {
            for (final TargetGenerationStrategy value : values()) {
                if (value.getId().equals(id)) {
                    return value;
                }
            }
        }
        return null;
    }
}
