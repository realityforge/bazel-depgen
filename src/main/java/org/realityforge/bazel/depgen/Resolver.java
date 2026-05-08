package org.realityforge.bazel.depgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DefaultDependencyNode;
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
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

final class Resolver
{
  @Nonnull
  private final Environment _environment;
  @Nonnull
  private final RepositorySystem _system;
  @Nonnull
  private final RepositorySystemSession _session;
  @Nonnull
  private final List<RemoteRepository> _repositories;
  @Nonnull
  private final List<RemoteRepository> _defaultRepositories;

  Resolver( @Nonnull final Environment environment,
            @Nonnull final RepositorySystem system,
            @Nonnull final RepositorySystemSession session,
            @Nonnull final List<RemoteRepository> repositories,
            @Nonnull final List<RemoteRepository> defaultRepositories )
  {
    _environment = Objects.requireNonNull( environment );
    _system = Objects.requireNonNull( system );
    _session = Objects.requireNonNull( session );
    _repositories = Objects.requireNonNull( repositories );
    _defaultRepositories = Objects.requireNonNull( defaultRepositories );
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

  @Nonnull
  List<RemoteRepository> getRepositories()
  {
    return _repositories;
  }

  @Nonnull
  List<RemoteRepository> getRepositories( @Nullable final ArtifactModel model )
  {
    if ( null == model || model.getRepositories().isEmpty() )
    {
      return _defaultRepositories;
    }
    else
    {
      return selectRepositories( model.getRepositories() );
    }
  }

  @Nonnull
  List<AuthenticationContext> getAuthenticationContexts()
  {
    return _repositories.stream()
      .map( r -> AuthenticationContext.forRepository( _session, r ) )
      .filter( Objects::nonNull )
      .collect( Collectors.toList() );
  }

  @Nonnull
  DependencyResult resolveDependencies( @Nonnull final ApplicationModel model,
                                        @Nonnull final OnInvalidPomFn onInvalidPomFn )
    throws DependencyResolutionException
  {
    final DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) _session;
    final ArrayList<Exclusion> exclusions = ResolverUtil.deriveGlobalExclusions( model );
    session.setDependencySelector( new AndDependencySelector( new ExclusionDependencySelector( exclusions ),
                                                              new ReplacementDependencySelector( model ),
                                                              new OptionalDependencySelector( model ) ) );
    session.setDependencyTraverser( new FatArtifactTraverser() );
    session.setDependencyManager( new ClassicDependencyManager() );
    final DependencyResult result = resolveDependencyScopes( model, onInvalidPomFn );
    result.getRoot().accept( new SourceDownloaderVisitor( this, model ) );
    result.getRoot().accept( new ExternalAnnotationsDownloaderVisitor( this, model ) );
    return result;
  }

  @Nonnull
  private DependencyResult resolveDependencyScopes( @Nonnull final ApplicationModel model,
                                                    @Nonnull final OnInvalidPomFn onInvalidPomFn )
    throws DependencyResolutionException
  {
    final Map<String, ResolutionScope> scopes = new LinkedHashMap<>();
    model.getArtifacts().stream().filter( ArtifactModel::isVersioned ).forEach( artifactModel ->
      addDependencyToScope( scopes,
                            getRepositories( artifactModel ),
                            toDependency( artifactModel,
                                          e -> onInvalidPomFn.onInvalidPom( artifactModel, e ) ) ) );
    model.getSystemArtifacts().forEach( artifactModel ->
      addDependencyToScope( scopes,
                            _defaultRepositories,
                            toDependency( artifactModel,
                                          e -> onInvalidPomFn.onInvalidPom( artifactModel, e ) ) ) );
    if ( scopes.isEmpty() )
    {
      scopes.put( toRepositoryKey( _defaultRepositories ), new ResolutionScope( _defaultRepositories ) );
    }

    final List<DependencyResult> results = new ArrayList<>();
    for ( final ResolutionScope scope : scopes.values() )
    {
      results.add( resolveDependencies( scope._dependencies, scope._repositories ) );
    }
    return mergeDependencyResults( results );
  }

  private void addDependencyToScope( @Nonnull final Map<String, ResolutionScope> scopes,
                                     @Nonnull final List<RemoteRepository> repositories,
                                     @Nonnull final Dependency dependency )
  {
    final ResolutionScope scope =
      scopes.computeIfAbsent( toRepositoryKey( repositories ), key -> new ResolutionScope( repositories ) );
    scope._dependencies.add( dependency );
  }

