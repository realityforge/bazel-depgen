package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.realityforge.bazel.depgen.config.ExcludeConfig;

public final class GlobalExcludeModel {
    private final ExcludeConfig _source;

    private final String _group;

    private final String _id;

    public static GlobalExcludeModel parse(final ExcludeConfig source) {
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

    private GlobalExcludeModel(final ExcludeConfig source, final String group, final String id) {
        _source = Objects.requireNonNull(source);
        _group = Objects.requireNonNull(group);
        _id = Objects.requireNonNull(id);
    }

    public ExcludeConfig getSource() {
        return _source;
    }

    public String getGroup() {
        return _group;
    }

    public String getId() {
        return _id;
    }
}
