package org.realityforge.bazel.depgen.config;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ArtifactConfig
{
  @Nullable
  private String coord;
  @Nullable
  private String group;
  @Nullable
  private String id;
  @Nullable
  private List<String> ids;
  @Nullable
  private String type;
  @Nullable
  private String classifier;
  @Nullable
  private String version;
  @Nullable
  private String alias;
  @Nullable
  private Boolean includeOptional;
  @Nullable
  private Boolean includeSource;
  @Nullable
  private Boolean exportDeps;
  @Nullable
  private List<String> excludes;
  @Nullable
  private List<String> visibility;

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
  public List<String> getIds()
  {
    return ids;
  }

  public void setIds( @Nonnull final List<String> ids )
  {
    this.ids = Objects.requireNonNull( ids );
  }

  @Nullable
  public String getType()
  {
    return type;
  }

  public void setType( @Nonnull final String type )
  {
    this.type = Objects.requireNonNull( type );
  }

  @Nullable
  public String getClassifier()
  {
    return classifier;
  }

  public void setClassifier( @Nonnull final String classifier )
  {
    this.classifier = Objects.requireNonNull( classifier );
  }

  @Nullable
  public String getVersion()
  {
    return version;
  }

  public void setVersion( final String version )
  {
    this.version = Objects.requireNonNull( version );
  }

  @Nullable
  public String getAlias()
  {
    return alias;
  }

  public void setAlias( @Nonnull final String alias )
  {
    this.alias = Objects.requireNonNull( alias );
  }

  @Nullable
  public Boolean getIncludeOptional()
  {
    return includeOptional;
  }

  public void setIncludeOptional( final Boolean includeOptional )
  {
    this.includeOptional = Objects.requireNonNull( includeOptional );
  }

  @Nullable
  public Boolean getIncludeSource()
  {
    return includeSource;
  }

  public void setIncludeSource( @Nonnull final Boolean includeSource )
  {
    this.includeSource = Objects.requireNonNull( includeSource );
  }

  @Nullable
  public Boolean getExportDeps()
  {
    return exportDeps;
  }

  public void setExportDeps( @Nonnull final Boolean exportDeps )
  {
    this.exportDeps = Objects.requireNonNull( exportDeps );
  }

  @Nullable
  public List<String> getExcludes()
  {
    return excludes;
  }

  public void setExcludes( @Nonnull final List<String> excludes )
  {
    this.excludes = Objects.requireNonNull( excludes );
  }

  @Nullable
  public List<String> getVisibility()
  {
    return visibility;
  }

  public void setVisibility( @Nonnull final List<String> visibility )
  {
    this.visibility = Objects.requireNonNull( visibility );
  }
}
