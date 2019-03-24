package org.realityforge.bazel.depgen.config;

import java.util.Map;
import javax.annotation.Nullable;

public class DepGenConfig
{
  @Nullable
  private Map<String, String> repositories;

  @Nullable
  public Map<String, String> getRepositories()
  {
    return repositories;
  }

  public void setRepositories( @Nullable final Map<String, String> repositories )
  {
    this.repositories = repositories;
  }
}
