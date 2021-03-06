package org.realityforge.bazel.depgen.config;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class ApplicationConfig
{
  @Nonnull
  public static final String DEFAULT_MODULE = "thirdparty";
  @Nonnull
  public static final String FILENAME = "dependencies.yml";
  @Nonnull
  public static final String MAVEN_CENTRAL_NAME = "central";
  @Nonnull
  public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
  @Nullable
  private Path _configLocation;
  @Nullable
  private OptionsConfig options;
  @Nullable
  private List<RepositoryConfig> repositories;
  @Nullable
  private List<ArtifactConfig> artifacts;
  @Nullable
  private List<ReplacementConfig> replacements;
  @Nullable
  private List<ExcludeConfig> excludes;

  @Nonnull
  public static ApplicationConfig load( @Nonnull final Path path )
    throws Exception
  {
    final Yaml yaml = new Yaml( new Constructor( ApplicationConfig.class ) );
    final ApplicationConfig config = yaml.load( new FileReader( path.toFile() ) );
    final ApplicationConfig applicationConfig = null == config ? new ApplicationConfig() : config;
    applicationConfig.setConfigLocation( path );
    return applicationConfig;
  }

  private void setConfigLocation( @Nonnull final Path configLocation )
  {
    _configLocation = Objects.requireNonNull( configLocation );
  }

  @Nonnull
  public Path getConfigLocation()
  {
    assert null != _configLocation;
    return _configLocation;
  }

  @Nullable
  public OptionsConfig getOptions()
  {
    return options;
  }

  public void setOptions( @Nonnull final OptionsConfig options )
  {
    this.options = Objects.requireNonNull( options );
  }

  @Nullable
  public List<RepositoryConfig> getRepositories()
  {
    return repositories;
  }

  public void setRepositories( @Nonnull final List<RepositoryConfig> repositories )
  {
    this.repositories = Objects.requireNonNull( repositories );
  }

  @Nullable
  public List<ArtifactConfig> getArtifacts()
  {
    return artifacts;
  }

  public void setArtifacts( @Nonnull final List<ArtifactConfig> artifacts )
  {
    this.artifacts = Objects.requireNonNull( artifacts );
  }

  @Nullable
  public List<ReplacementConfig> getReplacements()
  {
    return replacements;
  }

  public void setReplacements( @Nonnull final List<ReplacementConfig> replacements )
  {
    this.replacements = Objects.requireNonNull( replacements );
  }

  @Nullable
  public List<ExcludeConfig> getExcludes()
  {
    return excludes;
  }

  public void setExcludes( @Nonnull final List<ExcludeConfig> excludes )
  {
    this.excludes = Objects.requireNonNull( excludes );
  }
}
