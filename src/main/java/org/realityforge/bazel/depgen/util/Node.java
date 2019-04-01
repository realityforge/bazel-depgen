package org.realityforge.bazel.depgen.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

public final class Node<N, E>
{
  @Nonnull
  private final N _element;
  @Nonnull
  private final Map<Node<N, E>, Edge<N, E>> _outgoingEdges = new HashMap<>();
  @Nonnull
  private final Map<Node<N, E>, Edge<N, E>> _roOutgoingEdges = Collections.unmodifiableMap( _outgoingEdges );
  @Nonnull
  private final Map<Node<N, E>, Edge<N, E>> _incomingEdges = new HashMap<>();
  @Nonnull
  private final Map<Node<N, E>, Edge<N, E>> _roIncomingEdges = Collections.unmodifiableMap( _incomingEdges );

  Node( @Nonnull final N element )
  {
    _element = Objects.requireNonNull( element );
  }

  @Nonnull
  public N getElement()
  {
    return _element;
  }

  @Nonnull
  public Set<Node<N, E>> getOutgoingNodes()
  {
    return _roOutgoingEdges.keySet();
  }

  @Nonnull
  public Collection<Edge<N, E>> getOutgoingEdges()
  {
    return _roOutgoingEdges.values();
  }

  @Nonnull
  public Set<Node<N, E>> getIncomingNodes()
  {
    return _roIncomingEdges.keySet();
  }

  @Nonnull
  public Collection<Edge<N, E>> getIncomingEdges()
  {
    return _roIncomingEdges.values();
  }

  boolean addIncomingEdge( @Nonnull final Edge<N, E> edge )
  {
    assert edge.getDestination() == this;
    return null == _incomingEdges.putIfAbsent( edge.getSource(), edge );
  }

  boolean addOutgoingEdge( @Nonnull final Edge<N, E> edge )
  {
    assert edge.getSource() == this;
    return null == _outgoingEdges.putIfAbsent( edge.getDestination(), edge );
  }
}
