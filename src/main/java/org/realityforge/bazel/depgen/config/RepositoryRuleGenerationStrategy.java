package org.realityforge.bazel.depgen.config;

import org.jspecify.annotations.Nullable;

public enum RepositoryRuleGenerationStrategy {
    ExtensionFile("extensionFile"),
    Module("module");

    private final String _id;

    RepositoryRuleGenerationStrategy(final String id) {
        _id = id;
    }

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
