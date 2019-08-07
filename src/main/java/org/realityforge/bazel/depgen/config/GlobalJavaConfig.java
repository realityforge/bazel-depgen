package org.realityforge.bazel.depgen.config;

import javax.annotation.Nullable;

public final class GlobalJavaConfig
{
  @Nullable
  private Boolean exportDeps;

  @Nullable
  public Boolean getExportDeps()
  {
    return exportDeps;
  }

  public void setExportDeps( @Nullable final Boolean exportDeps )
  {
    this.exportDeps = exportDeps;
  }
}
