package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

final class AddCommand extends ConfigurableCommand {
    @NonNull
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

    @NonNull
    private final List<String> _repositories = new ArrayList<>();

    @NonNull
    private final List<String> _excludes = new ArrayList<>();

    @NonNull
    private final List<String> _visibility = new ArrayList<>();

    @NonNull
    private final List<Nature> _natures = new ArrayList<>();

    @Nullable
    private String _javaName;

    private boolean _javaExportDeps;

    @Nullable
    private String _j2clName;

    @Nullable
    private J2clMode _j2clMode;

    @NonNull
    private final List<String> _j2clSuppress = new ArrayList<>();

    @Nullable
    private String _pluginName;

    @Nullable
    private Boolean _pluginGeneratesApi;

    AddCommand() {
        super(COMMAND, "Add a dependency to the dependency configuration.", OPTIONS);
    }

    @Override
    boolean processArguments(@NonNull final Environment environment, @NonNull final List<CLOption> arguments) {
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

    private boolean setIncludeSource(@NonNull final Environment environment, final boolean includeSource) {
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
            @NonNull final Environment environment,
            @NonNull final String option,
            @NonNull final String value,
            @NonNull final T[] values) {
        for (final T candidate : values) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        environment.logger().log(Level.SEVERE, "Error: Invalid value for --" + option + ": " + value);
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private Boolean parseBoolean(
            @NonNull final Environment environment, @NonNull final String option, @NonNull final String value) {
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
    int run(@NonNull final Context context) throws Exception {
        final Environment environment = context.environment();
        final Path configFile = environment.getConfigFile();
        final ApplicationModel model = context.loadModel();
        final ArtifactConfig artifactConfig = createArtifactConfig();
        final ArtifactModel artifactModel = ArtifactModel.parse(artifactConfig);

        validateNatureSpecificOptions(model);
        validateNotDuplicate(model, artifactModel);

        final String candidateContent = appendArtifact(configFile, artifactConfig);
        DependencyConfigEditor.writeValidatedConfig(configFile, candidateContent);

        if (environment.logger().isLoggable(Level.INFO)) {
            environment
                    .logger()
                    .log(
                            Level.INFO,
                            "Added dependency '" + artifactModel.toCoord() + "' to configuration file " + configFile);
        }
        return ExitCodes.SUCCESS_EXIT_CODE;
    }

    @NonNull
    private ArtifactConfig createArtifactConfig() {
        final var config = new ArtifactConfig();
        config.setCoord(Objects.requireNonNull(_coord));
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
        final var config = new JavaConfig();
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
        final var config = new J2clConfig();
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
        final var config = new PluginConfig();
        config.setName(_pluginName);
        if (null != _pluginGeneratesApi) {
            config.setGeneratesApi(_pluginGeneratesApi);
        }
        return config;
    }

    private void validateNatureSpecificOptions(@NonNull final ApplicationModel model) {
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

    private boolean includeSource(@NonNull final ApplicationModel model) {
        return null == _includeSource ? model.getOptions().includeSource() : _includeSource;
    }

    private void validateNotDuplicate(@NonNull final ApplicationModel model, @NonNull final ArtifactModel artifact) {
        if (null != model.findApplicationArtifact(artifact.getGroup(), artifact.getId())) {
            throw new DepgenValidationException("Dependency '" + artifact.getGroup() + ":" + artifact.getId()
                    + "' already exists in configuration.");
        }
    }

    @NonNull
    private String appendArtifact(@NonNull final Path configFile, @NonNull final ArtifactConfig config)
            throws IOException {
        final MappingNode root = DependencyConfigEditor.loadRootMapping(configFile);
        appendArtifact(root, createArtifactNode(config));
        return DependencyConfigEditor.serialize(root);
    }

    private void appendArtifact(@NonNull final MappingNode root, @NonNull final MappingNode artifact) {
        DependencyConfigEditor.getOrCreateTopLevelSequence(root, "artifacts", COMMAND)
                .getValue()
                .add(artifact);
    }

    @NonNull
    private MappingNode createArtifactNode(@NonNull final ArtifactConfig config) {
        final var entries = new ArrayList<NodeTuple>();
        addScalar(entries, "coord", Objects.requireNonNull(config.getCoord()));
        addScalar(entries, "nameStrategy", config.getNameStrategy());
        addScalar(entries, "repositoryNameStrategy", config.getRepositoryNameStrategy());
        addScalar(entries, "repositoryName", config.getRepositoryName());
        addScalar(entries, "includeOptional", config.getIncludeOptional());
        addScalar(entries, "includeSource", config.getIncludeSource());
        addScalar(entries, "includeExternalAnnotations", config.getIncludeExternalAnnotations());
        addSequence(entries, "repositories", config.getRepositories(), true);
        addSequence(entries, "excludes", config.getExcludes(), true);
        addSequence(entries, "visibility", config.getVisibility(), true);
        addSequence(entries, "natures", config.getNatures(), false);
        final JavaConfig java = config.getJava();
        if (null != java) {
            final var javaEntries = new ArrayList<NodeTuple>();
            addScalar(javaEntries, "name", java.getName());
            addScalar(javaEntries, "exportDeps", java.getExportDeps());
            addMapping(entries, "java", javaEntries);
        }
        final J2clConfig j2cl = config.getJ2cl();
        if (null != j2cl) {
            final var j2clEntries = new ArrayList<NodeTuple>();
            addScalar(j2clEntries, "name", j2cl.getName());
            addScalar(j2clEntries, "mode", j2cl.getMode());
            addSequence(j2clEntries, "suppress", j2cl.getSuppress(), true);
            addMapping(entries, "j2cl", j2clEntries);
        }
        final PluginConfig plugin = config.getPlugin();
        if (null != plugin) {
            final var pluginEntries = new ArrayList<NodeTuple>();
            addScalar(pluginEntries, "name", plugin.getName());
            addScalar(pluginEntries, "generatesApi", plugin.getGeneratesApi());
            addMapping(entries, "plugin", pluginEntries);
        }
        return DependencyConfigEditor.mappingNode(entries);
    }

    private void addScalar(
            @NonNull final List<NodeTuple> entries, @NonNull final String name, @Nullable final Object value) {
        if (null != value) {
            entries.add(DependencyConfigEditor.tuple(name, DependencyConfigEditor.scalarNode(value)));
        }
    }

    private void addSequence(
            @NonNull final List<NodeTuple> entries,
            @NonNull final String name,
            @Nullable final List<?> values,
            final boolean quote) {
        if (null != values && !values.isEmpty()) {
            final var nodes = new ArrayList<Node>();
            for (final Object value : values) {
                nodes.add(DependencyConfigEditor.scalarNode(
                        value, quote ? DumperOptions.ScalarStyle.DOUBLE_QUOTED : DumperOptions.ScalarStyle.PLAIN));
            }
            entries.add(
                    DependencyConfigEditor.tuple(name, new SequenceNode(Tag.SEQ, nodes, DumperOptions.FlowStyle.FLOW)));
        }
    }

    private void addMapping(
            @NonNull final List<NodeTuple> entries, @NonNull final String name, @NonNull final List<NodeTuple> values) {
        entries.add(DependencyConfigEditor.tuple(name, DependencyConfigEditor.mappingNode(values)));
    }
}
