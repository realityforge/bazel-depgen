package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

final class DependencyConfigEditor {
    private DependencyConfigEditor() {}

    @NonNull
    static MappingNode loadRootMapping(@NonNull final Path configFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            return toRootMapping(createCommentAwareYaml().compose(reader));
        }
    }

    @NonNull
    static String serialize(@NonNull final MappingNode root) {
        final var writer = new StringWriter();
        createCommentAwareYaml().serialize(root, writer);
        return writer.toString();
    }

    @NonNull
    static SequenceNode getOrCreateTopLevelSequence(
            @NonNull final MappingNode root, @NonNull final String name, @NonNull final String command) {
        final SequenceNode sequence = getTopLevelSequence(root, name, command);
        if (null != sequence) {
            return sequence;
        }

        final SequenceNode newSequence = sequenceNode(Collections.emptyList());
        root.getValue().add(tuple(name, newSequence));
        return newSequence;
    }

    @Nullable
    static SequenceNode getTopLevelSequence(
            @NonNull final MappingNode root, @NonNull final String name, @NonNull final String command) {
        for (final NodeTuple entry : root.getValue()) {
            if (name.equals(scalarValue(entry.getKeyNode()))) {
                final Node value = entry.getValueNode();
                if (value instanceof SequenceNode sequence) {
                    sequence.setFlowStyle(DumperOptions.FlowStyle.BLOCK);
                    return sequence;
                } else {
                    throw new DepgenValidationException(
                            "The " + command + " command only supports sequence-style " + name + ".");
                }
            }
        }
        return null;
    }

    @Nullable
    static String scalarMappingValue(@NonNull final MappingNode mapping, @NonNull final String key) {
        for (final NodeTuple entry : mapping.getValue()) {
            if (key.equals(scalarValue(entry.getKeyNode()))) {
                return scalarValue(entry.getValueNode());
            }
        }
        return null;
    }

    @Nullable
    static String scalarValue(@NonNull final Node node) {
        return node instanceof ScalarNode scalarNode ? scalarNode.getValue() : null;
    }

    @NonNull
    static MappingNode mappingNode(@NonNull final List<NodeTuple> entries) {
        return new MappingNode(Tag.MAP, entries, DumperOptions.FlowStyle.BLOCK);
    }

    @NonNull
    static SequenceNode sequenceNode(@NonNull final List<Node> entries) {
        return new SequenceNode(Tag.SEQ, new ArrayList<>(entries), DumperOptions.FlowStyle.BLOCK);
    }

    @NonNull
    static NodeTuple tuple(@NonNull final String key, @NonNull final Node value) {
        return tuple(scalarNode(key), value);
    }

    @NonNull
    static NodeTuple tuple(@NonNull final Node key, @NonNull final Node value) {
        return new NodeTuple(key, value);
    }

    @NonNull
    static ScalarNode scalarNode(@NonNull final Object value) {
        return scalarNode(value, DumperOptions.ScalarStyle.PLAIN);
    }

    @NonNull
    static ScalarNode scalarNode(@NonNull final Object value, final DumperOptions.ScalarStyle style) {
        final Tag tag = value instanceof Boolean ? Tag.BOOL : Tag.STR;
        return new ScalarNode(tag, value.toString(), null, null, style);
    }

    static void writeValidatedConfig(@NonNull final Path configFile, @NonNull final String candidateContent)
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

    @NonNull
    private static Yaml createCommentAwareYaml() {
        final var loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        final var dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setIndentWithIndicator(true);
        return new Yaml(loaderOptions, dumperOptions);
    }

    @NonNull
    private static MappingNode toRootMapping(@Nullable final Node node) {
        if (null == node) {
            return mappingNode(new ArrayList<>());
        } else if (node instanceof MappingNode mappingNode) {
            mappingNode.setTag(Tag.MAP);
            mappingNode.setFlowStyle(DumperOptions.FlowStyle.BLOCK);
            mappingNode.setValue(new ArrayList<>(mappingNode.getValue()));
            return mappingNode;
        } else {
            throw new DepgenValidationException("The dependency configuration must be a YAML mapping.");
        }
    }
}
