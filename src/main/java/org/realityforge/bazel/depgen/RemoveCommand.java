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

final class RemoveCommand extends ConfigurableCommand {
    @NonNull
    static final String COMMAND = "remove";

    private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[0];

    @Nullable
    private String _coord;

    RemoveCommand() {
        super(COMMAND, "Remove a dependency from the dependency configuration.", OPTIONS);
    }

    @Override
    boolean processArguments(@NonNull final Environment environment, @NonNull final List<CLOption> arguments) {
        for (final var option : arguments) {
            if (CLOption.TEXT_ARGUMENT == option.getId()) {
                final var argument = option.getArgument();
                if (null == _coord) {
                    _coord = argument;
                } else {
                    environment.logger().log(Level.SEVERE, "Error: Invalid argument: " + argument);
                    return false;
                }
            }
        }

        if (null == _coord) {
            environment.logger().log(Level.SEVERE, "Error: Missing dependency coordinate.");
            return false;
        }
        return true;
    }

    @Override
    int run(@NonNull final Context context) throws Exception {
        final var environment = context.environment();
        final var configFile = environment.getConfigFile();
        final var model = context.loadModel();
        final var requestedArtifact = parseArtifact(Objects.requireNonNull(_coord));
        final var matchedArtifact = findMatchedArtifact(model, requestedArtifact);

        final var candidateContent = removeArtifact(configFile, matchedArtifact);
        DependencyConfigEditor.writeValidatedConfig(configFile, candidateContent);

        if (environment.logger().isLoggable(Level.INFO)) {
            environment
                    .logger()
                    .log(
                            Level.INFO,
                            "Removed dependency '" + matchedArtifact.toCoord() + "' from configuration file "
                                    + configFile);
        }
        return ExitCodes.SUCCESS_EXIT_CODE;
    }

    @NonNull
    private ArtifactModel findMatchedArtifact(
            @NonNull final ApplicationModel model, @NonNull final ArtifactModel requestedArtifact) {
        final var matches = new ArrayList<ArtifactModel>();
        for (final var artifact : model.getArtifacts()) {
            if (hasSameArtifactKey(requestedArtifact, artifact)) {
                matches.add(artifact);
            }
        }

        final var key = requestedArtifact.getGroup() + ":" + requestedArtifact.getId();
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
    private String removeArtifact(@NonNull final Path configFile, @NonNull final ArtifactModel artifact)
            throws IOException {
        final var root = DependencyConfigEditor.loadRootMapping(configFile);
        final var artifacts = DependencyConfigEditor.getTopLevelSequence(root, "artifacts", COMMAND);
        if (null == artifacts) {
            throw new DepgenValidationException("Dependency '" + artifact.getGroup() + ":" + artifact.getId()
                    + "' does not exist in configuration.");
        }

        var matchIndex = -1;
        final var nodes = artifacts.getValue();
        for (var i = 0; i < nodes.size(); i++) {
            final var node = nodes.get(i);
            if (node instanceof MappingNode mappingNode) {
                final var coord = DependencyConfigEditor.scalarMappingValue(mappingNode, "coord");
                if (null != coord && hasSameArtifactKey(artifact, parseArtifact(coord))) {
                    if (-1 != matchIndex) {
                        throw new DepgenValidationException("Dependency '" + artifact.getGroup() + ":"
                                + artifact.getId() + "' is declared multiple times in configuration.");
                    }
                    matchIndex = i;
                }
            }
        }

        if (-1 == matchIndex) {
            throw new DepgenValidationException("Dependency '" + artifact.getGroup() + ":" + artifact.getId()
                    + "' does not exist in configuration.");
        }
        nodes.remove(matchIndex);
        return DependencyConfigEditor.serialize(root);
    }

    private boolean hasSameArtifactKey(@NonNull final ArtifactModel a, @NonNull final ArtifactModel b) {
        return a.getGroup().equals(b.getGroup()) && a.getId().equals(b.getId());
    }

    @NonNull
    private ArtifactModel parseArtifact(@NonNull final String coord) {
        final var config = new ArtifactConfig();
        config.setCoord(coord);
        return ArtifactModel.parse(config);
    }
}
