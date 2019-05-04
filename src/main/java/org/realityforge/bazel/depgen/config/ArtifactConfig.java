package org.realityforge.bazel.depgen.config;

import java.util.List;

public class ArtifactConfig
{
  private String coord;
  private String group;
  private String id;
  private List<String> ids;
  private String type;
  private String classifier;
  private String version;
  private Boolean includeOptional;
  private List<String> excludes;

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

  public List<String> getIds()
  {
    return ids;
  }

  public void setIds( final List<String> ids )
  {
    this.ids = ids;
  }

  public String getType()
  {
    return type;
  }

  public void setType( final String type )
  {
    this.type = type;
  }

  public String getClassifier()
  {
    return classifier;
  }

  public void setClassifier( final String classifier )
  {
    this.classifier = classifier;
  }

  public String getVersion()
  {
    return version;
  }

  public void setVersion( final String version )
  {
    this.version = version;
  }

  public Boolean getIncludeOptional()
  {
    return includeOptional;
  }

  public void setIncludeOptional( final Boolean includeOptional )
  {
    this.includeOptional = includeOptional;
  }

  public List<String> getExcludes()
  {
    return excludes;
  }

  public void setExcludes( final List<String> excludes )
  {
    this.excludes = excludes;
  }

  @Override
  public String toString()
  {
    return "ArtifactConfig[" +
           "coord='" + coord + '\'' +
           ", group='" + group + '\'' +
           ", id='" + id + '\'' +
           ", ids='" + ids + '\'' +
           ", type='" + type + '\'' +
           ", classifier='" + classifier + '\'' +
           ", version='" + version + '\'' +
           ", includeOptional=" + includeOptional +
           ", excludes=" + excludes +
           "']";
  }
}
