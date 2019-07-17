package org.realityforge.bazel.depgen;

import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class ExternalAnnotationsDownloaderVisitor
  extends PeerArtifactDownloaderVisitor
{
  ExternalAnnotationsDownloaderVisitor( @Nonnull final Resolver resolver, @Nonnull final ApplicationModel model )
  {
    super( resolver, model, "annotations.present", Constants.EXTERNAL_ANNOTATIONS_ARTIFACT_FILENAME );
  }

  @Override
  boolean shouldDownloadPeerArtifact( @Nonnull final Artifact artifact )
  {
    final ApplicationModel model = getModel();
    final ArtifactModel artifactModel = model.findArtifact( artifact.getGroupId(), artifact.getArtifactId() );
    final boolean include = model.getOptions().includeExternalAnnotations();
    return null == artifactModel ? include : artifactModel.includeExternalAnnotations( include );
  }

  @Nonnull
  @Override
  SubArtifact toPeerArtifact( @Nonnull final Artifact artifact )
  {
    return new SubArtifact( artifact, "annotations", "jar" );
  }
}
