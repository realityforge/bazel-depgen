package org.realityforge.bazel.depgen.metadata;

@FunctionalInterface
public interface RecordBuildCallback {
    void onWarning(String message);
}
