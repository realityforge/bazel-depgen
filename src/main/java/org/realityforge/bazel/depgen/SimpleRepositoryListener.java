package org.realityforge.bazel.depgen;

import java.util.Objects;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.jspecify.annotations.NonNull;

final class SimpleRepositoryListener extends AbstractRepositoryListener {
    @NonNull
    private final Environment _environment;

    SimpleRepositoryListener(@NonNull final Environment environment) {
        _environment = Objects.requireNonNull(environment);
    }

    @Override
    public void artifactDeployed(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDeploying(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDescriptorInvalid(@NonNull final RepositoryEvent event) {
        _environment
                .logger()
                .warning("Invalid artifact descriptor for " + event.getArtifact() + ": "
                        + event.getException().getMessage());
    }

    @Override
    public void artifactDescriptorMissing(@NonNull final RepositoryEvent event) {
        _environment.logger().warning("Missing artifact descriptor for " + event.getArtifact());
    }

    @Override
    public void artifactResolved(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactDownloading(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactDownloaded(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactResolving(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Resolving artifact " + event.getArtifact());
    }

    @Override
    public void metadataDeployed(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataDeploying(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataResolved(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    @Override
    public void metadataResolving(@NonNull final RepositoryEvent event) {
        _environment.logger().fine("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
    }
}
