package org.realityforge.bazel.depgen.record;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.aether.graph.DependencyNode;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ArtifactModel;

public final class ApplicationRecord
{
  @Nonnull
  private final ApplicationModel _source;
  @Nonnull
  private final DependencyNode _node;
  @Nonnull
  private final Map<String, ArtifactRecord> _artifacts = new HashMap<>();

  @Nonnull
  public static ApplicationRecord build( @Nonnull final ApplicationModel model, @Nonnull final DependencyNode node )
  {
    final ApplicationRecord record = new ApplicationRecord( model, node );
    node.accept( new DependencyCollector( record ) );
    return record;
  }

  private ApplicationRecord( @Nonnull final ApplicationModel source, @Nonnull final DependencyNode node )
  {
    _source = Objects.requireNonNull( source );
    _node = Objects.requireNonNull( node );
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
  public List<ArtifactRecord> getArtifacts()
  {
    return _artifacts
      .values()
      .stream()
      .sorted( Comparator.comparing( ArtifactRecord::getKey ) )
      .collect( Collectors.toList() );
  }

  void artifact( @Nonnull final DependencyNode node )
  {
    final String groupId = node.getArtifact().getGroupId();
    final String artifactId = node.getArtifact().getArtifactId();
    final ArtifactModel model = _source.findArtifact( groupId, artifactId );
    final String key = RecordUtil.toArtifactKey( node );
    assert !_artifacts.containsKey( key );
    _artifacts.put( key, new ArtifactRecord( this, node, model ) );
  }

  @Nullable
  ArtifactRecord findArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return _artifacts
      .values()
      .stream()
      .filter( m -> m.getNode().getDependency().getArtifact().getGroupId().equals( groupId ) &&
                    m.getNode().getDependency().getArtifact().getArtifactId().equals( artifactId ) )
      .findAny()
      .orElse( null );
  }
}
