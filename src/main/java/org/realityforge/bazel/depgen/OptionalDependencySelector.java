package org.realityforge.bazel.depgen;

import java.util.Objects;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.model.ApplicationModel;

/**
 * This selector is used to exclude optional dependencies unless includeOptional is configured for artifact.
 */
final class OptionalDependencySelector implements DependencySelector {
    @NonNull
    private final ApplicationModel _model;

    OptionalDependencySelector(@NonNull final ApplicationModel model) {
        _model = Objects.requireNonNull(model);
    }

    @Override
    public boolean selectDependency(@NonNull final Dependency dependency) {
        return true;
    }

    @Override
    public DependencySelector deriveChildSelector(@NonNull final DependencyCollectionContext context) {
        final Dependency dependency = context.getDependency();
        if (null == dependency) {
            return this;
        } else {
            final var artifact = dependency.getArtifact();
            final var groupId = artifact.getGroupId();
            final var artifactId = artifact.getArtifactId();
            final var model = _model.findArtifact(groupId, artifactId);
            if (!dependency.isOptional() || (null != model && model.includeOptional())) {
                return this;
            } else {
                return RejectDependencySelector.INSTANCE;
            }
        }
    }
}
