package org.realityforge.bazel.depgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;

/**
 * A dependency visitor that emits the graph in a format based on Mavens output.
 */
public final class DependencyGraphEmitter
  implements DependencyVisitor
{
  @FunctionalInterface
  public interface LineEmitterFn
  {
    void emitLine( @Nonnull String line );
  }

  @Nonnull
  private final ApplicationModel _model;
  @Nonnull
  private final LineEmitterFn _emitter;
  @Nonnull
  private final List<ChildInfo> _childInfos = new ArrayList<>();

  public DependencyGraphEmitter( @Nonnull final ApplicationModel model, @Nonnull final LineEmitterFn emitter )
  {
    _model = Objects.requireNonNull( model );
    _emitter = Objects.requireNonNull( emitter );
  }

  @Override
  public boolean visitEnter( @Nonnull final DependencyNode node )
  {
    final String line = formatIndentation() + formatNode( node );
    if ( !line.isEmpty() )
    {
      _emitter.emitLine( line );
    }
    _childInfos.add( new ChildInfo( node.getChildren().size() ) );
    return true;
  }

  @Nonnull
  private String formatIndentation()
  {
    final StringBuilder buffer = new StringBuilder( 128 );
    for ( final Iterator<ChildInfo> it = _childInfos.iterator(); it.hasNext(); )
    {
      buffer.append( it.next().formatIndentation( !it.hasNext() ) );
    }
    return buffer.toString();
  }

  @Nonnull
  private String formatNode( @Nonnull final DependencyNode node )
  {
    final StringBuilder buffer = new StringBuilder( 128 );
    final Artifact a = node.getArtifact();
    final Dependency d = node.getDependency();
    if ( null != a )
    {
      buffer.append( a );
    }
    if ( null != d && d.getScope().length() > 0 )
    {
      buffer.append( " [" ).append( d.getScope() );
      if ( d.isOptional() )
      {
        buffer.append( ", optional" );
      }
      buffer.append( "]" );
    }
    {
      final String premanaged = DependencyManagerUtils.getPremanagedVersion( node );
      if ( null != premanaged && null != a && !premanaged.equals( a.getBaseVersion() ) )
      {
        buffer.append( " (version managed from " ).append( premanaged ).append( ")" );
      }
    }
    {
      final String premanaged = DependencyManagerUtils.getPremanagedScope( node );
      if ( null != premanaged && null != d && !premanaged.equals( d.getScope() ) )
      {
        buffer.append( " (scope managed from " ).append( premanaged ).append( ")" );
      }
    }
    final DependencyNode winner = (DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
    if ( winner != null && !ArtifactIdUtils.equalsId( a, winner.getArtifact() ) )
    {
      final Artifact w = winner.getArtifact();
      buffer.append( " (conflicts with " );
      assert null != a;
      if ( ArtifactIdUtils.toVersionlessId( a ).equals( ArtifactIdUtils.toVersionlessId( w ) ) )
      {
        buffer.append( w.getVersion() );
      }
      else
      {
        buffer.append( w );
      }
      buffer.append( ")" );
    }
    final boolean excluded =
      null != d && _model.isExcluded( d.getArtifact().getGroupId(), d.getArtifact().getArtifactId() );
    if ( excluded )
    {
      buffer.append( " EXCLUDED" );
    }
    final ReplacementModel replacementModel =
      null != d ? _model.findReplacement( d.getArtifact().getGroupId(), d.getArtifact().getArtifactId() ) : null;
    if ( null != replacementModel )
    {
      buffer.append( " REPLACED BY " );
      buffer.append( replacementModel.getTargets()
                       .stream()
                       .map( t -> t.getTarget() + " (" + t.getNature() + ")" )
                       .collect( Collectors.joining( ", " ) ) );
    }
    return buffer.toString();
  }

  @Override
  public boolean visitLeave( @Nonnull final DependencyNode node )
  {
    if ( !_childInfos.isEmpty() )
    {
      _childInfos.remove( _childInfos.size() - 1 );
    }
    if ( !_childInfos.isEmpty() )
    {
      _childInfos.get( _childInfos.size() - 1 )._index++;
    }
    return true;
  }

  private static class ChildInfo
  {
    final int _count;
    int _index;

    ChildInfo( final int count )
    {
      this._count = count;
    }

    String formatIndentation( final boolean end )
    {
      final boolean last = _index + 1 >= _count;
      if ( end )
      {
        return last ? "\\- " : "+- ";
      }
      else
      {
        return last ? "   " : "|  ";
      }
    }
  }
}
