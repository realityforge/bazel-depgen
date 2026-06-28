package org.realityforge.bazel.depgen;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;

abstract class Command {
    interface Context {
        @NonNull
        Environment environment();

        @NonNull
        ApplicationModel loadModel();

        @NonNull
        ApplicationRecord loadRecord() throws Exception;
    }

    @NonNull
    private final String _name;

    @NonNull
    private final String _help;

    Command(@NonNull final String name, @NonNull final String help) {
        _name = Objects.requireNonNull(name);
        _help = Objects.requireNonNull(help);
    }

    @NonNull
    String getName() {
        return _name;
    }

    @NonNull
    String getHelp() {
        return _help;
    }

    boolean requireConfigFile() {
        return true;
    }

    boolean mayUseArtifactCache() {
        return false;
    }

    boolean mayUseRepositoryCache() {
        return false;
    }

    boolean processOptions(@NonNull final Environment environment, @NonNull final String... args) {
        if (args.length > 0) {
            environment
                    .logger()
                    .log(
                            Level.SEVERE,
                            "Error: Unknown arguments to " + _name + " command. Arguments: " + Arrays.asList(args));
            return false;
        } else {
            return true;
        }
    }

    abstract int run(@NonNull Context context) throws Exception;
}
