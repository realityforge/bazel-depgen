package org.realityforge.bazel.depgen.config;

import java.util.List;
import javax.annotation.Nullable;

public final class J2clConfig
{
  @Nullable
  private List<String> suppress;

  @Nullable
  public List<String> getSuppress()
  {
    return suppress;
  }

  public void setSuppress( @Nullable final List<String> suppress )
  {
    this.suppress = suppress;
  }
}
