package org.realityforge.bazel.depgen.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ArtifactConfig
{
  @Nullable
  private String coord;
  @Nullable
  private String alias;
  @Nullable
  private Nature nature;
  @Nullable
  private Boolean includeOptional;
  @Nullable
  private Boolean includeSource;
  @Nullable
  private Boolean exportDeps;
  @Nullable
  private Boolean generatesApi;
  @Nullable
  private List<String> excludes;
  @Nullable
  private List<String> visibility;
  @Nullable
  private List<Language> languages;
  @Nullable
  private ArtifactJ2clConfig j2cl;

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
  public String getAlias()
  {
    return alias;
  }

  public void setAlias( @Nonnull final String alias )
  {
    this.alias = Objects.requireNonNull( alias );
  }

  @Nullable
  public Nature getNature()
  {
    return nature;
  }

  public void setNature( @Nonnull final Nature nature )
  {
    this.nature = Objects.requireNonNull( nature );
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
  public Boolean getGeneratesApi()
  {
    return generatesApi;
  }

  public void setGeneratesApi( @Nonnull final Boolean generatesApi )
  {
    this.generatesApi = Objects.requireNonNull( generatesApi );
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

  @Nullable
  public List<Language> getLanguages()
  {
    return languages;
  }

  public void setLanguages( @Nonnull final List<Language> languages )
  {
    this.languages = Collections.unmodifiableList( Objects.requireNonNull( languages ) );
  }

  @Nullable
  public ArtifactJ2clConfig getJ2cl()
  {
    return j2cl;
  }

  public void setJ2cl( @Nonnull final ArtifactJ2clConfig j2cl )
  {
    this.j2cl = Objects.requireNonNull( j2cl );
  }
}
