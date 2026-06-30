package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

final class UpdateCommand extends ConfigurableCommand {
    @NonNull
    static final String COMMAND = "update";

    private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[0];

    @Nullable
    private String _coord;

    @Nullable
    private String _version;

    UpdateCommand() {
        super(COMMAND, "Update a dependency version in the dependency configuration.", OPTIONS);
    }

    @Override
    boolean processArguments(@NonNull final Environment environment, @NonNull final List<CLOption> arguments) {
        for (final CLOption option : arguments) {
            if (CLOption.TEXT_ARGUMENT == option.getId()) {
                final String argument = option.getArgument();
                if (null == _coord) {
                    _coord = argument;
                } else if (null == _version) {
                    _version = argument;
                } else {
                    environment.logger().log(Level.SEVERE, "Error: Invalid argument: " + argument);
                    return false;
                }
            }
        }

        if (null == _coord) {
            environment.logger().log(Level.SEVERE, "Error: Missing dependency coordinate.");
            return false;
        } else if (null == _version) {
            environment.logger().log(Level.SEVERE, "Error: Missing dependency version.");
            return false;
        }
        return true;
    }

    @Override
    int run(@NonNull final Context context) throws Exception {
        final Environment environment = context.environment();
        final Path configFile = environment.getConfigFile();
        final ApplicationModel model = context.loadModel();
        final ArtifactModel requestedArtifact = parseDependencyKey(Objects.requireNonNull(_coord));
        final String version = validateVersion(Objects.requireNonNull(_version));
        final ArtifactModel matchedArtifact = findMatchedArtifact(model, requestedArtifact);

        final UpdateResult update = updateArtifact(configFile, matchedArtifact, version);
        DependencyConfigEditor.writeValidatedConfig(configFile, update.getContent());

        if (environment.logger().isLoggable(Level.INFO)) {
            environment
                    .logger()
                    .log(
                            Level.INFO,
                            "Updated dependency '" + update.getOldCoord() + "' to '" + update.getNewCoord()
                                    + "' in configuration file " + configFile);
        }
        return ExitCodes.SUCCESS_EXIT_CODE;
    }

    @NonNull
    private ArtifactModel findMatchedArtifact(
            @NonNull final ApplicationModel model, @NonNull final ArtifactModel requestedArtifact) {
        final var matches = new ArrayList<ArtifactModel>();
        for (final ArtifactModel artifact : model.getArtifacts()) {
            if (hasSameArtifactKey(requestedArtifact, artifact)) {
                matches.add(artifact);
            }
        }

        final String key = requestedArtifact.getGroup() + ":" + requestedArtifact.getId();
        if (matches.isEmpty()) {
            throw new DepgenValidationException("Dependency '" + key + "' does not exist in configuration.");
        } else if (matches.size() > 1) {
            throw new DepgenValidationException(
                    "Dependency '" + key + "' is declared multiple times in configuration.");
        } else {
            return matches.get(0);
        }
    }

    @NonNull
    private UpdateResult updateArtifact(
            @NonNull final Path configFile, @NonNull final ArtifactModel artifact, @NonNull final String version)
            throws IOException {
        final MappingNode root = DependencyConfigEditor.loadRootMapping(configFile);
        final SequenceNode artifacts =
                Objects.requireNonNull(DependencyConfigEditor.getTopLevelSequence(root, "artifacts", COMMAND));
        final String oldCoord = Objects.requireNonNull(artifact.getSource().getCoord());

        final List<Node> nodes = artifacts.getValue();
        for (final Node node : nodes) {
            if (node instanceof MappingNode mappingNode) {
                final String coord = DependencyConfigEditor.scalarMappingValue(mappingNode, "coord");
                if (oldCoord.equals(coord)) {
                    final String newCoord = replaceVersion(oldCoord, version);
                    replaceCoord(mappingNode, oldCoord, newCoord);
                    return new UpdateResult(
                            DependencyConfigEditor.serialize(root),
                            parseArtifact(oldCoord).toCoord(),
                            parseArtifact(newCoord).toCoord());
                }
            }
        }

        throw new DepgenValidationException(
                "Dependency '" + artifact.getGroup() + ":" + artifact.getId() + "' does not exist in configuration.");
    }

    private void replaceCoord(
            @NonNull final MappingNode artifact, @NonNull final String oldCoord, @NonNull final String newCoord) {
        final List<NodeTuple> entries = artifact.getValue();
        for (int i = 0; i < entries.size(); i++) {
            final NodeTuple entry = entries.get(i);
            if ("coord".equals(DependencyConfigEditor.scalarValue(entry.getKeyNode()))
                    && oldCoord.equals(DependencyConfigEditor.scalarValue(entry.getValueNode()))) {
                entries.set(
                        i,
                        DependencyConfigEditor.tuple(
                                entry.getKeyNode(),
                                createReplacementCoordNode((ScalarNode) entry.getValueNode(), newCoord)));
                return;
            }
        }
        throw new DepgenValidationException(
                "Dependency '" + parseArtifact(oldCoord).getGroup() + ":"
                        + parseArtifact(oldCoord).getId() + "' does not exist in configuration.");
    }

    @NonNull
    private Node createReplacementCoordNode(@NonNull final ScalarNode previousValue, @NonNull final String newCoord) {
        return DependencyConfigEditor.scalarNode(newCoord, previousValue.getScalarStyle());
    }

    @NonNull
    private String replaceVersion(@NonNull final String coord, @NonNull final String version) {
        final String[] components = coord.split(":", -1);
        if (2 == components.length || 3 == components.length) {
            return components[0] + ":" + components[1] + ":" + version;
        } else if (4 == components.length) {
            return components[0] + ":" + components[1] + ":" + components[2] + ":" + version;
        } else {
            return components[0] + ":" + components[1] + ":" + components[2] + ":" + components[3] + ":" + version;
        }
    }

    private boolean hasSameArtifactKey(@NonNull final ArtifactModel a, @NonNull final ArtifactModel b) {
        return a.getGroup().equals(b.getGroup()) && a.getId().equals(b.getId());
    }

    @NonNull
    private ArtifactModel parseDependencyKey(@NonNull final String coord) {
        final String[] components = coord.split(":", -1);
        if (2 != components.length || components[0].isEmpty() || components[1].isEmpty()) {
            throw new DepgenValidationException(
                    "The update command dependency coordinate must be in the form 'group:id'.");
        }
        return parseArtifact(coord);
    }

    @NonNull
    private ArtifactModel parseArtifact(@NonNull final String coord) {
        final var config = new ArtifactConfig();
        config.setCoord(coord);
        return ArtifactModel.parse(config);
    }

    @NonNull
    private String validateVersion(@NonNull final String version) {
        if (version.contains(":")) {
            throw new DepgenValidationException("Dependency version must not contain ':'.");
        }
        return version;
    }

    private static final class UpdateResult {
        @NonNull
        private final String _content;

        @NonNull
        private final String _oldCoord;

        @NonNull
        private final String _newCoord;

        UpdateResult(@NonNull final String content, @NonNull final String oldCoord, @NonNull final String newCoord) {
            _content = Objects.requireNonNull(content);
            _oldCoord = Objects.requireNonNull(oldCoord);
            _newCoord = Objects.requireNonNull(newCoord);
        }

        @NonNull
        String getContent() {
            return _content;
        }

        @NonNull
        String getOldCoord() {
            return _oldCoord;
        }

        @NonNull
        String getNewCoord() {
            return _newCoord;
        }
    }
}
