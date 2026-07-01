package org.realityforge.bazel.depgen;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.model.ApplicationModel;

final class SourceDownloaderVisitor extends PeerArtifactDownloaderVisitor {
    private static final String SOURCES_PRESENT_PROPERTY = "sources.present";

    SourceDownloaderVisitor(@NonNull final Resolver resolver, @NonNull final ApplicationModel model) {
        super(resolver, model, SOURCES_PRESENT_PROPERTY, Constants.SOURCE_ARTIFACT_FILENAME);
    }

    @Override
    boolean shouldDownloadPeerArtifact(@NonNull final Artifact artifact) {
        final var model = getModel();
        final var artifactModel = model.findArtifact(artifact.getGroupId(), artifact.getArtifactId());
        final var includeSource = model.getOptions().includeSource();
        return null == artifactModel ? includeSource : artifactModel.includeSource(includeSource);
    }

    @NonNull
    @Override
    SubArtifact toPeerArtifact(@NonNull final Artifact artifact) {
        return new SubArtifact(artifact, "sources", "jar");
    }
}
