package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ReplacementConfig
{
  @Nullable
  private String coord;
  @Nullable
  private String target;

  @Nullable
  public String getCoord()
  {
    return coord;
  }

  public void setCoord( @Nonnull final String coord )
  {
    this.coord = Objects.requireNonNull( coord );
  }

  @Nullable
  public String getTarget()
  {
    return target;
  }

  public void setTarget( @Nonnull final String target )
  {
    this.target = Objects.requireNonNull( target );
  }
}
