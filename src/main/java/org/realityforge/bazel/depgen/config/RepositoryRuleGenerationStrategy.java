package org.realityforge.bazel.depgen.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public enum RepositoryRuleGenerationStrategy {
    ExtensionFile("extensionFile"),
    Module("module");

    @NonNull
    private final String _id;

    RepositoryRuleGenerationStrategy(@NonNull final String id) {
        _id = id;
    }

    @NonNull
    public String getId() {
        return _id;
    }

    @Nullable
    public static RepositoryRuleGenerationStrategy findById(@Nullable final String id) {
        if (null != id) {
            for (final RepositoryRuleGenerationStrategy value : values()) {
                if (value.getId().equals(id)) {
                    return value;
                }
            }
        }
        return null;
    }
}
