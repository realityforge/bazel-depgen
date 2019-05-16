package org.realityforge.bazel.depgen;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

/**
 * This selector is used to exclude optional dependencies unless includeOptional is configured for artifact.
 */
final class OptionalDependencySelector
  implements DependencySelector
{
  @Nonnull
  private final ApplicationModel _model;

  OptionalDependencySelector( @Nonnull final ApplicationModel model )
  {
    _model = Objects.requireNonNull( model );
  }

  @Override
  public boolean selectDependency( @Nonnull final Dependency dependency )
  {
    return true;
  }

  @Override
  public DependencySelector deriveChildSelector( @Nonnull final DependencyCollectionContext context )
  {
    final Dependency dependency = context.getDependency();
    if ( null == dependency )
    {
      return this;
    }
    else
    {
      final Artifact artifact = dependency.getArtifact();
      final String groupId = artifact.getGroupId();
      final String artifactId = artifact.getArtifactId();
      final ArtifactModel model = _model.findArtifact( groupId, artifactId );
      if ( !dependency.isOptional() || ( null != model && model.includeOptional() ) )
      {
        return this;
      }
      else
      {
        return RejectDependencySelector.INSTANCE;
      }
    }
  }
}
