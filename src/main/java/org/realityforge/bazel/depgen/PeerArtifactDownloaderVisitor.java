package org.realityforge.bazel.depgen;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.model.ApplicationModel;

abstract class PeerArtifactDownloaderVisitor
  implements DependencyVisitor
{
  @Nonnull
  private final Resolver _resolver;
  @Nonnull
  private final ApplicationModel _model;
  @Nonnull
  private final String _metadataProperty;
  @Nonnull
  private final String _filenameKey;

  PeerArtifactDownloaderVisitor( @Nonnull final Resolver resolver,
                                 @Nonnull final ApplicationModel model,
                                 @Nonnull final String metadataProperty,
                                 @Nonnull final String filenameKey )
  {
    _resolver = Objects.requireNonNull( resolver );
    _model = Objects.requireNonNull( model );
    _metadataProperty = Objects.requireNonNull( metadataProperty );
    _filenameKey = Objects.requireNonNull( filenameKey );
  }

  @Nonnull
  ApplicationModel getModel()
  {
    return _model;
  }

  @Override
  public final boolean visitEnter( @Nonnull final DependencyNode node )
  {
    final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
    if ( null != artifact )
    {
      final boolean shouldDownloadPeerArtifact = shouldDownloadPeerArtifact( artifact );
      if ( shouldDownloadPeerArtifact )
      {
        node.setArtifact( downloadPeerArtifact( node ) );
      }
    }
    return true;
  }

  @Override
  public final boolean visitLeave( @Nonnull final DependencyNode node )
  {
    return true;
  }

  @Nonnull
  private org.eclipse.aether.artifact.Artifact downloadPeerArtifact( @Nonnull final DependencyNode node )
  {
    final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
    assert null != artifact;
    final File file = artifact.getFile();
    if ( null == file )
    {
      // If we get here then the resolver has determined that the
      // artifact is a conflict and has not downloaded it
      return artifact;
    }
    final DepgenMetadata metadata = DepgenMetadata.fromDirectory( _model, file.getParentFile().toPath() );
    final SubArtifact peerArtifact = toPeerArtifact( artifact );
    try
    {
      final ArtifactResult sourceArtifactResult =
        _resolver.getSystem()
          .resolveArtifact( _resolver.getSession(),
                            new ArtifactRequest( peerArtifact, _resolver.getRepositories(), null ) );
      final HashMap<String, String> properties = new HashMap<>( artifact.getProperties() );
      properties.put( _filenameKey, sourceArtifactResult.getArtifact().getFile().getAbsolutePath() );
      metadata.updateProperty( _metadataProperty, "true" );
      return artifact.setProperties( properties );
    }
    catch ( final ArtifactResolutionException ignored )
    {
      metadata.updateProperty( _metadataProperty, "false" );
      // User has already received a warning to console. The tool may generate an error at a later
      // stage if in strict mode.
      return artifact;
    }
  }

  abstract boolean shouldDownloadPeerArtifact( @Nonnull Artifact artifact );

  @Nonnull
  abstract SubArtifact toPeerArtifact( @Nonnull Artifact artifact );
}
