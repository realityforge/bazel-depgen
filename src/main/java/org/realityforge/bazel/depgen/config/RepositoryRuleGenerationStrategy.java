package org.realityforge.bazel.depgen.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum RepositoryRuleGenerationStrategy {
    ExtensionFile("extensionFile"),
    Module("module");

    @Nonnull
    private final String _id;

    RepositoryRuleGenerationStrategy(@Nonnull final String id) {
        _id = id;
    }

    @Nonnull
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
