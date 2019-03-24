package org.realityforge.bazel.depgen;

import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

final class SimpleRepositoryListener
  extends AbstractRepositoryListener
{
  @Nonnull
  private final Logger _logger;

  SimpleRepositoryListener( @Nonnull final Logger logger )
  {
    _logger = Objects.requireNonNull( logger );
  }

  @Override
  public void artifactDeployed( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Deployed " + event.getArtifact() + " to " + event.getRepository() );
  }

  @Override
  public void artifactDeploying( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Deploying " + event.getArtifact() + " to " + event.getRepository() );
  }

  @Override
  public void artifactDescriptorInvalid( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage() );
  }

  @Override
  public void artifactDescriptorMissing( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Missing artifact descriptor for " + event.getArtifact() );
  }

  @Override
  public void artifactInstalled( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Installed " + event.getArtifact() + " to " + event.getFile() );
  }

  @Override
  public void artifactInstalling( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Installing " + event.getArtifact() + " to " + event.getFile() );
  }

  @Override
  public void artifactResolved( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Resolved artifact " + event.getArtifact() + " from " + event.getRepository() );
  }

  @Override
  public void artifactDownloading( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Downloading artifact " + event.getArtifact() + " from " + event.getRepository() );
  }

  @Override
  public void artifactDownloaded( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Downloaded artifact " + event.getArtifact() + " from " + event.getRepository() );
  }

  @Override
  public void artifactResolving( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Resolving artifact " + event.getArtifact() );
  }

  @Override
  public void metadataDeployed( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Deployed " + event.getMetadata() + " to " + event.getRepository() );
  }

  @Override
  public void metadataDeploying( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Deploying " + event.getMetadata() + " to " + event.getRepository() );
  }

  @Override
  public void metadataInstalled( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Installed " + event.getMetadata() + " to " + event.getFile() );
  }

  @Override
  public void metadataInstalling( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Installing " + event.getMetadata() + " to " + event.getFile() );
  }

  @Override
  public void metadataInvalid( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Invalid metadata " + event.getMetadata() );
  }

  @Override
  public void metadataResolved( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Resolved metadata " + event.getMetadata() + " from " + event.getRepository() );
  }

  @Override
  public void metadataResolving( @Nonnull final RepositoryEvent event )
  {
    _logger.info( "Resolving metadata " + event.getMetadata() + " from " + event.getRepository() );
  }
}
