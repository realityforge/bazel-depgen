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
  private final ArtifactModel _source;

  public ArtifactRecord( @Nonnull final ApplicationRecord application,
                         @Nonnull final DependencyNode node,
                         @Nullable final ArtifactModel source )
  {
    _application = Objects.requireNonNull( application );
    _node = Objects.requireNonNull( node );
    _source = source;
  }

  @Nonnull
  public String getKey()
  {
    return RecordUtil.toArtifactKey( _node );
  }

  @Nonnull
  public DependencyNode getNode()
  {
    return _node;
  }

  @Nullable
  public ArtifactModel getSource()
  {
    return _source;
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
}
