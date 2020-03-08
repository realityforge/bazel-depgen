package org.realityforge.bazel.depgen;

import java.util.Objects;
import javax.annotation.Nonnull;

public class DepgenException
  extends RuntimeException
{
  public DepgenException( @Nonnull final String message )
  {
    super( message );
  }

  public DepgenException( @Nonnull final String message, @Nonnull final Throwable cause )
  {
    super( Objects.requireNonNull( message ), Objects.requireNonNull( cause ) );
  }
}
