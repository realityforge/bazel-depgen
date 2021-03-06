package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ExcludeModel;
import org.realityforge.bazel.depgen.model.GlobalExcludeModel;
import org.realityforge.bazel.depgen.model.OptionsModel;
import org.realityforge.bazel.depgen.model.RepositoryModel;

final class ResolverUtil
{
  private ResolverUtil()
  {
  }

  @Nonnull
  static Resolver createResolver( @Nonnull final Environment environment,
                                  @Nonnull final Path cacheDir,
                                  @Nonnull final ApplicationModel model,
                                  @Nonnull final Settings settings )
  {
    final OptionsModel options = model.getOptions();
    return createResolver( environment,
                           cacheDir,
                           ResolverUtil.getRemoteRepositories( model.getRepositories(), settings ),
                           options.failOnMissingPom(),
                           options.failOnInvalidPom() );
  }

  @Nonnull
  static Resolver createResolver( @Nonnull final Environment environment,
                                  @Nonnull final Path cacheDir,
                                  @Nonnull final List<RemoteRepository> repositories,
                                  final boolean failOnMissingPom,
                                  final boolean failOnInvalidPom )
  {
    final RepositorySystem system = newRepositorySystem( environment );
    final RepositorySystemSession session =
      newRepositorySystemSession( system, cacheDir, environment, failOnMissingPom, failOnInvalidPom );
    return new Resolver( environment, system, session, repositories );
  }

  @Nonnull
  private static RepositorySystem newRepositorySystem( @Nonnull final Environment environment )
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
        environment.logger().log( Level.SEVERE,
                                  "Service creation failed for " + type + " implementation " + impl +
                                  ": " + exception.getMessage(), exception );
      }
    } );

    final RepositorySystem service = locator.getService( RepositorySystem.class );
    if ( null == service )
    {
      throw new DepgenConfigurationException( "Unable create RepositorySystem" );
    }
    return service;
  }

  @Nonnull
  private static RepositorySystemSession newRepositorySystemSession( @Nonnull final RepositorySystem system,
                                                                     @Nonnull final Path cacheDir,
                                                                     @Nonnull final Environment environment,
                                                                     final boolean failOnMissingPom,
                                                                     final boolean failOnInvalidPom )
  {
    final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    final LocalRepository localRepository = new LocalRepository( cacheDir.toString() );

    session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepository ) );

    // Avoid using repositories set up in artifact's pom.xml
    session.setIgnoreArtifactDescriptorRepositories( true );

    session.setConfigProperty( ConflictResolver.CONFIG_PROP_VERBOSE, true );
    session.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, true );

    session.setTransferListener( new SimpleTransferListener( environment ) );
    session.setRepositoryListener( new SimpleRepositoryListener( environment ) );
    session.setArtifactDescriptorPolicy( new SimpleArtifactDescriptorPolicy( !failOnMissingPom, !failOnInvalidPom ) );

    return session;
  }

  @Nonnull
  static List<RemoteRepository> getRemoteRepositories( @Nonnull final List<RepositoryModel> repositories,
                                                       @Nonnull final Settings settings )
  {
    final List<RemoteRepository> remoteRepositories = new ArrayList<>();

    for ( final RepositoryModel repository : repositories )
    {
      final String name = repository.getName();
      final RemoteRepository.Builder builder = new RemoteRepository.Builder( name, "default", repository.getUrl() );
      final Server server = settings.getServer( name );
      if ( null != server )
      {
        final Authentication authentication =
          new AuthenticationBuilder().addUsername( server.getUsername() ).addPassword( server.getPassword() ).build();
        builder.setAuthentication( authentication );
      }
      builder.setReleasePolicy( new RepositoryPolicy( true, null, repository.checksumPolicy().name() ) );
      remoteRepositories.add( builder.build() );
    }
    return remoteRepositories;
  }

  @Nonnull
  static ArrayList<Exclusion> deriveGlobalExclusions( @Nonnull final ApplicationModel model )
  {
    final ArrayList<Exclusion> exclusions = new ArrayList<>();
    for ( final GlobalExcludeModel exclude : model.getExcludes() )
    {
      exclusions.add( new Exclusion( exclude.getGroup(), exclude.getId(), "*", "*" ) );
    }
    return exclusions;
  }

  @Nonnull
  static ArrayList<Exclusion> deriveExclusions( @Nonnull final ArtifactModel artifactModel )
  {
    final ArrayList<Exclusion> exclusions = new ArrayList<>();
    for ( final ExcludeModel exclude : artifactModel.getExcludes() )
    {
      final String id = exclude.getId();
      exclusions.add( new Exclusion( exclude.getGroup(), null == id ? "*" : id, "*", "*" ) );
    }
    return exclusions;
  }
}
