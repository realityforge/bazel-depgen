package org.realityforge.bazel.depgen;

public class DepgenException
  extends RuntimeException
{
  public DepgenException( final String message )
  {
    super( message );
  }

  public DepgenException( final String message, final Throwable cause )
  {
    super( message, cause );
  }
}
