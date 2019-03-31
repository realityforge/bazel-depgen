package org.realityforge.bazel.depgen;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class Resolver
{
  @Nonnull
  private final Logger _logger;
  @Nonnull
  private final RepositorySystem _system;
  @Nonnull
  private final RepositorySystemSession _session;
  @Nonnull
  private final List<RemoteRepository> _repositories;

  Resolver( @Nonnull final Logger logger,
            @Nonnull final RepositorySystem system,
            @Nonnull final RepositorySystemSession session,
            @Nonnull final List<RemoteRepository> repositories )
  {
    _logger = Objects.requireNonNull( logger );
    _system = Objects.requireNonNull( system );
    _session = Objects.requireNonNull( session );
    _repositories = Objects.requireNonNull( repositories );
  }

  @Nonnull
  RepositorySystem getSystem()
  {
    return _system;
  }

  @Nonnull
  RepositorySystemSession getSession()
  {
    return _session;
  }

  /**
   * Retrieve the artifact associated with model from the remote repositories or the local cache and
   * load the associated pom to build complete artifact representation.
   */
  @Nonnull
  org.eclipse.aether.artifact.Artifact toArtifact( @Nonnull final ArtifactModel model,
                                                   @Nonnull final Consumer<List<Exception>> onInvalidPomFn )
  {
    final DefaultArtifact artifact =
      new DefaultArtifact( model.getGroup(),
                           model.getId(),
                           model.getClassifier(),
                           model.getType(),
                           model.getVersion() );
    try
    {
      final ArtifactResult artifactResult =
        _system.resolveArtifact( _session, new ArtifactRequest( artifact, _repositories, null ) );

      final ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest( artifactResult.getArtifact(), _repositories, null );

      final ArtifactDescriptorResult result = _system.readArtifactDescriptor( _session, request );
      final List<Exception> exceptions = result.getExceptions();
      if ( !exceptions.isEmpty() )
      {
        onInvalidPomFn.accept( exceptions );
      }
      return result.getArtifact();
    }
    catch ( final ArtifactResolutionException are )
    {
      final String message = are.getMessage();
      _logger.warning( null != message ? message : are.toString() );
      onInvalidPomFn.accept( Collections.singletonList( are ) );
      return artifact;
    }
    catch ( final ArtifactDescriptorException ade )
    {
      onInvalidPomFn.accept( Collections.singletonList( ade ) );
      return artifact;
    }
  }

  @FunctionalInterface
  interface OnInvalidPomFn
  {
    void onInvalidPom( @Nonnull ArtifactModel artifactModel, @Nonnull List<Exception> exceptions );
  }
}
