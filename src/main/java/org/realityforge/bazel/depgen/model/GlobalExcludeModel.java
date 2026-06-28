package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.config.ExcludeConfig;

public final class GlobalExcludeModel {
    @NonNull
    private final ExcludeConfig _source;

    @NonNull
    private final String _group;

    @NonNull
    private final String _id;

    @NonNull
    public static GlobalExcludeModel parse(@NonNull final ExcludeConfig source) {
        final String coord = source.getCoord();
        if (null == coord) {
            throw new InvalidModelException("The global exclude must specify the 'coord' property.", source);
        } else {
            final String[] components = coord.split(":");
            if (components.length != 2) {
                throw new InvalidModelException(
                        "The 'coord' property on the dependency must specify 2 components "
                                + "separated by the ':' character. The 'coords' must be in the form; "
                                + "'group:id'.",
                        source);
            } else {
                return new GlobalExcludeModel(source, components[0], components[1]);
            }
        }
    }

    private GlobalExcludeModel(
            @NonNull final ExcludeConfig source, @NonNull final String group, @NonNull final String id) {
        _source = Objects.requireNonNull(source);
        _group = Objects.requireNonNull(group);
        _id = Objects.requireNonNull(id);
    }

    @NonNull
    public ExcludeConfig getSource() {
        return _source;
    }

    @NonNull
    public String getGroup() {
        return _group;
    }

    @NonNull
    public String getId() {
        return _id;
    }
}
