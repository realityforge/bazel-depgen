package org.realityforge.bazel.depgen.record;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.AuthenticationContext;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;

public final class ApplicationRecord
{
  @Nonnull
  private final ApplicationModel _source;
  @Nonnull
  private final DependencyNode _node;
  @Nonnull
  private final Map<String, ArtifactRecord> _artifacts = new HashMap<>();
  @Nonnull
  private final Map<String, AuthenticationContext> _authenticationContexts;

  @Nonnull
  public static ApplicationRecord build( @Nonnull final ApplicationModel model,
                                         @Nonnull final DependencyNode node,
                                         @Nonnull final List<AuthenticationContext> authenticationContexts )
  {
    final ApplicationRecord record = new ApplicationRecord( model, node, authenticationContexts );
    node.accept( new DependencyCollector( record ) );
    ensureAliasesAreUnique( record );
    return record;
  }

  private static void ensureAliasesAreUnique( @Nonnull final ApplicationRecord record )
  {
    final HashMap<String, ArtifactRecord> aliases = new HashMap<>();
    for ( final ArtifactRecord artifact : record.getArtifacts() )
    {
      final String alias = artifact.getAlias();
      final ArtifactRecord existing = aliases.get( alias );
      if ( null != existing )
      {
        throw new IllegalStateException( "Multiple artifacts have the same alias '" + alias + "' which is " +
                                         "not supported. Either change the aliasStrategy option or explicitly " +
                                         "specify the alias for the artifacts '" + existing.getArtifact() +
                                         "' and '" + artifact.getArtifact() + "'." );
      }
      else
      {
        aliases.put( alias, artifact );
      }
    }
  }

  private ApplicationRecord( @Nonnull final ApplicationModel source,
                             @Nonnull final DependencyNode node,
                             @Nonnull final List<AuthenticationContext> authenticationContexts )
  {
    _source = Objects.requireNonNull( source );
    _node = Objects.requireNonNull( node );
    final Map<String, AuthenticationContext> contexts = new HashMap<>();
    authenticationContexts.forEach( c -> contexts.put( c.getRepository().getId(), c ) );
    _authenticationContexts = Collections.unmodifiableMap( contexts );
  }

  @Nonnull
  public ApplicationModel getSource()
  {
    return _source;
  }

  @Nonnull
  public DependencyNode getNode()
  {
    return _node;
  }

  @Nonnull
  public Map<String, AuthenticationContext> getAuthenticationContexts()
  {
    return _authenticationContexts;
  }

  @Nonnull
  public List<ArtifactRecord> getArtifacts()
  {
    return _artifacts
      .values()
      .stream()
      .sorted( Comparator.comparing( ArtifactRecord::getKey ) )
      .collect( Collectors.toList() );
  }

  void replacement( @Nonnull final DependencyNode node )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ReplacementModel model = _source.findReplacement( groupId, artifactId );
    assert null != model;
    final ArtifactRecord record = new ArtifactRecord( this, node, null, null, null, null, null, model );
    final String key = record.getKey();
    assert !_artifacts.containsKey( key );
    _artifacts.put( key, record );
  }

  void artifact( @Nonnull final DependencyNode node,
                 @Nonnull final String sha256,
                 @Nonnull final List<String> urls,
                 @Nullable final String sourceSha256,
                 @Nullable final List<String> sourceUrls )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ArtifactModel model = _source.findArtifact( groupId, artifactId );
    final ArtifactRecord record = new ArtifactRecord( this, node, sha256, urls, sourceSha256, sourceUrls, model, null );
    final String key = record.getKey();
    final ArtifactRecord existing = _artifacts.get( key );
    if ( null == existing )
    {
      _artifacts.put( key, record );
    }
    else
    {
      if ( !"".equals( existing.getArtifact().getClassifier() ) && "".equals( node.getArtifact().getClassifier() ) )
      {
        _artifacts.put( key, record );
      }
    }
  }

  @Nonnull
  ArtifactRecord getArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return Objects.requireNonNull( findArtifact( groupId, artifactId ) );
  }

  @Nullable
  ArtifactRecord findArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findArtifact( m -> m.shouldMatch( groupId, artifactId ) );
  }

  @Nullable
  private ArtifactRecord findArtifact( @Nonnull final Predicate<ArtifactRecord> predicate )
  {
    return _artifacts
      .values()
      .stream()
      .filter( predicate )
      .findAny()
      .orElse( null );
  }
}
