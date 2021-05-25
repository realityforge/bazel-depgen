package org.realityforge.bazel.depgen.config;

import javax.annotation.Nullable;

public final class JavaConfig
{
  @Nullable
  private Boolean exportDeps;
  @Nullable
  private String alias;

  @Nullable
  public Boolean getExportDeps()
  {
    return exportDeps;
  }

  public void setExportDeps( @Nullable final Boolean exportDeps )
  {
    this.exportDeps = exportDeps;
  }

  @Nullable
  public String getAlias()
  {
    return alias;
  }

  public void setAlias( @Nullable final String alias )
  {
    this.alias = alias;
  }
}
