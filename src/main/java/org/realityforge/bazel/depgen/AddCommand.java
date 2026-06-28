package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.J2clConfig;
import org.realityforge.bazel.depgen.config.J2clMode;
import org.realityforge.bazel.depgen.config.JavaConfig;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.PluginConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;

final class AddCommand extends ConfigurableCommand {
    @Nonnull
    static final String COMMAND = "add";

    private static final int NATURE_OPT = 1;
    private static final int NAME_STRATEGY_OPT = 2;
    private static final int REPOSITORY_NAME_OPT = 3;
    private static final int REPOSITORY_NAME_STRATEGY_OPT = 4;
    private static final int INCLUDE_OPTIONAL_OPT = 5;
    private static final int INCLUDE_SOURCE_OPT = 6;
    private static final int NO_INCLUDE_SOURCE_OPT = 7;
    private static final int INCLUDE_EXTERNAL_ANNOTATIONS_OPT = 8;
    private static final int REPOSITORY_OPT = 9;
    private static final int EXCLUDE_OPT = 10;
    private static final int VISIBILITY_OPT = 11;
    private static final int JAVA_NAME_OPT = 12;
    private static final int JAVA_EXPORT_DEPS_OPT = 13;
    private static final int J2CL_NAME_OPT = 14;
    private static final int J2CL_MODE_OPT = 15;
    private static final int J2CL_SUPPRESS_OPT = 16;
    private static final int PLUGIN_NAME_OPT = 17;
    private static final int PLUGIN_GENERATES_API_OPT = 18;
    private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[] {
        new CLOptionDescriptor(
                "nature",
                CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                NATURE_OPT,
                "The nature of the dependency. May be specified multiple times."),
        new CLOptionDescriptor(
                "name-strategy",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                NAME_STRATEGY_OPT,
                "Override the target name strategy for this dependency."),
        new CLOptionDescriptor(
                "repository-name",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                REPOSITORY_NAME_OPT,
                "Override the generated repository base name for this dependency."),
        new CLOptionDescriptor(
                "repository-name-strategy",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                REPOSITORY_NAME_STRATEGY_OPT,
                "Override the repository name strategy for this dependency."),
        new CLOptionDescriptor(
                "include-optional",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                INCLUDE_OPTIONAL_OPT,
                "Include optional dependencies when resolving this dependency."),
        new CLOptionDescriptor(
                "include-source",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                INCLUDE_SOURCE_OPT,
                "Enable source artifact generation for this dependency."),
        new CLOptionDescriptor(
                "no-include-source",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                NO_INCLUDE_SOURCE_OPT,
                "Disable source artifact generation for this dependency."),
        new CLOptionDescriptor(
                "include-external-annotations",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                INCLUDE_EXTERNAL_ANNOTATIONS_OPT,
                "Enable external annotations for this dependency."),
        new CLOptionDescriptor(
                "repository",
                CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                REPOSITORY_OPT,
                "Restrict resolution of this dependency to a named repository."),
        new CLOptionDescriptor(
                "exclude",
                CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                EXCLUDE_OPT,
                "Exclude a transitive dependency. May be specified multiple times."),
        new CLOptionDescriptor(
                "visibility",
                CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                VISIBILITY_OPT,
                "Add a visibility label. May be specified multiple times."),
        new CLOptionDescriptor(
                "java-name",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                JAVA_NAME_OPT,
                "Override the generated Java target name."),
        new CLOptionDescriptor(
                "java-export-deps",
                CLOptionDescriptor.ARGUMENT_DISALLOWED,
                JAVA_EXPORT_DEPS_OPT,
                "Export Java dependencies from this dependency."),
        new CLOptionDescriptor(
                "j2cl-name",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                J2CL_NAME_OPT,
                "Override the generated J2CL target name."),
        new CLOptionDescriptor(
                "j2cl-mode",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                J2CL_MODE_OPT,
                "Set the J2CL mode for this dependency."),
        new CLOptionDescriptor(
                "j2cl-suppress",
                CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                J2CL_SUPPRESS_OPT,
                "Suppress a J2CL compiler check. May be specified multiple times."),
        new CLOptionDescriptor(
                "plugin-name",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                PLUGIN_NAME_OPT,
                "Override the generated Plugin target name."),
        new CLOptionDescriptor(
                "plugin-generates-api",
                CLOptionDescriptor.ARGUMENT_REQUIRED,
                PLUGIN_GENERATES_API_OPT,
                "Set whether plugin processors generate API code.")
    };

