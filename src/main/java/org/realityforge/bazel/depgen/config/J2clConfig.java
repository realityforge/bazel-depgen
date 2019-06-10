package org.realityforge.bazel.depgen.config;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class J2clConfig
{
  @Nullable
  private List<String> suppress;
  @Nullable
  private J2clMode mode;

  @Nullable
  public List<String> getSuppress()
  {
    return suppress;
  }

  public void setSuppress( @Nonnull final List<String> suppress )
  {
    this.suppress = Objects.requireNonNull( suppress );
  }

  @Nullable
  public J2clMode getMode()
  {
    return mode;
  }

  public void setMode( @Nonnull final J2clMode mode )
  {
    this.mode = Objects.requireNonNull( mode );
  }
}
