package org.realityforge.bazel.depgen.config;

import java.util.Objects;

public enum Nature {
    Java("-java", false),
    Plugin("-plugin", false),
    J2cl("-j2cl", true);

    private final String _suffix;

    private final boolean _mandatorySuffix;

    Nature(final String suffix, final boolean mandatorySuffix) {
        _suffix = Objects.requireNonNull(suffix);
        _mandatorySuffix = mandatorySuffix;
    }

    public String suffix(final boolean multipleNatures, final Nature defaultValue) {
        return _mandatorySuffix || (defaultValue != this && multipleNatures) ? _suffix : "";
    }
}
