package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InvalidModelException
  extends RuntimeException
{
  @Nonnull
  private final Object _model;

  public InvalidModelException( @Nullable final String message, @Nonnull final Object model )
  {
    super( message );
    _model = Objects.requireNonNull( model );
  }

  @Nonnull
  public Object getModel()
  {
    return _model;
  }
}
