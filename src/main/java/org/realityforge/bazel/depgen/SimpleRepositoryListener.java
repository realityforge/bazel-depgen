package org.realityforge.bazel.depgen;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

final class SimpleRepositoryListener
  extends AbstractRepositoryListener
{
  @Nonnull
  private final Environment _environment;

  SimpleRepositoryListener( @Nonnull final Environment environment )
  {
    _environment = Objects.requireNonNull( environment );
  }

  @Override
  public void artifactDeployed( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Deployed " + event.getArtifact() + " to " + event.getRepository() );
  }

  @Override
  public void artifactDeploying( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Deploying " + event.getArtifact() + " to " + event.getRepository() );
  }

  @Override
  public void artifactDescriptorInvalid( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().warning( "Invalid artifact descriptor for " + event.getArtifact() + ": " +
                                   event.getException().getMessage() );
  }

  @Override
  public void artifactDescriptorMissing( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().warning( "Missing artifact descriptor for " + event.getArtifact() );
  }

  @Override
  public void artifactResolved( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Resolved artifact " + event.getArtifact() + " from " + event.getRepository() );
  }

  @Override
  public void artifactDownloading( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Downloading artifact " + event.getArtifact() + " from " + event.getRepository() );
  }

  @Override
  public void artifactDownloaded( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Downloaded artifact " + event.getArtifact() + " from " + event.getRepository() );
  }

  @Override
  public void artifactResolving( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Resolving artifact " + event.getArtifact() );
  }

  @Override
  public void metadataDeployed( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Deployed " + event.getMetadata() + " to " + event.getRepository() );
  }

  @Override
  public void metadataDeploying( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Deploying " + event.getMetadata() + " to " + event.getRepository() );
  }

  @Override
  public void metadataResolved( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Resolved metadata " + event.getMetadata() + " from " + event.getRepository() );
  }

  @Override
  public void metadataResolving( @Nonnull final RepositoryEvent event )
  {
    _environment.logger().fine( "Resolving metadata " + event.getMetadata() + " from " + event.getRepository() );
  }
}
