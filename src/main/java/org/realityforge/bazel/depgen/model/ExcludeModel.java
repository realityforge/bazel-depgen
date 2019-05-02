package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ExcludeModel
{
  @Nonnull
  private final String _group;
  @Nullable
  private final String _id;

  @Nonnull
  public static ExcludeModel parse( @Nonnull final String value )
  {
    final int index = value.indexOf( ':' );
    final String group = -1 == index ? value : value.substring( 0, index );
    final String id = -1 == index ? null : value.substring( index + 1 );
    return new ExcludeModel( group, id );
  }

  public ExcludeModel( @Nonnull final String group, @Nullable final String id )
  {
    _group = Objects.requireNonNull( group );
    _id = id;
  }

  @Nonnull
  public String getGroup()
  {
    return _group;
  }

  public boolean hasId()
  {
    return null != _id;
  }

  @Nullable
  public String getId()
  {
    return _id;
  }
}
