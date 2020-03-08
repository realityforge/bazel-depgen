package org.realityforge.bazel.depgen;

import java.util.Objects;
import javax.annotation.Nonnull;

public final class DepgenConfigurationException
  extends DepgenException
{
  public DepgenConfigurationException( @Nonnull final String message )
  {
    super( Objects.requireNonNull( message ) );
  }

  public DepgenConfigurationException( @Nonnull final String message, @Nonnull final Throwable cause )
  {
    super( message, cause );
  }
}
