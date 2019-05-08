package org.realityforge.bazel.depgen.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
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
    return null != _artifactModel ? RecordUtil.toArtifactKey( _artifactModel ) : RecordUtil.toArtifactKey( _node );
  }

  @Nonnull
  public String getName()
  {
    return getAlias() + "__" + RecordUtil.cleanNamePart( _node.getArtifact().getVersion() );
  }

  @Nonnull
  public String getAlias()
  {
    final org.eclipse.aether.artifact.Artifact artifact = _node.getArtifact();
    final String prefix = RecordUtil.cleanNamePart( _application.getSource().getOptions().getNamePrefix() );
    return ( prefix.isEmpty() ? "" : prefix.endsWith( "_" ) ? prefix : prefix + "_" ) +
           RecordUtil.cleanNamePart( artifact.getGroupId() ) +
           "__" +
           RecordUtil.cleanNamePart( artifact.getArtifactId() );
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
        .filter( c -> shouldIncludeDependency( Artifact.SCOPE_COMPILE, c ) )
        .map( c -> _application.findArtifact( c.getDependency().getArtifact().getGroupId(),
                                              c.getDependency().getArtifact().getArtifactId() ) )
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
        .map( c -> _application.findArtifact( c.getDependency().getArtifact().getGroupId(),
                                              c.getDependency().getArtifact().getArtifactId() ) )
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
