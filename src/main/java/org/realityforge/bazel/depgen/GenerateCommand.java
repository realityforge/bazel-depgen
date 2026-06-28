package org.realityforge.bazel.depgen;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.model.OptionsModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.util.GeneratedSectionWriter;
import org.realityforge.bazel.depgen.util.StarlarkOutput;

final class GenerateCommand extends Command {
    @Nonnull
    static final String COMMAND = "generate";

    GenerateCommand() {
        super(COMMAND, "Generate the Bazel outputs from the dependency configuration.");
    }

    @Override
    boolean mayUseArtifactCache() {
        return true;
    }

    @Override
    boolean mayUseRepositoryCache() {
        return true;
    }

    @Override
    int run(@Nonnull final Context context) throws Exception {
        final ApplicationRecord record = context.loadRecord();
        final OptionsModel options = record.getSource().getOptions();
        final Path extensionFile = options.getExtensionFile();
        final Path extensionDir = extensionFile.getParent();
        final Path extensionBuildfile = extensionDir.resolve("BUILD.bazel");
        final Path configBuildfile =
                record.getSource().getConfigLocation().getParent().resolve("BUILD.bazel");
        final Path moduleFile = options.getModuleFile();
        final boolean requiresExtensionFile = options.requiresExtensionFile();
        final boolean generatesTargetsInExtension = options.isTargetGenerationInExtensionFile();

        if (requiresExtensionFile
                && !extensionDir.toFile().exists()
                && !extensionDir.toFile().mkdirs()) {
            throw new DepgenException("Failed to create directory " + extensionDir.toFile());
        }

        if (requiresExtensionFile
                && generatesTargetsInExtension
                && extensionBuildfile.equals(configBuildfile)
                && !extensionBuildfile.toFile().exists()) {
            try (final var output = new StarlarkOutput(extensionBuildfile)) {
                record.writeDefaultExtensionBuild(output);
            }
        } else if (requiresExtensionFile
                && !extensionBuildfile.equals(configBuildfile)
                && !extensionBuildfile.toFile().exists()) {
            try (final var output = new StarlarkOutput(extensionBuildfile)) {
                record.writeDefaultExtensionBuild(output, generatesTargetsInExtension);
            }
        }

        if (generatesTargetsInExtension
                && !extensionBuildfile.equals(configBuildfile)
                && !configBuildfile.toFile().exists()) {
            try (final var output = new StarlarkOutput(configBuildfile)) {
                record.writeDefaultConfigBuild(output);
            }
        }

        if (requiresExtensionFile) {
            try (final var output = new StarlarkOutput(extensionFile)) {
                record.writeBazelExtension(output);
            }
        } else if (extensionFile.toFile().exists()
                && context.environment().logger().isLoggable(Level.WARNING)) {
            context.environment()
                    .logger()
                    .log(
                            Level.WARNING,
                            "Generated extension file '" + extensionFile
                                    + "' is no longer used and can be removed manually.");
        }

        if (!options.isRepositoryRuleGenerationInExtensionFile()) {
            GeneratedSectionWriter.replaceSection(
                    moduleFile,
                    options.getRepositoryRuleStartToken(),
                    options.getRepositoryRuleEndToken(),
                    emit(record::writeBazelModuleSection));
        }

        if (!generatesTargetsInExtension) {
            GeneratedSectionWriter.replaceSection(
                    configBuildfile,
                    options.getTargetStartToken(),
                    options.getTargetEndToken(),
                    emit(record::writeBazelBuildSection));
        }
        return ExitCodes.SUCCESS_EXIT_CODE;
    }

    @Nonnull
    private String emit(@Nonnull final Emitter emitter) throws Exception {
        final var baos = new ByteArrayOutputStream();
        try (final var output = new StarlarkOutput(baos)) {
            emitter.emit(output);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface Emitter {
        void emit(@Nonnull StarlarkOutput output) throws Exception;
    }
}
