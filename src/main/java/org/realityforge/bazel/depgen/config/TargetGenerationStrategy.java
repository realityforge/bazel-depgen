package org.realityforge.bazel.depgen.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public enum TargetGenerationStrategy {
    ExtensionFile("extensionFile"),
    Build("build");

    @NonNull
    private final String _id;

    TargetGenerationStrategy(@NonNull final String id) {
        _id = id;
    }

    @NonNull
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
