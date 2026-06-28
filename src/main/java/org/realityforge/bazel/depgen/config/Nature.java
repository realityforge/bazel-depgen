package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

public enum Nature {
    Java("-java", false),
    Plugin("-plugin", false),
    J2cl("-j2cl", true);

    @NonNull
    private final String _suffix;

    private final boolean _mandatorySuffix;

    Nature(@NonNull final String suffix, final boolean mandatorySuffix) {
        _suffix = Objects.requireNonNull(suffix);
        _mandatorySuffix = mandatorySuffix;
    }

    @NonNull
    public String suffix(final boolean multipleNatures, @NonNull final Nature defaultValue) {
        return _mandatorySuffix || (defaultValue != this && multipleNatures) ? _suffix : "";
    }
}
