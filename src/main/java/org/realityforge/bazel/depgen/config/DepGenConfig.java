package org.realityforge.bazel.depgen.config;

import javax.annotation.Nullable;

public class DepGenConfig
{
  private MavenServer[] repositories;

  @Nullable
  public MavenServer[] getRepositories()
  {
    return repositories;
  }

  public void setRepositories( @Nullable final MavenServer[] repositories )
  {
    this.repositories = repositories;
  }
}
