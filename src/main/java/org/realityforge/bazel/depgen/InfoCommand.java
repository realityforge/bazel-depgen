package org.realityforge.bazel.depgen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;

final class InfoCommand extends ConfigurableCommand {
    @NonNull
    static final String COMMAND = "info";

    @NonNull
    private final Set<String> _outputKeys = new HashSet<>();

    InfoCommand() {
        super(COMMAND, "Print runtime info about the tool.", new CLOptionDescriptor[0]);
    }

    @Override
    boolean processArguments(@NonNull final Environment environment, @NonNull final List<CLOption> arguments) {
        // Get a list of parsed options
        for (final CLOption option : arguments) {
            assert CLOption.TEXT_ARGUMENT == option.getId();
            _outputKeys.add(option.getArgument());
        }

        return true;
    }

    @Override
    int run(@NonNull final Context context) {
        final Environment environment = context.environment();
        printInfo(context, "config-file", environment::getConfigFile);
        printInfo(context, "settings-file", environment::getSettingsFile);
        printInfo(context, "cache-directory", () -> environment.hasCacheDir() ? environment.getCacheDir() : "-");
        printInfo(context, "reset-cached-metadata", environment::shouldResetCachedMetadata);
        printInfo(
                context,
                "bazel-repository-cache",
                () -> environment.hasRepositoryCacheDir() ? environment.getRepositoryCacheDir() : "-");
        return ExitCodes.SUCCESS_EXIT_CODE;
    }

    private void printInfo(
            @NonNull final Context context, @NonNull final String key, @NonNull final Supplier<Object> accessor) {
        if (_outputKeys.isEmpty() || _outputKeys.contains(key)) {
            final Logger logger = context.environment().logger();
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, key + "=" + accessor.get());
            }
        }
    }
}
