package org.realityforge.bazel.depgen.util;

import java.util.Objects;
import javax.annotation.Nonnull;

public final class Edge<N, E>
{
  @Nonnull
  private final Node<N, E> _source;
  @Nonnull
  private final Node<N, E> _destination;

  Edge( @Nonnull final Node<N, E> source, @Nonnull final Node<N, E> destination )
  {
    _source = Objects.requireNonNull( source );
    _destination = Objects.requireNonNull( destination );
  }

  @Nonnull
  public Node<N, E> getSource()
  {
    return _source;
  }

  @Nonnull
  public Node<N, E> getDestination()
  {
    return _destination;
  }
}
