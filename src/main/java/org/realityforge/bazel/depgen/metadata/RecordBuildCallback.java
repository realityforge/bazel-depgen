package org.realityforge.bazel.depgen.metadata;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface RecordBuildCallback {
    void onWarning(@NonNull String message);
}
