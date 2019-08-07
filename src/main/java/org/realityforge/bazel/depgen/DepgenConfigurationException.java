package org.realityforge.bazel.depgen;

public final class DepgenConfigurationException
  extends DepgenException
{
  public DepgenConfigurationException( final String message )
  {
    super( message );
  }

  public DepgenConfigurationException( final String message, final Throwable cause )
  {
    super( message, cause );
  }
}
