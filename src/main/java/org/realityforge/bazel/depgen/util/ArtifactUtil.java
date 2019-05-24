package org.realityforge.bazel.depgen.util;

import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;

public final class ArtifactUtil
{
  private ArtifactUtil()
  {
  }

  @Nonnull
  public static String artifactToPath( @Nonnull final Artifact artifact )
  {
    return artifactToPath( artifact.getGroupId(),
                           artifact.getArtifactId(),
                           artifact.getVersion(),
                           artifact.getClassifier(),
                           artifact.getExtension() );
  }

  @Nonnull
  public static String artifactToPath( @Nonnull final String groupId,
                                       @Nonnull final String artifactId,
                                       @Nonnull final String version,
                                       @Nonnull final String classifier,
                                       @Nonnull final String extension )
  {
    return groupId.replaceAll( "\\.", "/" ) +
           "/" +
           artifactId +
           "/" +
           version +
           "/" +
           artifactId +
           "-" +
           version +
           ( classifier.isEmpty() ? "" : "-" + classifier ) +
           "." + extension;
  }
}
