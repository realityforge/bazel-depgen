package org.realityforge.bazel.depgen.config;

import javax.annotation.Nullable;

public final class JavaConfig
{
  @Nullable
  private Boolean exportDeps;
  @Nullable
  private String name;

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
  public String getName()
  {
    return name;
  }

  public void setName( @Nullable final String name )
  {
    this.name = name;
  }
}
