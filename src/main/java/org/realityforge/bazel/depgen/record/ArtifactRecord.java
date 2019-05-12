package org.realityforge.bazel.depgen.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.realityforge.bazel.depgen.config.AliasStrategy;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;

public final class ArtifactRecord
{
  @Nonnull
  private final ApplicationRecord _application;
  @Nonnull
  private final DependencyNode _node;
  @Nullable
  private final ArtifactModel _artifactModel;
  @Nullable
  private final ReplacementModel _replacementModel;
  @Nullable
  private final String _sha256;
  @Nullable
  private final List<String> _urls;
  @Nullable
  private final String _sourceSha256;
  @Nullable
  private final List<String> _sourceUrls;

  ArtifactRecord( @Nonnull final ApplicationRecord application,
                  @Nonnull final DependencyNode node,
                  @Nullable final String sha256,
                  @Nullable final List<String> urls,
                  @Nullable final String sourceSha256,
                  @Nullable final List<String> sourceUrls,
                  @Nullable final ArtifactModel artifactModel,
                  @Nullable final ReplacementModel replacementModel )
  {
    assert ( null == sha256 && null == urls ) || ( null != sha256 && null != urls && !urls.isEmpty() );
    assert ( null == sourceSha256 && null == sourceUrls ) ||
           ( null != sourceSha256 && null != sourceUrls && !sourceUrls.isEmpty() );
    assert null == artifactModel || null == replacementModel;
    _application = Objects.requireNonNull( application );
    _node = Objects.requireNonNull( node );
    if ( null == replacementModel )
    {
      _sha256 = Objects.requireNonNull( sha256 );
      _urls = Collections.unmodifiableList( new ArrayList<>( Objects.requireNonNull( urls ) ) );
      _replacementModel = null;
      _artifactModel = artifactModel;
      _sourceSha256 = sourceSha256;
      _sourceUrls = null != sourceUrls ?
                    Collections.unmodifiableList( new ArrayList<>( Objects.requireNonNull( sourceUrls ) ) ) :
                    null;
    }
    else
    {
      assert null == sha256;
      assert null == sourceSha256;
      _sha256 = null;
      _urls = null;
      _sourceSha256 = null;
      _sourceUrls = null;
      _replacementModel = replacementModel;
      _artifactModel = null;
    }
  }

  @Nonnull
  public String getKey()
  {
    final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
    return artifact.getGroupId() + ":" + artifact.getArtifactId();
  }

  @Nonnull
  public String getName()
  {
    final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
    return getNamePrefix() +
           RecordUtil.cleanNamePart( artifact.getGroupId() ) +
           "__" +
           RecordUtil.cleanNamePart( artifact.getArtifactId() ) +
           "__" +
           RecordUtil.cleanNamePart( artifact.getVersion() );
  }

  @Nonnull
  public String getLabel()
  {
    return null != _replacementModel ? _replacementModel.getTarget() : getAlias();
  }

  @Nonnull
  public String getAlias()
  {
    final String declaredAlias = null != _artifactModel ? _artifactModel.getAlias() : null;
    if ( null != declaredAlias )
    {
      return RecordUtil.cleanNamePart( declaredAlias );
    }
    else
    {
      final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
      final AliasStrategy aliasStrategy = _application.getSource().getOptions().getAliasStrategy();
      if ( AliasStrategy.GroupIdAndArtifactId == aliasStrategy )
      {
        return getNamePrefix() +
               RecordUtil.cleanNamePart( artifact.getGroupId() ) +
               "__" +
               RecordUtil.cleanNamePart( artifact.getArtifactId() );
      }
      else
      {
        assert AliasStrategy.ArtifactId == aliasStrategy;
        return getNamePrefix() + RecordUtil.cleanNamePart( artifact.getArtifactId() );
      }
    }
  }

  @Nonnull
  private String getNamePrefix()
  {
    return RecordUtil.cleanNamePart( _application.getSource().getOptions().getNamePrefix() );
  }

  @Nonnull
  public String getMavenCoordinatesBazelTag()
  {
    final org.eclipse.aether.artifact.Artifact artifact = getArtifact();
    return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
  }

