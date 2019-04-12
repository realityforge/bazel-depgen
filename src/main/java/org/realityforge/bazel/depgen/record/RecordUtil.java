package org.realityforge.bazel.depgen.record;

import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

public final class RecordUtil
{
  private RecordUtil()
  {
  }

  @Nonnull
  public static String toArtifactKey( @Nonnull final DependencyNode node )
  {
    final Artifact artifact = node.getArtifact();
    assert null != artifact;
    return artifact.getGroupId() + ":" + artifact.getArtifactId();
  }
}
