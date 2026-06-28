package org.realityforge.bazel.depgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class Resolver {
    @NonNull
    private final Environment _environment;

    @NonNull
    private final RepositorySystem _system;

    @NonNull
    private final RepositorySystemSession _session;

    @NonNull
    private final List<RemoteRepository> _repositories;

    @NonNull
    private final List<RemoteRepository> _defaultRepositories;

    Resolver(
            @NonNull final Environment environment,
            @NonNull final RepositorySystem system,
            @NonNull final RepositorySystemSession session,
            @NonNull final List<RemoteRepository> repositories,
            @NonNull final List<RemoteRepository> defaultRepositories) {
        _environment = Objects.requireNonNull(environment);
        _system = Objects.requireNonNull(system);
        _session = Objects.requireNonNull(session);
        _repositories = Objects.requireNonNull(repositories);
        _defaultRepositories = Objects.requireNonNull(defaultRepositories);
    }

    @NonNull
    RepositorySystem getSystem() {
        return _system;
    }

    @NonNull
    RepositorySystemSession getSession() {
        return _session;
    }

    @NonNull
    List<RemoteRepository> getRepositories() {
        return _repositories;
    }

    @NonNull
    List<RemoteRepository> getRepositories(@Nullable final ArtifactModel model) {
        if (null == model || model.getRepositories().isEmpty()) {
            return _defaultRepositories;
        } else {
            return selectRepositories(model.getRepositories());
        }
    }

    @NonNull
    List<AuthenticationContext> getAuthenticationContexts() {
        return _repositories.stream()
                .map(r -> AuthenticationContext.forRepository(_session, r))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @NonNull
    DependencyResult resolveDependencies(
            @NonNull final ApplicationModel model, @NonNull final OnInvalidPomFn onInvalidPomFn)
            throws DependencyResolutionException {
        final var session = (DefaultRepositorySystemSession) _session;
        final ArrayList<Exclusion> exclusions = ResolverUtil.deriveGlobalExclusions(model);
        session.setDependencySelector(new AndDependencySelector(
                new ExclusionDependencySelector(exclusions),
                new CompileAndRuntimeDependencySelector(),
                new OptionalDependencySelector(model)));
        session.setDependencyTraverser(new FatArtifactTraverser());
        session.setDependencyManager(new ClassicDependencyManager());
        final DependencyResult result = resolveDependencyScopes(model, onInvalidPomFn);
        result.getRoot().accept(new SourceDownloaderVisitor(this, model));
        result.getRoot().accept(new ExternalAnnotationsDownloaderVisitor(this, model));
        return result;
    }

    @NonNull
    private DependencyResult resolveDependencyScopes(
            @NonNull final ApplicationModel model, @NonNull final OnInvalidPomFn onInvalidPomFn)
            throws DependencyResolutionException {
        final var scopes = new LinkedHashMap<String, ResolutionScope>();
        model.getArtifacts().stream()
                .filter(ArtifactModel::isVersioned)
                .forEach(artifactModel -> addDependencyToScope(
                        scopes,
                        getRepositories(artifactModel),
                        toDependency(artifactModel, e -> onInvalidPomFn.onInvalidPom(artifactModel, e))));
        model.getSystemArtifacts()
                .forEach(artifactModel -> addDependencyToScope(
                        scopes,
                        _defaultRepositories,
                        toDependency(artifactModel, e -> onInvalidPomFn.onInvalidPom(artifactModel, e))));
        if (scopes.isEmpty()) {
            scopes.put(toRepositoryKey(_defaultRepositories), new ResolutionScope(_defaultRepositories));
        }

        final var results = new ArrayList<DependencyResult>();
        for (final ResolutionScope scope : scopes.values()) {
            results.add(resolveDependencies(scope._dependencies, scope._repositories));
        }
        return mergeDependencyResults(results);
    }

    private void addDependencyToScope(
            @NonNull final Map<String, ResolutionScope> scopes,
            @NonNull final List<RemoteRepository> repositories,
            @NonNull final Dependency dependency) {
        final ResolutionScope scope =
                scopes.computeIfAbsent(toRepositoryKey(repositories), key -> new ResolutionScope(repositories));
        scope._dependencies.add(dependency);
    }

    @NonNull
    private String toRepositoryKey(@NonNull final List<RemoteRepository> repositories) {
        return repositories.stream().map(RemoteRepository::getId).collect(Collectors.joining("\n"));
    }

    @NonNull
    private DependencyResult mergeDependencyResults(@NonNull final List<DependencyResult> results) {
        if (1 == results.size()) {
            return results.get(0);
        }

        final DependencyResult first = results.get(0);
        final var children = new ArrayList<DependencyNode>();
        final var cycles = new ArrayList<org.eclipse.aether.graph.DependencyCycle>();
        final var collectExceptions = new ArrayList<Exception>();
        final var artifactResults = new ArrayList<ArtifactResult>();
        for (final DependencyResult result : results) {
            children.addAll(result.getRoot().getChildren());
            cycles.addAll(result.getCycles());
            collectExceptions.addAll(result.getCollectExceptions());
            artifactResults.addAll(result.getArtifactResults());
        }

        final var root = new DefaultDependencyNode((Dependency) null);
        root.setChildren(children);
        return new DependencyResult(first.getRequest())
                .setRoot(root)
                .setCycles(cycles)
                .setCollectExceptions(collectExceptions)
                .setArtifactResults(artifactResults);
    }

    @NonNull
    private DependencyResult resolveDependencies(
            @NonNull final List<Dependency> dependencies, @NonNull final List<RemoteRepository> repositories)
            throws DependencyResolutionException {
        final var collectRequest = new CollectRequest();
        collectRequest.setDependencies(dependencies);
        collectRequest.setRepositories(repositories);
        // This filter may also need to skip artifacts with replacements.
        final DependencyFilter filter =
                (node, parents) -> !node.getData().containsKey(ConflictResolver.NODE_DATA_WINNER);
        return _system.resolveDependencies(_session, new DependencyRequest(collectRequest, filter));
    }

    @NonNull
    List<Dependency> deriveRootDependencies(
            @NonNull final ApplicationModel model, @NonNull final OnInvalidPomFn onInvalidPomFn) {
        final List<ArtifactModel> artifactModels =
                model.getArtifacts().stream().filter(ArtifactModel::isVersioned).collect(Collectors.toList());
        artifactModels.addAll(model.getSystemArtifacts());
        return deriveDependencies(artifactModels, onInvalidPomFn);
    }

    @NonNull
    private List<Dependency> deriveDependencies(
            @NonNull final List<ArtifactModel> artifactModels, @NonNull final OnInvalidPomFn onInvalidPomFn) {
        final var dependencies = new ArrayList<Dependency>();
        for (final ArtifactModel artifactModel : artifactModels) {
            dependencies.add(toDependency(artifactModel, e -> onInvalidPomFn.onInvalidPom(artifactModel, e)));
        }
        return dependencies;
    }

    @NonNull
    private Dependency toDependency(
            @NonNull final ArtifactModel artifactModel, @NonNull final Consumer<List<Exception>> onInvalidPomFn) {
        return new Dependency(
                toArtifact(artifactModel, onInvalidPomFn),
                Artifact.SCOPE_COMPILE,
                Boolean.FALSE,
                ResolverUtil.deriveExclusions(artifactModel));
    }

    /**
     * Retrieve the artifact associated with model from the remote repositories or the local cache and
     * load the associated pom to build complete artifact representation.
     */
    org.eclipse.aether.artifact.Artifact toArtifact(
            @NonNull final ArtifactModel model, @NonNull final Consumer<List<Exception>> onInvalidPomFn) {
        final var artifact = new DefaultArtifact(
                model.getGroup(), model.getId(), model.getClassifier(), model.getType(), model.getVersion());
        try {
            final List<RemoteRepository> remoteRepositories = getRepositories(model);
            final ArtifactResult artifactResult =
                    _system.resolveArtifact(_session, new ArtifactRequest(artifact, remoteRepositories, null));

            final var request = new ArtifactDescriptorRequest(artifactResult.getArtifact(), remoteRepositories, null);

            final ArtifactDescriptorResult result = _system.readArtifactDescriptor(_session, request);
            final List<Exception> exceptions = result.getExceptions();
            if (!exceptions.isEmpty()) {
                onInvalidPomFn.accept(exceptions);
            }

            return result.getArtifact();
        } catch (final ArtifactResolutionException are) {
            final String message = are.getMessage();
            _environment.logger().warning(null != message ? message : are.toString());
            onInvalidPomFn.accept(Collections.singletonList(are));
            return artifact;
        } catch (final ArtifactDescriptorException ade) {
            onInvalidPomFn.accept(Collections.singletonList(ade));
            return artifact;
        }
    }

    @FunctionalInterface
    interface OnInvalidPomFn {
        void onInvalidPom(@NonNull ArtifactModel artifactModel, @NonNull List<Exception> exceptions);
    }

    @NonNull
    private List<RemoteRepository> selectRepositories(@NonNull final List<String> repositoryIds) {
        final var repositories = new ArrayList<RemoteRepository>();
        for (final RemoteRepository repository : _repositories) {
            if (repositoryIds.contains(repository.getId())) {
                repositories.add(repository);
            }
        }
        return repositories;
    }

    private static final class ResolutionScope {
        @NonNull
        private final List<RemoteRepository> _repositories;

        @NonNull
        private final List<Dependency> _dependencies = new ArrayList<>();

        private ResolutionScope(@NonNull final List<RemoteRepository> repositories) {
            _repositories = Objects.requireNonNull(repositories);
        }
    }
}
