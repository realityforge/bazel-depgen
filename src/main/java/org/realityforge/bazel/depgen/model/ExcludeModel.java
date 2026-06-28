package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ExcludeModel {
    @NonNull
    private final String _group;

    @Nullable
    private final String _id;

    @NonNull
    public static ExcludeModel parse(@NonNull final String value) {
        final int index = value.indexOf(':');
        final String group = -1 == index ? value : value.substring(0, index);
        final String id = -1 == index ? null : value.substring(index + 1);
        return new ExcludeModel(group, id);
    }

    public ExcludeModel(@NonNull final String group, @Nullable final String id) {
        _group = Objects.requireNonNull(group);
        _id = id;
    }

    @NonNull
    public String getGroup() {
        return _group;
    }

    public boolean hasId() {
        return null != _id;
    }

    @Nullable
    public String getId() {
        return _id;
    }
}
