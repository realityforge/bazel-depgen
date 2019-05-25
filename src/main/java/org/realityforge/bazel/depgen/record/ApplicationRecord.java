package org.realityforge.bazel.depgen.record;

import java.io.IOException;
import java.nio.file.Path;
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
import org.realityforge.bazel.depgen.DependencyGraphEmitter;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.gen.StarlarkOutput;
import org.realityforge.bazel.depgen.metadata.RecordBuildCallback;
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
                                         @Nonnull final List<AuthenticationContext> authenticationContexts,
                                         @Nonnull final RecordBuildCallback callback )
  {
    final ApplicationRecord record = new ApplicationRecord( model, node, authenticationContexts );
    node.accept( new DependencyCollector( record, callback ) );
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

  /**
   * Return the relative path from the extension file to the source dependency file.
   *
   * @return the relative path from the  extension file to the source dependency file.
   */
  @Nonnull
  public Path getPathFromExtensionToConfig()
  {
    final Path configLocation = _source.getConfigLocation();
    final Path extensionFile = _source.getOptions().getExtensionFile();
    return extensionFile.getParent()
      .toAbsolutePath()
      .normalize()
      .relativize( configLocation.toAbsolutePath().normalize() );
  }

  public void writeTargetMacro( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.writeMacro( getSource().getOptions().getTargetMacroName(),
                       getArtifacts()
                         .stream()
                         .filter( a -> null == a.getReplacementModel() )
                         .map( a -> "omit_" + a.getAlias() + " = False" )
                         .collect( Collectors.toList() ), macro -> {
        macro.writeMultilineComment( o -> o.write( "Macro to define targets for dependencies specified by '" +
                                                   getPathFromExtensionToConfig() +
                                                   "'." ) );
        for ( final ArtifactRecord artifact : getArtifacts() )
        {
          if ( null == artifact.getReplacementModel() )
          {
            macro.newLine();
            macro.writeIfCondition( "not omit_" + artifact.getAlias(), artifact::emitArtifactTargets );
          }
        }
      } );
  }

  public void writeWorkspaceMacro( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    output.writeMacro( getSource().getOptions().getWorkspaceMacroName(),
                       getArtifacts()
                         .stream()
                         .filter( a -> null == a.getReplacementModel() )
                         .map( a -> "omit_" + a.getAlias() + " = False" )
                         .collect( Collectors.toList() ), macro -> {
        macro.writeMultilineComment( o -> {
          o.write( "Repository rules macro to load dependencies specified by '" +
                   getPathFromExtensionToConfig() +
                   "'." );
          o.newLine();
          o.write( "Must be run from a WORKSPACE file." );
        } );

        for ( final ArtifactRecord artifact : getArtifacts() )
        {
          if ( null == artifact.getReplacementModel() )
          {
            macro.newLine();
            macro.writeIfCondition( "not omit_" + artifact.getAlias(), o -> {
              artifact.emitArtifactHttpFileRule( o );

              final String sourceSha256 = artifact.getSourceSha256();
              if ( null != sourceSha256 )
              {
                o.newLine();
                final List<String> sourceUrls = artifact.getSourceUrls();
                assert null != sourceUrls && !sourceUrls.isEmpty();
                artifact.emitArtifactSourcesHttpFileRule( o );
              }
            } );
          }
        }
      } );
  }

  public void emitDependencyGraphIfRequired( @Nonnull final StarlarkOutput output )
    throws IOException
  {
    final ApplicationModel source = getSource();
    if ( source.getOptions().emitDependencyGraph() )
    {
      output.write( "# Dependency Graph Generated from the input data" );
      getNode().accept( new DependencyGraphEmitter( source, line -> {

        try
        {
          output.write( "# " + line );
        }
        catch ( final IOException ioe )
        {
          throw new IllegalStateException( ioe );
        }
      } ) );
      output.newLine();
    }
  }

  void replacement( @Nonnull final DependencyNode node )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ReplacementModel model = _source.findReplacement( groupId, artifactId );
    assert null != model;
    final ArtifactRecord record = new ArtifactRecord( this, node, null, null, null, null, null, null, model );
    final String key = record.getKey();
    assert !_artifacts.containsKey( key );
    _artifacts.put( key, record );
  }

  void artifact( @Nonnull final DependencyNode node,
                 @Nonnull final String sha256,
                 @Nonnull final List<String> urls,
                 @Nullable final String sourceSha256,
                 @Nullable final List<String> sourceUrls,
                 @Nullable final List<String> processors )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ArtifactModel model = _source.findArtifact( groupId, artifactId );
    final ArtifactRecord record =
      new ArtifactRecord( this, node, sha256, urls, sourceSha256, sourceUrls, processors, model, null );
    if ( null != model &&
         null != model.getSource().getGeneratesApi() &&
         ( null == processors || Nature.Library == record.getNature() ) )
    {
      final String message =
        "Artifact '" + node.getArtifact() + "' has specified the 'generatesApi' configuration " +
        "setting but is not a plugin or contains no annotation processors.";
      throw new IllegalStateException( message );
    }
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
