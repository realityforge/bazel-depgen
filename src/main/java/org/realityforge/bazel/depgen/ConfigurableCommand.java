package org.realityforge.bazel.depgen;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;

abstract class ConfigurableCommand extends Command {
    private static final int HELP_OPT = 'h';
    private static final CLOptionDescriptor HELP_DESCRIPTOR = new CLOptionDescriptor(
            "help", CLOptionDescriptor.ARGUMENT_DISALLOWED, HELP_OPT, "print this message and exit");

    @NonNull
    private final CLOptionDescriptor[] _options;

    ConfigurableCommand(
            @NonNull final String name,
            @NonNull final String help,
            @NonNull final CLOptionDescriptor @NonNull [] options) {
        super(name, help);
        _options = new CLOptionDescriptor[options.length + 1];
        _options[0] = HELP_DESCRIPTOR;
        System.arraycopy(options, 0, _options, 1, options.length);
    }

    @Override
    final boolean processOptions(@NonNull final Environment environment, @NonNull final String... args) {
        // Parse the arguments
        final var parser = new CLArgsParser(args, _options);

        // Make sure that there was no errors parsing arguments
        final var logger = environment.logger();
        if (null != parser.getErrorString()) {
            logger.log(Level.SEVERE, "Error: " + parser.getErrorString());
            return false;
        }
        // Get a list of parsed options
        final var arguments = parser.getArguments();
        final var argumentsToProcess = new ArrayList<>(arguments);
        for (final var option : arguments) {
            if (HELP_OPT == option.getId()) {
                argumentsToProcess.remove(option);
                printUsage(environment);
                return false;
            }
        }
        return processArguments(environment, argumentsToProcess);
    }

    abstract boolean processArguments(@NonNull Environment environment, @NonNull List<CLOption> arguments);

    /**
     * Print out a usage statement
     */
    private void printUsage(@NonNull final Environment environment) {
        final var logger = environment.logger();
        logger.info(getName() + " Options:");
        final var options = CLUtil.describeOptions(_options).toString().split(System.getProperty("line.separator"));
        for (final var line : options) {
            logger.info(line);
        }
    }
}
