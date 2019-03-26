package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

final class ResolverUtil
{
  private ResolverUtil()
  {
  }

  @Nonnull
  static RepositorySystem newRepositorySystem( @Nonnull final Logger logger )
  {
    // Use the pre-populated DefaultServiceLocator rather than explicitly registering components
    final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
    locator.addService( TransporterFactory.class, FileTransporterFactory.class );
    locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

    locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
    {
      @Override
      public void serviceCreationFailed( @Nonnull final Class<?> type,
                                         @Nonnull final Class<?> impl,
                                         @Nonnull final Throwable exception )
      {
        logger.log( Level.SEVERE,
                    "Service creation failed for " + type + " implementation " + impl + ": " + exception.getMessage(),
                    exception );
      }
    } );

    final RepositorySystem service = locator.getService( RepositorySystem.class );
    if ( null == service )
    {
      throw new IllegalStateException( "Unable create RepositorySystem" );
    }
    return service;
  }

  @Nonnull
  static RepositorySystemSession newRepositorySystemSession( @Nonnull final RepositorySystem system,
                                                             @Nonnull final Path cacheDir,
                                                             @Nonnull final Logger logger )
  {
    final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    final LocalRepository localRepository = new LocalRepository( cacheDir.toString() );

    session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepository ) );

    // Avoid using repositories set up in artifact's pom.xml
    session.setIgnoreArtifactDescriptorRepositories( true );

    session.setTransferListener( new SimpleTransferListener( logger ) );
    session.setRepositoryListener( new SimpleRepositoryListener( logger ) );

    return session;
  }
}
