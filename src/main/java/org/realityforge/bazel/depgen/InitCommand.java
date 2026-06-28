package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.realityforge.bazel.depgen.util.GeneratedSectionWriter;
import org.realityforge.bazel.depgen.util.StarlarkOutput;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;

final class InitCommand extends ConfigurableCommand {
    @NonNull
    static final String COMMAND = "init";

    private static final int NO_CREATE_WORKSPACE_OPT = 1;
    private static final int NO_GENERATE_OPT = 2;
    private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[] {
        new CLOptionDescriptor(
                "no-create-workspace",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                NO_CREATE_WORKSPACE_OPT,
                "Skip generation of WORKSPACE file even if it is not present."),
        new CLOptionDescriptor(
                "no-generate",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                NO_GENERATE_OPT,
                "Skip running generate command after initializing configuration.")
    };
    private boolean _createWorkspace = true;
    private boolean _runGenerate = true;

    InitCommand() {
        super(COMMAND, "Initialize an empty dependency configuration and Bazel scaffolding.", OPTIONS);
    }

    @Override
    boolean requireConfigFile() {
        return false;
    }

    @Override
    boolean processArguments(@NonNull final Environment environment, @NonNull final List<CLOption> arguments) {
        // Get a list of parsed options
        for (final CLOption option : arguments) {
            switch (option.getId()) {
                case CLOption.TEXT_ARGUMENT: {
                    final String argument = option.getArgument();
                    environment.logger().log(Level.SEVERE, "Error: Invalid argument: " + argument);
                    return false;
                }
                case NO_CREATE_WORKSPACE_OPT: {
                    _createWorkspace = false;
                    break;
                }
                case NO_GENERATE_OPT: {
                    _runGenerate = false;
                    break;
                }
            }
        }

        return true;
    }

    @Override
    int run(@NonNull final Context context) throws Exception {
        final Environment environment = context.environment();
        final Path configFile = environment.getConfigFile();
        final Logger logger = environment.logger();
        final Path workspaceDir = environment.currentDirectory();
        final boolean moduleMode = Files.exists(workspaceDir.resolve("MODULE.bazel"));
        if (Files.exists(configFile)) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error: Configuration file already exists. File: " + configFile);
            }
            return ExitCodes.ERROR_DEPENDENCY_CONFIG_PRESENT_CODE;
        } else {
            if (!createConfigDirectory(logger, configFile)) {
                return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
            } else if (!createConfigFile(logger, configFile, workspaceDir, moduleMode)) {
                return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
            } else if (moduleMode) {
                if (!prepareModuleModeFiles(logger, workspaceDir, configFile)) {
                    return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
                }
            } else if (_createWorkspace) {
                final Path workspaceFile = workspaceDir.resolve("WORKSPACE");
                if (!Files.exists(workspaceFile)) {
                    if (!createWorkspaceFile(logger, workspaceFile, configFile)) {
                        return ExitCodes.ERROR_INIT_WRITE_FAILED_CODE;
                    }
                }
            }

            if (_runGenerate) {
                return new GenerateCommand().run(context);
            } else {
                return ExitCodes.SUCCESS_EXIT_CODE;
            }
        }
    }

    private boolean createConfigDirectory(@NonNull final Logger logger, @NonNull final Path configFile) {
        final Path configDirectory = configFile.getParent();
        if (!Files.exists(configDirectory)) {
            try {
                Files.createDirectories(configDirectory);
            } catch (final IOException e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(
                            Level.WARNING,
                            "Error: Failed to create directory to contain configuration file. Directory: "
                                    + configDirectory);
                    logger.log(Level.WARNING, e.toString());
                }
                return false;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Created configuration directory " + configDirectory);
            }
        }
        return true;
    }

    private boolean createConfigFile(
            @NonNull final Logger logger,
            @NonNull final Path configFile,
            @NonNull final Path workspaceDir,
            final boolean moduleMode) {
        try {
            final byte[] data;
            final int count;
            try (InputStream inputStream = getClass().getResourceAsStream("templates/dependencies.yml")) {
                assert null != inputStream;
                data = new byte[inputStream.available()];
                count = inputStream.read(data);
            }
            if (data.length != count) {
                throw new IOException("Failed to ready file fully");
            }

            final Path configDirectory = Objects.requireNonNull(configFile.getParent());
            final var outputData = new String(data, StandardCharsets.UTF_8)
                    .replace(
                            "workspaceDirectory: ..",
                            "workspaceDirectory: " + configDirectory.relativize(workspaceDir));
            final String finalOutputData = moduleMode
                    ? outputData
                            .replace(
                                    "  #repositoryRuleGenerationStrategy: extensionFile",
                                    "  repositoryRuleGenerationStrategy: module")
                            .replace("  #targetGenerationStrategy: extensionFile", "  targetGenerationStrategy: build")
                    : outputData;
            Files.write(configFile, finalOutputData.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error: Failed to create configuration file. File: " + configFile);
                logger.log(Level.WARNING, e.toString());
            }
            return false;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Created configuration file " + configFile);
        }
        return true;
    }

    private boolean prepareModuleModeFiles(
            @NonNull final Logger logger, @NonNull final Path workspaceDir, @NonNull final Path configFile) {
        try {
            final Path moduleFile = workspaceDir.resolve("MODULE.bazel");
            final Path buildFile =
                    Objects.requireNonNull(configFile.getParent()).resolve("BUILD.bazel");
            final boolean moduleUpdated = GeneratedSectionWriter.ensureSectionExists(
                    moduleFile,
                    OptionsConfig.DEFAULT_REPOSITORY_RULE_START_TOKEN,
                    OptionsConfig.DEFAULT_REPOSITORY_RULE_END_TOKEN);
            if (moduleUpdated && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Updated generated section markers in " + moduleFile);
            }
            final boolean buildUpdated = GeneratedSectionWriter.ensureSectionExists(
                    buildFile, OptionsConfig.DEFAULT_TARGET_START_TOKEN, OptionsConfig.DEFAULT_TARGET_END_TOKEN);
            if (buildUpdated && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Updated generated section markers in " + buildFile);
            }
            return true;
        } catch (final IOException | DepgenException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error: Failed to prepare module-mode scaffolding.");
                logger.log(Level.WARNING, e.getMessage());
            }
            return false;
        }
    }

    private boolean createWorkspaceFile(
            @NonNull final Logger logger, @NonNull final Path workspaceFile, @NonNull final Path configFile) {
        try {
            final Path workspaceDirectory = Objects.requireNonNull(workspaceFile.getParent());
            final Path relativeConfigDirectory = Objects.requireNonNull(
                    workspaceDirectory.relativize(configFile).getParent());
            final var output = new StarlarkOutput(workspaceFile);
            output.write("workspace(name = \"" + workspaceDirectory.getFileName() + "\")");
            output.newLine();
            output.write("load(\"//" + relativeConfigDirectory
                    + ":"
                    + OptionsConfig.DEFAULT_EXTENSION_FILE
                    + "\", \""
                    + OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME
                    + "\")");
            output.newLine();
            output.write("generate_workspace_rules()");
            output.close();
        } catch (final IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error: Failed to create WORKSPACE file. File: " + workspaceFile);
                logger.log(Level.WARNING, e.toString());
            }
            return false;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Created WORKSPACE file " + workspaceFile);
        }
        return true;
    }
}
