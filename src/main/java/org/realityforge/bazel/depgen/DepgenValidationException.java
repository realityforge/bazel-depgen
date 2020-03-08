package org.realityforge.bazel.depgen;

import javax.annotation.Nonnull;

public final class DepgenValidationException
  extends DepgenException
{
  public DepgenValidationException( @Nonnull final String message )
  {
    super( message );
  }
}
