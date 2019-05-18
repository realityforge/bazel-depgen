package org.realityforge.bazel.depgen;

import java.util.HashMap;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class SourceDownloaderVisitor
  implements DependencyVisitor
{
  private Resolver _resolver;
  @Nonnull
  private final ApplicationModel _model;

  SourceDownloaderVisitor( @Nonnull final Resolver resolver, @Nonnull final ApplicationModel model )
  {
    _resolver = resolver;
    _model = Objects.requireNonNull( model );
  }

  @Override
  public boolean visitEnter( @Nonnull final DependencyNode node )
  {
    final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
    if ( null != artifact )
    {
      final ArtifactModel artifactModel = _model.findArtifact( artifact.getGroupId(), artifact.getArtifactId() );
      final boolean includeSource = _model.getOptions().includeSource();
      if ( null == artifactModel ? includeSource : artifactModel.includeSource( includeSource ) )
      {
        node.setArtifact( downloadSourcesArtifact( node ) );
      }
    }
    return true;
  }

  @Override
  public boolean visitLeave( @Nonnull final DependencyNode node )
  {
    return true;
  }

  @Nonnull
  private org.eclipse.aether.artifact.Artifact downloadSourcesArtifact( @Nonnull final DependencyNode node )
  {
    final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
    assert null != artifact;
    final SubArtifact sourcesArtifact = new SubArtifact( artifact, "sources", "jar" );
    try
    {
      final ArtifactResult sourceArtifactResult =
        _resolver.getSystem()
          .resolveArtifact( _resolver.getSession(),
                            new ArtifactRequest( sourcesArtifact, _resolver.getRepositories(), null ) );
      final HashMap<String, String> properties = new HashMap<>( artifact.getProperties() );
      properties.put( Constants.SOURCE_ARTIFACT_FILENAME,
                      sourceArtifactResult.getArtifact().getFile().getAbsolutePath() );
      return artifact.setProperties( properties );
    }
    catch ( final ArtifactResolutionException ignored )
    {
      // User has already received a warning to console and ultimately it is only a warning as most
      // builds will continue to work if source is not available.
      return artifact;
    }
  }
}
