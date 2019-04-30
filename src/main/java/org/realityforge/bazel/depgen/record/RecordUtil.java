package org.realityforge.bazel.depgen.record;

import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class RecordUtil
{
  private RecordUtil()
  {
  }

  @Nonnull
  static String toArtifactKey( @Nonnull final ArtifactModel model )
  {
    return model.getGroup() + ":" + model.getId();
  }

  @Nonnull
  static String toArtifactKey( @Nonnull final DependencyNode node )
  {
    final Artifact artifact = node.getArtifact();
    assert null != artifact;
    return artifact.getGroupId() + ":" + artifact.getArtifactId();
  }

  @Nonnull
  static String artifactToPath( @Nonnull final Artifact artifact )
  {
    return artifact.getGroupId().replaceAll( "\\.", "/" ) +
           "/" +
           artifact.getArtifactId() +
           "/" +
           artifact.getVersion() +
           "/" +
           artifact.getArtifactId() +
           "-" +
           artifact.getVersion() +
           ( artifact.getClassifier().isEmpty() ? "" : "-" + artifact.getClassifier() ) +
           "." +
           artifact.getExtension();
  }
}
