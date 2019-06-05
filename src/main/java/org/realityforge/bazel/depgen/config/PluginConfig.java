package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PluginConfig
{
  @Nullable
  private Boolean generatesApi;

  @Nullable
  public Boolean getGeneratesApi()
  {
    return generatesApi;
  }

  public void setGeneratesApi( @Nonnull final Boolean generatesApi )
  {
    this.generatesApi = Objects.requireNonNull( generatesApi );
  }
}