  @Nonnull
  public org.eclipse.aether.artifact.Artifact getArtifact()
  {
    return _node.getArtifact();
  }

  @Nonnull
  public DependencyNode getNode()
  {
    return _node;
  }

  /**
   * Return the sha256 of the specified artifact.
   * This MUST be null when {@link #getReplacementModel()} is non-null otherwise it must be non-null.
   *
   * @return the sha256 of artifact or null if {@link #getReplacementModel()} returns a non-null value.
   */
  @Nullable
  public String getSha256()
  {
    return _sha256;
  }

  /**
   * Return the urls that the artifact can be downloaded from.
   * This MUST be null when {@link #getReplacementModel()} is non-null otherwise it must be non-null and non-empty.
   *
   * @return the urls.
   */
  @Nullable
  public List<String> getUrls()
  {
    return _urls;
  }

  /**
   * Return the sha256 of the source artifact associated with the artifact.
   * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
   * if {@link #getSourceUrls()} returns a non-null value.
   *
   * @return the sha256 of the source artifact associated with the artifact.
   */
  @Nullable
  public String getSourceSha256()
  {
    return _sourceSha256;
  }

  /**
   * Return the urls that the source artifact can be downloaded from.
   * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
   * if {@link #getSourceSha256()} returns a non-null value.
   *
   * @return the urls.
   */
  @Nullable
  public List<String> getSourceUrls()
  {
    return _sourceUrls;
  }

  /**
   * Return the model that represents the configuration supplied by the user.
   * This method may return null if the artifact was not declared by the user but
   * is a dependency of another artifact or if {@link #getReplacementModel()} returns
   * a non-null value.
   *
   * @return the model if any.
   */
  @Nullable
  public ArtifactModel getArtifactModel()
  {
    return _artifactModel;
  }

  /**
   * Return the replacement for artifact as supplied by the user.
   * This method may return null if the artifact was not as a replacement.
   *
   * @return the replacement if any.
   */
  @Nullable
  public ReplacementModel getReplacementModel()
  {
    return _replacementModel;
  }

  @Nonnull
  public List<ArtifactRecord> getDeps()
  {
    return
      _node
        .getChildren()
        .stream()
        .filter( c -> !_application.getSource().isExcluded( c.getDependency().getArtifact().getGroupId(),
                                                            c.getDependency().getArtifact().getArtifactId() ) )
        .filter( c -> shouldIncludeDependency( Artifact.SCOPE_COMPILE, c ) )
        .map( c -> _application.getArtifact( c.getDependency().getArtifact().getGroupId(),
                                             c.getDependency().getArtifact().getArtifactId() ) )
        .distinct()
        .sorted( Comparator.comparing( ArtifactRecord::getKey ) )
        .collect( Collectors.toList() );
  }

  @Nonnull
  public List<ArtifactRecord> getRuntimeDeps()
  {
    return
      _node
        .getChildren()
        .stream()
        .filter( c -> shouldIncludeDependency( Artifact.SCOPE_RUNTIME, c ) )
        .filter( c -> !_application.getSource().isExcluded( c.getDependency().getArtifact().getGroupId(),
                                                            c.getDependency().getArtifact().getArtifactId() ) )
        .map( c -> _application.getArtifact( c.getDependency().getArtifact().getGroupId(),
                                             c.getDependency().getArtifact().getArtifactId() ) )
        .distinct()
        .sorted( Comparator.comparing( ArtifactRecord::getKey ) )
        .collect( Collectors.toList() );
  }

  private boolean shouldIncludeDependency( @Nonnull final String scope, @Nonnull final DependencyNode c )
  {
    final boolean includeOptional = null != _artifactModel && _artifactModel.includeOptional();
    return ( includeOptional || !c.getDependency().isOptional() ) && scope.equals( c.getDependency().getScope() );
  }

  boolean shouldMatch( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    final DependencyNode node = getNode();
    final org.eclipse.aether.artifact.Artifact artifact = node.getDependency().getArtifact();
    return groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() );
  }
}