    @Nullable
    private String _coord;

    @Nullable
    private NameStrategy _nameStrategy;

    @Nullable
    private NameStrategy _repositoryNameStrategy;

    @Nullable
    private String _repositoryName;

    private boolean _includeOptional;

    @Nullable
    private Boolean _includeSource;

    private boolean _includeExternalAnnotations;

    @Nonnull
    private final List<String> _repositories = new ArrayList<>();

    @Nonnull
    private final List<String> _excludes = new ArrayList<>();

    @Nonnull
    private final List<String> _visibility = new ArrayList<>();

    @Nonnull
    private final List<Nature> _natures = new ArrayList<>();

    @Nullable
    private String _javaName;

    private boolean _javaExportDeps;

    @Nullable
    private String _j2clName;

    @Nullable
    private J2clMode _j2clMode;

    @Nonnull
    private final List<String> _j2clSuppress = new ArrayList<>();

    @Nullable
    private String _pluginName;

    @Nullable
    private Boolean _pluginGeneratesApi;

    AddCommand() {
        super(COMMAND, "Add a dependency to the dependency configuration.", OPTIONS);
    }

    @Override
    boolean processArguments(@Nonnull final Environment environment, @Nonnull final List<CLOption> arguments) {
        for (final CLOption option : arguments) {
            switch (option.getId()) {
                case CLOption.TEXT_ARGUMENT: {
                    final String argument = option.getArgument();
                    if (null == _coord) {
                        _coord = argument;
                    } else {
                        environment.logger().log(Level.SEVERE, "Error: Invalid argument: " + argument);
                        return false;
                    }
                    break;
                }
                case NATURE_OPT: {
                    final Nature nature = parseEnum(environment, "nature", option.getArgument(), Nature.values());
                    if (null == nature) {
                        return false;
                    } else if (!_natures.contains(nature)) {
                        _natures.add(nature);
                    }
                    break;
                }
                case NAME_STRATEGY_OPT: {
                    _nameStrategy =
                            parseEnum(environment, "name-strategy", option.getArgument(), NameStrategy.values());
                    if (null == _nameStrategy) {
                        return false;
                    }
                    break;
                }
                case REPOSITORY_NAME_OPT: {
                    _repositoryName = option.getArgument();
                    break;
                }
                case REPOSITORY_NAME_STRATEGY_OPT: {
                    _repositoryNameStrategy = parseEnum(
                            environment, "repository-name-strategy", option.getArgument(), NameStrategy.values());
                    if (null == _repositoryNameStrategy) {
                        return false;
                    }
                    break;
                }
                case INCLUDE_OPTIONAL_OPT: {
                    _includeOptional = true;
                    break;
                }
                case INCLUDE_SOURCE_OPT: {
                    if (!setIncludeSource(environment, true)) {
                        return false;
                    }
                    break;
                }
                case NO_INCLUDE_SOURCE_OPT: {
                    if (!setIncludeSource(environment, false)) {
                        return false;
                    }
                    break;
                }
                case INCLUDE_EXTERNAL_ANNOTATIONS_OPT: {
                    _includeExternalAnnotations = true;
                    break;
                }
                case REPOSITORY_OPT: {
                    _repositories.add(option.getArgument());
                    break;
                }
                case EXCLUDE_OPT: {
                    _excludes.add(option.getArgument());
                    break;
                }
                case VISIBILITY_OPT: {
                    _visibility.add(option.getArgument());
                    break;
                }
                case JAVA_NAME_OPT: {
                    _javaName = option.getArgument();
                    break;
                }
                case JAVA_EXPORT_DEPS_OPT: {
                    _javaExportDeps = true;
                    break;
                }
                case J2CL_NAME_OPT: {
                    _j2clName = option.getArgument();
                    break;
                }
                case J2CL_MODE_OPT: {
                    _j2clMode = parseEnum(environment, "j2cl-mode", option.getArgument(), J2clMode.values());
                    if (null == _j2clMode) {
                        return false;
                    }
                    break;
                }
                case J2CL_SUPPRESS_OPT: {
                    _j2clSuppress.add(option.getArgument());
                    break;
                }
                case PLUGIN_NAME_OPT: {
                    _pluginName = option.getArgument();
                    break;
                }
                case PLUGIN_GENERATES_API_OPT: {
                    _pluginGeneratesApi = parseBoolean(environment, "plugin-generates-api", option.getArgument());
                    if (null == _pluginGeneratesApi) {
                        return false;
                    }
                    break;
                }
            }
        }

        if (null == _coord) {
            environment.logger().log(Level.SEVERE, "Error: Missing dependency coordinate.");
            return false;
        }
        return true;
    }

