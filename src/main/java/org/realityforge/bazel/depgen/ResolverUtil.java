package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.realityforge.bazel.depgen.config.ApplicationConfig;

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

  @Nonnull
  static List<RemoteRepository> getRemoteRepositories( @Nonnull final ApplicationConfig config,
                                                       @Nonnull final Settings settings )
  {
    final List<RemoteRepository> repositories = new ArrayList<>();
    final Map<String, String> servers = config.getRepositories();

    for ( final Map.Entry<String, String> server : servers.entrySet() )
    {
      final RemoteRepository.Builder builder =
        new RemoteRepository.Builder( server.getKey(), "default", server.getValue() );
      final Server serverSetting = settings.getServer( server.getKey() );
      if ( null != serverSetting )
      {
        final Authentication authentication =
          new AuthenticationBuilder()
            .addUsername( serverSetting.getUsername() )
            .addPassword( serverSetting.getPassword() )
            .build();
        builder.setAuthentication( authentication );
      }
      repositories.add( builder.build() );
    }
    return repositories;
  }
}
