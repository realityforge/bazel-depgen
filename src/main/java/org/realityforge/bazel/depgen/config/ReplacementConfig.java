package org.realityforge.bazel.depgen.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReplacementConfig
{
  @Nullable
  private String coord;
  @Nullable
  private String group;
  @Nullable
  private String id;
  @Nullable
  private String target;

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
  public String getGroup()
  {
    return group;
  }

  public void setGroup( @Nonnull final String group )
  {
    this.group = Objects.requireNonNull( group );
  }

  @Nullable
  public String getId()
  {
    return id;
  }

  public void setId( @Nonnull final String id )
  {
    this.id = Objects.requireNonNull( id );
  }

  @Nullable
  public String getTarget()
  {
    return target;
  }

  public void setTarget( @Nonnull final String target )
  {
    this.target = Objects.requireNonNull( target );
  }
}
