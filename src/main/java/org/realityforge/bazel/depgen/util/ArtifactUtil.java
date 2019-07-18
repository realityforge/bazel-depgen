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
    return artifactToDirectory( groupId, artifactId, version ) +
           "/" +
           artifactToLocalFilename( artifactId, version, classifier, extension );
  }

  @Nonnull
  public static String artifactToDirectory( @Nonnull final String groupId,
                                            @Nonnull final String artifactId,
                                            @Nonnull final String version )
  {
    return groupId.replaceAll( "\\.", "/" ) + "/" + artifactId + "/" + version;
  }

  @Nonnull
  public static String artifactToLocalFilename( @Nonnull final Artifact artifact )
  {
    return artifactToLocalFilename( artifact.getArtifactId(),
                                    artifact.getVersion(),
                                    artifact.getClassifier(),
                                    artifact.getExtension() );
  }

  @Nonnull
  private static String artifactToLocalFilename( @Nonnull final String artifactId,
                                                 @Nonnull final String version,
                                                 @Nonnull final String classifier,
                                                 @Nonnull final String extension )
  {
    return artifactId + "-" + version + ( classifier.isEmpty() ? "" : "-" + classifier ) + "." + extension;
  }
}
