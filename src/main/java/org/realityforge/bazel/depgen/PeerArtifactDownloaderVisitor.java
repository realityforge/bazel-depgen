package org.realityforge.bazel.depgen;

import java.util.HashMap;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.model.ApplicationModel;

abstract class PeerArtifactDownloaderVisitor implements DependencyVisitor {
    @NonNull
    private final Resolver _resolver;

    @NonNull
    private final ApplicationModel _model;

    @NonNull
    private final String _metadataProperty;

    @NonNull
    private final String _filenameKey;

    PeerArtifactDownloaderVisitor(
            @NonNull final Resolver resolver,
            @NonNull final ApplicationModel model,
            @NonNull final String metadataProperty,
            @NonNull final String filenameKey) {
        _resolver = Objects.requireNonNull(resolver);
        _model = Objects.requireNonNull(model);
        _metadataProperty = Objects.requireNonNull(metadataProperty);
        _filenameKey = Objects.requireNonNull(filenameKey);
    }

    @NonNull
    ApplicationModel getModel() {
        return _model;
    }

    @Override
    public final boolean visitEnter(@NonNull final DependencyNode node) {
        final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
        if (null != artifact) {
            final boolean shouldDownloadPeerArtifact = shouldDownloadPeerArtifact(artifact);
            if (shouldDownloadPeerArtifact) {
                node.setArtifact(downloadPeerArtifact(node));
            }
        }
        return true;
    }

    @Override
    public final boolean visitLeave(@NonNull final DependencyNode node) {
        return true;
    }

    private org.eclipse.aether.artifact.Artifact downloadPeerArtifact(@NonNull final DependencyNode node) {
        final var artifact = node.getArtifact();
        assert null != artifact;
        final var file = artifact.getFile();
        if (null == file) {
            // If we get here then the resolver has determined that the
            // artifact is a conflict and has not downloaded it
            return artifact;
        }
        final var metadata =
                DepgenMetadata.fromDirectory(_model, file.getParentFile().toPath());
        final var peerArtifact = toPeerArtifact(artifact);
        final var artifactModel = _model.findArtifact(artifact.getGroupId(), artifact.getArtifactId());
        final var repositories =
                null != artifactModel && !artifactModel.getRepositories().isEmpty()
                        ? _resolver.getRepositories(artifactModel)
                        : node.getRepositories();
        try {
            final var sourceArtifactResult = _resolver
                    .getSystem()
                    .resolveArtifact(_resolver.getSession(), new ArtifactRequest(peerArtifact, repositories, null));
            final var properties = new HashMap<>(artifact.getProperties());
            properties.put(
                    _filenameKey, sourceArtifactResult.getArtifact().getFile().getAbsolutePath());
            metadata.updateProperty(_metadataProperty, "true");
            return artifact.setProperties(properties);
        } catch (final ArtifactResolutionException ignored) {
            metadata.updateProperty(_metadataProperty, "false");
            // User has already received a warning to console. The tool may generate an error at a later
            // stage if in strict mode.
            return artifact;
        }
    }

    abstract boolean shouldDownloadPeerArtifact(@NonNull Artifact artifact);

    @NonNull
    abstract SubArtifact toPeerArtifact(@NonNull Artifact artifact);
}
