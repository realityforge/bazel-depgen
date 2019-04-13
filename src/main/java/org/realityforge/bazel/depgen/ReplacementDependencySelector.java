package org.realityforge.bazel.depgen;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.realityforge.bazel.depgen.model.ApplicationModel;

/**
 * This selector is used to traverse to a replacement dependency but avoid
 * walking any child dependencies of replacement. This ultimately means that
 * the dependency node appears in the output but children of replacement node
 * do not influence the shape of the graph (by adding dependency that may conflict
 * eith another dependency).
 */
final class ReplacementDependencySelector
  implements DependencySelector
{
  @Nonnull
  private final ApplicationModel _model;

  ReplacementDependencySelector( @Nonnull final ApplicationModel model )
  {
    _model = Objects.requireNonNull( model );
  }

  @Override
  public boolean selectDependency( @Nonnull final Dependency dependency )
  {
    final String classifier = dependency.getScope();
    return "".equals( classifier ) || "compile".equals( classifier ) || "runtime".equals( classifier );
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
      if ( null != _model.findReplacement( groupId, artifactId ) )
      {
        return RejectDependencySelector.INSTANCE;
      }
      else
      {
        return this;
      }
    }
  }
}
