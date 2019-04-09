package org.realityforge.bazel.depgen.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DirectedGraph<N, E>
{
  @Nonnull
  private final Map<N, Node<N, E>> _nodes = new HashMap<>();
  @Nonnull
  private final Map<N, Node<N, E>> _roNodes = Collections.unmodifiableMap( _nodes );

  public boolean addNode( @Nonnull final N element )
  {
    if ( containsNode( element ) )
    {
      return false;
    }
    else
    {
      _nodes.put( element, new Node<>( element ) );
      return true;
    }
  }

  @Nullable
  public Node<N, E> findNodeByElement( @Nonnull final N element )
    throws NoSuchElementException
  {
    return _nodes.get( element );
  }

  @Nonnull
  public Node<N, E> getNodeByElement( @Nonnull final N element )
    throws NoSuchElementException
  {
    final Node<N, E> node = findNodeByElement( element );
    if ( null == node )
    {
      throw new NoSuchElementException();
    }
    return node;
  }

  @Nonnull
  public Collection<Node<N, E>> getNodes()
  {
    return _roNodes.values();
  }

  public boolean addEdge( @Nonnull final E element, @Nonnull final N source, @Nonnull final N destination )
  {
    final Node<N, E> sourceNode = _nodes.get( source );
    assert null != sourceNode;
    final Node<N, E> destinationNode = _nodes.get( destination );
    assert null != destinationNode;

    final Edge<N, E> edge = new Edge<>( element, sourceNode, destinationNode );
    return sourceNode.addOutgoingEdge( edge ) && destinationNode.addIncomingEdge( edge );
  }

  public boolean containsNode( @Nonnull final N element )
  {
    return _nodes.containsKey( element );
  }
}
