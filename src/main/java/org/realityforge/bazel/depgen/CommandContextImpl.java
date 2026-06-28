package org.realityforge.bazel.depgen;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;

final class CommandContextImpl implements Command.Context {
    @NonNull
    private final Environment _environment;

    CommandContextImpl(@NonNull final Environment environment) {
        _environment = Objects.requireNonNull(environment);
    }

    @NonNull
    @Override
    public Environment environment() {
        return _environment;
    }

    @NonNull
    @Override
    public ApplicationModel loadModel() {
        return Main.loadModel(_environment);
    }

    @NonNull
    @Override
    public ApplicationRecord loadRecord() throws Exception {
        return Main.loadRecord(_environment);
    }
}
