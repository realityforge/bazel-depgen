package org.realityforge.bazel.depgen;

public final class DepgenValidationException
  extends DepgenException
{
  public DepgenValidationException( final String message )
  {
    super( message );
  }

  public DepgenValidationException( final String message, final Throwable cause )
  {
    super( message, cause );
  }
}
