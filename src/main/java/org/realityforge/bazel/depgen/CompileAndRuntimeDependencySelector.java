package org.realityforge.bazel.depgen;

import javax.annotation.Nonnull;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * This selector is used to limit traversal to compile/runtime dependencies.
 */
final class CompileAndRuntimeDependencySelector
  implements DependencySelector
{
  @Override
  public boolean selectDependency( @Nonnull final Dependency dependency )
  {
    final String classifier = dependency.getScope();
    return "".equals( classifier ) || "compile".equals( classifier ) || "runtime".equals( classifier );
  }

  @Override
  public DependencySelector deriveChildSelector( @Nonnull final DependencyCollectionContext context )
  {
    return this;
  }
}
