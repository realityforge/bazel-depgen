package org.realityforge.bazel.depgen.record;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.realityforge.bazel.depgen.model.ArtifactModel;

public final class ArtifactRecord
{
  @Nonnull
  private final ApplicationRecord _application;
  @Nonnull
  private final DependencyNode _node;
  @Nullable
  private final ArtifactModel _artifactModel;

  ArtifactRecord( @Nonnull final ApplicationRecord application,
                  @Nonnull final DependencyNode node,
                  @Nullable final ArtifactModel artifactModel,
  {
    _application = Objects.requireNonNull( application );
    _node = Objects.requireNonNull( node );
    _artifactModel = artifactModel;
  }

  @Nonnull
  public String getKey()
  {
    return null != _source ? RecordUtil.toArtifactKey( _source ) : RecordUtil.toArtifactKey( _node );
  }

  @Nonnull
  public DependencyNode getNode()
  {
    return _node;
  }

  /**
   * Return the model that represents the configuration supplied by the user.
   * This method may return null if the artifact was not declared by the user but
   * is a dependency of another artifact.
   *
   * @return the model if any.
   */
  @Nullable
  public ArtifactModel getArtifactModel()
  {
    return _artifactModel;
  }

  @Nonnull
  public List<ArtifactRecord> getDeps()
  {
    return
      _node
        .getChildren()
        .stream()
        .filter( c -> !c.getDependency().isOptional() &&
                      Artifact.SCOPE_COMPILE.equals( c.getDependency().getScope() ) )
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
        .filter( c -> !c.getDependency().isOptional() &&
                      Artifact.SCOPE_RUNTIME.equals( c.getDependency().getScope() ) )
        .map( c -> _application.findArtifact( c.getDependency().getArtifact().getGroupId(),
                                              c.getDependency().getArtifact().getArtifactId() ) )
        .collect( Collectors.toList() );
  }

  boolean shouldMatch( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    final DependencyNode node = getNode();
    final org.eclipse.aether.artifact.Artifact artifact = node.getDependency().getArtifact();
    return groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() );
  }
}