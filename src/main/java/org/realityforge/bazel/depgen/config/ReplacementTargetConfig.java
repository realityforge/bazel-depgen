package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ReplacementTargetConfig
{
  @Nullable
  private Nature nature;
  @Nullable
  private String target;

  @Nullable
  public Nature getNature()
  {
    return nature;
  }

  public void setNature( @Nonnull final Nature nature )
  {
    this.nature = Objects.requireNonNull( nature );
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
