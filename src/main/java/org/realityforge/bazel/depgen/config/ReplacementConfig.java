package org.realityforge.bazel.depgen.config;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ReplacementConfig
{
  @Nullable
  private String coord;
  @Nullable
  private List<ReplacementTargetConfig> targets;

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
  public List<ReplacementTargetConfig> getTargets()
  {
    return targets;
  }

  public void setTargets( @Nonnull final List<ReplacementTargetConfig> targets )
  {
    this.targets = Objects.requireNonNull( targets );
  }
}
