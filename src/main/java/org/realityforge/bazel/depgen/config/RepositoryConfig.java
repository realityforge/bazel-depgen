package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RepositoryConfig
{
  @Nullable
  private String name;
  @Nullable
  private String url;

  @Nullable
  public String getName()
  {
    return name;
  }

  public void setName( @Nonnull final String name )
  {
    this.name = Objects.requireNonNull( name );
  }

  @Nullable
  public String getUrl()
  {
    return url;
  }

  public void setUrl( @Nonnull final String url )
  {
    this.url = Objects.requireNonNull( url );
  }
}
