package org.realityforge.bazel.depgen;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.jspecify.annotations.NonNull;

/**
 * This selector is used to limit traversal to compile/runtime dependencies.
 */
final class CompileAndRuntimeDependencySelector implements DependencySelector {
    @Override
    public boolean selectDependency(@NonNull final Dependency dependency) {
        final String classifier = dependency.getScope();
        return "".equals(classifier) || "compile".equals(classifier) || "runtime".equals(classifier);
    }

    @Override
    public DependencySelector deriveChildSelector(@NonNull final DependencyCollectionContext context) {
        return this;
    }
}