  @Nonnull
  private String toRepositoryKey( @Nonnull final List<RemoteRepository> repositories )
  {
    return repositories.stream().map( RemoteRepository::getId ).collect( Collectors.joining( "\n" ) );
  }

  @Nonnull
  private DependencyResult mergeDependencyResults( @Nonnull final List<DependencyResult> results )
  {
    if ( 1 == results.size() )
    {
      return results.get( 0 );
    }

    final DependencyResult first = results.get( 0 );
    final List<DependencyNode> children = new ArrayList<>();
    final List<org.eclipse.aether.graph.DependencyCycle> cycles = new ArrayList<>();
    final List<Exception> collectExceptions = new ArrayList<>();
    final List<ArtifactResult> artifactResults = new ArrayList<>();
    for ( final DependencyResult result : results )
    {
      children.addAll( result.getRoot().getChildren() );
      cycles.addAll( result.getCycles() );
      collectExceptions.addAll( result.getCollectExceptions() );
      artifactResults.addAll( result.getArtifactResults() );
    }

    final DefaultDependencyNode root = new DefaultDependencyNode( (Dependency) null );
    root.setChildren( children );
    return new DependencyResult( first.getRequest() )
      .setRoot( root )
      .setCycles( cycles )
      .setCollectExceptions( collectExceptions )
      .setArtifactResults( artifactResults );
  }

  @Nonnull
  private DependencyResult resolveDependencies( @Nonnull final List<Dependency> dependencies,
                                                @Nonnull final List<RemoteRepository> repositories )
    throws DependencyResolutionException
  {
    final CollectRequest collectRequest = new CollectRequest();
    collectRequest.setDependencies( dependencies );
    collectRequest.setRepositories( repositories );
    // This filter may also need to skip artifacts with replacements.
    final DependencyFilter filter =
      ( node, parents ) -> !node.getData().containsKey( ConflictResolver.NODE_DATA_WINNER );
    return _system.resolveDependencies( _session, new DependencyRequest( collectRequest, filter ) );
  }

  @Nonnull
  List<Dependency> deriveRootDependencies( @Nonnull final ApplicationModel model,
                                           @Nonnull final OnInvalidPomFn onInvalidPomFn )
  {
    final List<ArtifactModel> artifactModels =
      model.getArtifacts().stream().filter( ArtifactModel::isVersioned ).collect( Collectors.toList() );
    artifactModels.addAll( model.getSystemArtifacts() );
    return deriveDependencies( artifactModels, onInvalidPomFn );
  }

  @Nonnull
  private List<Dependency> deriveDependencies( @Nonnull final List<ArtifactModel> artifactModels,
                                               @Nonnull final OnInvalidPomFn onInvalidPomFn )
  {
    final List<Dependency> dependencies = new ArrayList<>();
    for ( final ArtifactModel artifactModel : artifactModels )
    {
      dependencies.add( toDependency( artifactModel, e -> onInvalidPomFn.onInvalidPom( artifactModel, e ) ) );
    }
    return dependencies;
  }

  @Nonnull
  private Dependency toDependency( @Nonnull final ArtifactModel artifactModel,
                                   @Nonnull final Consumer<List<Exception>> onInvalidPomFn )
  {
    return new Dependency( toArtifact( artifactModel, onInvalidPomFn ),
                           Artifact.SCOPE_COMPILE,
                           Boolean.FALSE,
                           ResolverUtil.deriveExclusions( artifactModel ) );
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
      final List<RemoteRepository> remoteRepositories = getRepositories( model );
      final ArtifactResult artifactResult =
        _system.resolveArtifact( _session, new ArtifactRequest( artifact, remoteRepositories, null ) );

      final ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest( artifactResult.getArtifact(), remoteRepositories, null );

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
      _environment.logger().warning( null != message ? message : are.toString() );
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

  @Nonnull
  private List<RemoteRepository> selectRepositories( @Nonnull final List<String> repositoryIds )
  {
    final ArrayList<RemoteRepository> repositories = new ArrayList<>();
    for ( final RemoteRepository repository : _repositories )
    {
      if ( repositoryIds.contains( repository.getId() ) )
      {
        repositories.add( repository );
      }
    }
    return repositories;
  }

  private static final class ResolutionScope
  {
    @Nonnull
    private final List<RemoteRepository> _repositories;
    @Nonnull
    private final List<Dependency> _dependencies = new ArrayList<>();

    private ResolutionScope( @Nonnull final List<RemoteRepository> repositories )
    {
      _repositories = Objects.requireNonNull( repositories );
    }
  }
}
