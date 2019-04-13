package org.realityforge.bazel.depgen;

import javax.annotation.Nonnull;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

final class RejectDependencySelector
  implements DependencySelector
{
  static final RejectDependencySelector INSTANCE = new RejectDependencySelector();

  private RejectDependencySelector()
  {
  }

  @Override
  public boolean selectDependency( @Nonnull final Dependency dependency )
  {
    return false;
  }

  @Override
  public DependencySelector deriveChildSelector( @Nonnull final DependencyCollectionContext context )
  {
    return this;
  }
}