    private boolean setIncludeSource(@Nonnull final Environment environment, final boolean includeSource) {
        if (null == _includeSource) {
            _includeSource = includeSource;
            return true;
        } else {
            environment
                    .logger()
                    .log(Level.SEVERE, "Error: Only one of --include-source or --no-include-source may be specified.");
            return false;
        }
    }

    @Nullable
    private <T extends Enum<T>> T parseEnum(
            @Nonnull final Environment environment,
            @Nonnull final String option,
            @Nonnull final String value,
            @Nonnull final T[] values) {
        for (final T candidate : values) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        environment.logger().log(Level.SEVERE, "Error: Invalid value for --" + option + ": " + value);
        return null;
    }

    @Nullable
    private Boolean parseBoolean(
            @Nonnull final Environment environment, @Nonnull final String option, @Nonnull final String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        } else {
            environment.logger().log(Level.SEVERE, "Error: Invalid value for --" + option + ": " + value);
            return null;
        }
    }

    @Override
    int run(@Nonnull final Context context) throws Exception {
        final Environment environment = context.environment();
        final Path configFile = environment.getConfigFile();
        final ApplicationModel model = context.loadModel();
        final ArtifactConfig artifactConfig = createArtifactConfig();
        final ArtifactModel artifactModel = ArtifactModel.parse(artifactConfig);

        validateNatureSpecificOptions(model);
        validateNotDuplicate(model, artifactModel);

        final String content = Files.readString(configFile, StandardCharsets.UTF_8);
        final String candidateContent = insertArtifact(content, renderArtifact(artifactConfig));
        writeValidatedConfig(configFile, candidateContent);

        if (environment.logger().isLoggable(Level.INFO)) {
            environment
                    .logger()
                    .log(
                            Level.INFO,
                            "Added dependency '" + artifactModel.toCoord() + "' to configuration file " + configFile);
        }
        return ExitCodes.SUCCESS_EXIT_CODE;
    }

    @Nonnull
    private ArtifactConfig createArtifactConfig() {
        final ArtifactConfig config = new ArtifactConfig();
        assert null != _coord;
        config.setCoord(_coord);
        if (null != _nameStrategy) {
            config.setNameStrategy(_nameStrategy);
        }
        if (null != _repositoryNameStrategy) {
            config.setRepositoryNameStrategy(_repositoryNameStrategy);
        }
        if (null != _repositoryName) {
            config.setRepositoryName(_repositoryName);
        }
        if (_includeOptional) {
            config.setIncludeOptional(true);
        }
        if (null != _includeSource) {
            config.setIncludeSource(_includeSource);
        }
        if (_includeExternalAnnotations) {
            config.setIncludeExternalAnnotations(true);
        }
        if (!_repositories.isEmpty()) {
            config.setRepositories(_repositories);
        }
        if (!_excludes.isEmpty()) {
            config.setExcludes(_excludes);
        }
        if (!_visibility.isEmpty()) {
            config.setVisibility(_visibility);
        }
        if (!_natures.isEmpty()) {
            config.setNatures(_natures);
        }
        final JavaConfig java = createJavaConfig();
        if (null != java) {
            config.setJava(java);
        }
        final J2clConfig j2cl = createJ2clConfig();
        if (null != j2cl) {
            config.setJ2cl(j2cl);
        }
        final PluginConfig plugin = createPluginConfig();
        if (null != plugin) {
            config.setPlugin(plugin);
        }
        return config;
    }

    @Nullable
    private JavaConfig createJavaConfig() {
        if (null == _javaName && !_javaExportDeps) {
            return null;
        }
        final JavaConfig config = new JavaConfig();
        config.setName(_javaName);
        if (_javaExportDeps) {
            config.setExportDeps(true);
        }
        return config;
    }

    @Nullable
    private J2clConfig createJ2clConfig() {
        if (null == _j2clName && null == _j2clMode && _j2clSuppress.isEmpty()) {
            return null;
        }
        final J2clConfig config = new J2clConfig();
        config.setName(_j2clName);
        if (null != _j2clMode) {
            config.setMode(_j2clMode);
        }
        if (!_j2clSuppress.isEmpty()) {
            config.setSuppress(_j2clSuppress);
        }
        return config;
    }

    @Nullable
    private PluginConfig createPluginConfig() {
        if (null == _pluginName && null == _pluginGeneratesApi) {
            return null;
        }
        final PluginConfig config = new PluginConfig();
        config.setName(_pluginName);
        if (null != _pluginGeneratesApi) {
            config.setGeneratesApi(_pluginGeneratesApi);
        }
        return config;
    }

    private void validateNatureSpecificOptions(@Nonnull final ApplicationModel model) {
        final List<Nature> natures = _natures.isEmpty()
                ? Collections.singletonList(model.getOptions().getDefaultNature())
                : _natures;
        if ((null != _javaName || _javaExportDeps) && !natures.contains(Nature.Java)) {
            throw new DepgenValidationException(
                    "The Java-specific options require the dependency to have the Java nature.");
        }
        if ((null != _j2clName || null != _j2clMode || !_j2clSuppress.isEmpty()) && !natures.contains(Nature.J2cl)) {
            throw new DepgenValidationException(
                    "The J2CL-specific options require the dependency to have the J2cl nature.");
        }
        if ((null != _pluginName || null != _pluginGeneratesApi) && !natures.contains(Nature.Plugin)) {
            throw new DepgenValidationException(
                    "The Plugin-specific options require the dependency to have the Plugin nature.");
        }
        if (natures.contains(Nature.J2cl) && !includeSource(model)) {
            throw new DepgenValidationException(
                    "Dependencies with the J2cl nature require includeSource to resolve to true.");
        }
        if (!_j2clSuppress.isEmpty() && J2clMode.Import == _j2clMode) {
            throw new DepgenValidationException("The --j2cl-suppress option is incompatible with --j2cl-mode Import.");
        }
    }

    private boolean includeSource(@Nonnull final ApplicationModel model) {
        return null == _includeSource ? model.getOptions().includeSource() : _includeSource;
    }

    private void validateNotDuplicate(@Nonnull final ApplicationModel model, @Nonnull final ArtifactModel artifact) {
        if (null != model.findApplicationArtifact(artifact.getGroup(), artifact.getId())) {
            throw new DepgenValidationException("Dependency '" + artifact.getGroup() + ":" + artifact.getId()
                    + "' already exists in configuration.");
        }
    }

    @Nonnull
    private String renderArtifact(@Nonnull final ArtifactConfig config) {
        final StringBuilder output = new StringBuilder();
        output.append("  - coord: ").append(config.getCoord()).append('\n');
        appendScalar(output, "    ", "nameStrategy", config.getNameStrategy());
        appendScalar(output, "    ", "repositoryNameStrategy", config.getRepositoryNameStrategy());
        appendScalar(output, "    ", "repositoryName", config.getRepositoryName());
        appendScalar(output, "    ", "includeOptional", config.getIncludeOptional());
        appendScalar(output, "    ", "includeSource", config.getIncludeSource());
        appendScalar(output, "    ", "includeExternalAnnotations", config.getIncludeExternalAnnotations());
        appendList(output, "    ", "repositories", config.getRepositories(), true);
        appendList(output, "    ", "excludes", config.getExcludes(), true);
        appendList(output, "    ", "visibility", config.getVisibility(), true);
        appendList(output, "    ", "natures", config.getNatures(), false);
        final JavaConfig java = config.getJava();
        if (null != java) {
            output.append("    java:\n");
            appendScalar(output, "      ", "name", java.getName());
            appendScalar(output, "      ", "exportDeps", java.getExportDeps());
        }
        final J2clConfig j2cl = config.getJ2cl();
        if (null != j2cl) {
            output.append("    j2cl:\n");
            appendScalar(output, "      ", "name", j2cl.getName());
            appendScalar(output, "      ", "mode", j2cl.getMode());
            appendList(output, "      ", "suppress", j2cl.getSuppress(), true);
        }
        final PluginConfig plugin = config.getPlugin();
        if (null != plugin) {
            output.append("    plugin:\n");
            appendScalar(output, "      ", "name", plugin.getName());
            appendScalar(output, "      ", "generatesApi", plugin.getGeneratesApi());
        }
        return output.toString();
    }

    private void appendScalar(
            @Nonnull final StringBuilder output,
            @Nonnull final String indent,
            @Nonnull final String name,
            @Nullable final Object value) {
        if (null != value) {
            output.append(indent).append(name).append(": ").append(value).append('\n');
        }
    }

    private void appendList(
            @Nonnull final StringBuilder output,
            @Nonnull final String indent,
            @Nonnull final String name,
            @Nullable final List<?> values,
            final boolean quote) {
        if (null != values && !values.isEmpty()) {
            output.append(indent).append(name).append(": [");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    output.append(", ");
                }
                final Object value = values.get(i);
                if (quote) {
                    output.append('"')
                            .append(value.toString().replace("\\", "\\\\").replace("\"", "\\\""))
                            .append('"');
                } else {
                    output.append(value);
                }
            }
            output.append("]\n");
        }
    }

    @Nonnull
    private String insertArtifact(@Nonnull final String content, @Nonnull final String artifact) {
        final String normalized = content.endsWith("\n") ? content : content + "\n";
        final String[] lines = normalized.split("\n", -1);
        final int lineCount = lines.length - 1;
        int artifactsLine = -1;
        for (int i = 0; i < lineCount; i++) {
            if (lines[i].startsWith("artifacts:")) {
                artifactsLine = i;
                validateArtifactsLineShape(lines[i]);
                break;
            }
        }
        if (-1 == artifactsLine) {
            return normalized + "artifacts:\n" + artifact;
        } else {
            int insertionIndex = lineCount;
            for (int i = artifactsLine + 1; i < lineCount; i++) {
                if (isTopLevelKey(lines[i])) {
                    insertionIndex = i;
                    while (insertionIndex > artifactsLine + 1
                            && lines[insertionIndex - 1].trim().isEmpty()) {
                        insertionIndex--;
                    }
                    break;
                }
            }
            final StringBuilder output = new StringBuilder();
            for (int i = 0; i < insertionIndex; i++) {
                output.append(lines[i]).append('\n');
            }
            output.append(artifact);
            for (int i = insertionIndex; i < lineCount; i++) {
                output.append(lines[i]).append('\n');
            }
            return output.toString();
        }
    }

    private void validateArtifactsLineShape(@Nonnull final String line) {
        String remainder = line.substring("artifacts:".length()).trim();
        final int commentIndex = remainder.indexOf('#');
        if (-1 != commentIndex) {
            remainder = remainder.substring(0, commentIndex).trim();
        }
        if (!remainder.isEmpty()) {
            throw new DepgenValidationException("The add command only supports block-style artifacts sections.");
        }
    }

    private boolean isTopLevelKey(@Nonnull final String line) {
        return !line.isEmpty()
                && !Character.isWhitespace(line.charAt(0))
                && '#' != line.charAt(0)
                && '-' != line.charAt(0)
                && line.indexOf(':') > 0;
    }

    private void writeValidatedConfig(@Nonnull final Path configFile, @Nonnull final String candidateContent)
            throws Exception {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(configFile.getParent(), ".dependencies", ".yml");
            Files.writeString(tempFile, candidateContent, StandardCharsets.UTF_8);
            ApplicationModel.load(ApplicationConfig.load(tempFile), false);
            try {
                Files.move(tempFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile = null;
        } catch (final IOException e) {
            throw new DepgenException("Failed to update configuration file " + configFile, e);
        } finally {
            if (null != tempFile) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
