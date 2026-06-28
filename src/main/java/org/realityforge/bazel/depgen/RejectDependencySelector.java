package org.realityforge.bazel.depgen;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.jspecify.annotations.NonNull;

final class RejectDependencySelector implements DependencySelector {
    static final RejectDependencySelector INSTANCE = new RejectDependencySelector();

    private RejectDependencySelector() {}

    @Override
    public boolean selectDependency(@NonNull final Dependency dependency) {
        return false;
    }

    @Override
    public DependencySelector deriveChildSelector(@NonNull final DependencyCollectionContext context) {
        return this;
    }
}
