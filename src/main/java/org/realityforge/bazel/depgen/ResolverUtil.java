package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
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
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.RepositoryModel;

final class ResolverUtil {
    private ResolverUtil() {}

    @NonNull
    static Resolver createResolver(
            @NonNull final Environment environment,
            @NonNull final Path cacheDir,
            @NonNull final ApplicationModel model,
            @NonNull final Settings settings) {
        final var options = model.getOptions();
        final var repositories = ResolverUtil.getRemoteRepositories(model.getRepositories(), settings);
        final var defaultRepositories = ResolverUtil.getRemoteRepositories(
                model.getRepositories().stream()
                        .filter(RepositoryModel::searchByDefault)
                        .toList(),
                settings);
        return createResolver(
                environment,
                cacheDir,
                repositories,
                defaultRepositories,
                options.failOnMissingPom(),
                options.failOnInvalidPom());
    }

    @NonNull
    static Resolver createResolver(
            @NonNull final Environment environment,
            @NonNull final Path cacheDir,
            @NonNull final List<RemoteRepository> repositories,
            @NonNull final List<RemoteRepository> defaultRepositories,
            final boolean failOnMissingPom,
            final boolean failOnInvalidPom) {
        final var system = newRepositorySystem(environment);
        final var session =
                newRepositorySystemSession(system, cacheDir, environment, failOnMissingPom, failOnInvalidPom);
        return new Resolver(environment, system, session, repositories, defaultRepositories);
    }

    @NonNull
    static Resolver createResolver(
            @NonNull final Environment environment,
            @NonNull final Path cacheDir,
            @NonNull final List<RemoteRepository> repositories,
            final boolean failOnMissingPom,
            final boolean failOnInvalidPom) {
        return createResolver(environment, cacheDir, repositories, repositories, failOnMissingPom, failOnInvalidPom);
    }

    @NonNull
    private static RepositorySystem newRepositorySystem(@NonNull final Environment environment) {
        // Use the pre-populated DefaultServiceLocator rather than explicitly registering components
        final var locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(
                    @NonNull final Class<?> type, @NonNull final Class<?> impl, @NonNull final Throwable exception) {
                environment
                        .logger()
                        .log(
                                Level.SEVERE,
                                "Service creation failed for " + type + " implementation " + impl + ": "
                                        + exception.getMessage(),
                                exception);
            }
        });

        final var service = locator.getService(RepositorySystem.class);
        if (null == service) {
            throw new DepgenConfigurationException("Unable create RepositorySystem");
        }
        return service;
    }

    @NonNull
    private static RepositorySystemSession newRepositorySystemSession(
            @NonNull final RepositorySystem system,
            @NonNull final Path cacheDir,
            @NonNull final Environment environment,
            final boolean failOnMissingPom,
            final boolean failOnInvalidPom) {
        final var session = MavenRepositorySystemUtils.newSession();

        final var localRepository = new LocalRepository(cacheDir.toString());

        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));

        // Avoid using repositories set up in artifact's pom.xml
        session.setIgnoreArtifactDescriptorRepositories(true);

        session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
        session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

        session.setTransferListener(new SimpleTransferListener(environment));
        session.setRepositoryListener(new SimpleRepositoryListener(environment));
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(!failOnMissingPom, !failOnInvalidPom));

        return session;
    }

    @NonNull
    static List<RemoteRepository> getRemoteRepositories(
            @NonNull final List<RepositoryModel> repositories, @NonNull final Settings settings) {
        final var remoteRepositories = new ArrayList<RemoteRepository>();

        for (final var repository : repositories) {
            final var name = repository.getName();
            final var builder = new RemoteRepository.Builder(name, "default", repository.getUrl());
            final var server = settings.getServer(name);
            if (null != server) {
                final var authentication = new AuthenticationBuilder()
                        .addUsername(server.getUsername())
                        .addPassword(server.getPassword())
                        .build();
                builder.setAuthentication(authentication);
            }
            builder.setReleasePolicy(
                    new RepositoryPolicy(true, null, repository.checksumPolicy().name()));
            remoteRepositories.add(builder.build());
        }
        return remoteRepositories;
    }

    @NonNull
    static ArrayList<Exclusion> deriveGlobalExclusions(@NonNull final ApplicationModel model) {
        final var exclusions = new ArrayList<Exclusion>();
        for (final var exclude : model.getExcludes()) {
            exclusions.add(new Exclusion(exclude.getGroup(), exclude.getId(), "*", "*"));
        }
        return exclusions;
    }

    @NonNull
    static ArrayList<Exclusion> deriveExclusions(@NonNull final ArtifactModel artifactModel) {
        final var exclusions = new ArrayList<Exclusion>();
        for (final var exclude : artifactModel.getExcludes()) {
            final var id = exclude.getId();
            exclusions.add(new Exclusion(exclude.getGroup(), null == id ? "*" : id, "*", "*"));
        }
        return exclusions;
    }
}
