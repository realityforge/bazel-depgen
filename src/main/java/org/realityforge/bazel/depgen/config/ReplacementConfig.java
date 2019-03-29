package org.realityforge.bazel.depgen.config;

public class ReplacementConfig
{
  private String coord;
  private String group;
  private String id;
  private String target;

  public String getCoord()
  {
    return coord;
  }

  public void setCoord( final String coord )
  {
    this.coord = coord;
  }

  public String getGroup()
  {
    return group;
  }

  public void setGroup( final String group )
  {
    this.group = group;
  }

  public String getId()
  {
    return id;
  }

  public void setId( final String id )
  {
    this.id = id;
  }

  public String getTarget()
  {
    return target;
  }

  public void setTarget( final String target )
  {
    this.target = target;
  }

  @Override
  public String toString()
  {
    return "ReplacementConfig[" +
           "coord='" + coord + '\'' +
           ", group='" + group + '\'' +
           ", id='" + id + '\'' +
           ", target='" + target +
           "']";
  }
}
