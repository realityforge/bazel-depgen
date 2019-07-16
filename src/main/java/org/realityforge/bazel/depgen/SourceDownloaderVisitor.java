package org.realityforge.bazel.depgen;

import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class SourceDownloaderVisitor
  extends PeerArtifactDownloaderVisitor
{
  private static final String SOURCES_PRESENT_PROPERTY = "sources.present";

  SourceDownloaderVisitor( @Nonnull final Resolver resolver, @Nonnull final ApplicationModel model )
  {
    super( resolver, model, SOURCES_PRESENT_PROPERTY, Constants.SOURCE_ARTIFACT_FILENAME );
  }

  @Override
  boolean shouldDownloadPeerArtifact( @Nonnull final Artifact artifact )
  {
    final ApplicationModel model = getModel();
    final ArtifactModel artifactModel = model.findArtifact( artifact.getGroupId(), artifact.getArtifactId() );
    final boolean includeSource = model.getOptions().includeSource();
    return null == artifactModel ? includeSource : artifactModel.includeSource( includeSource );
  }

  @Nonnull
  @Override
  SubArtifact toPeerArtifact( @Nonnull final Artifact artifact )
  {
    return new SubArtifact( artifact, "sources", "jar" );
  }
}
