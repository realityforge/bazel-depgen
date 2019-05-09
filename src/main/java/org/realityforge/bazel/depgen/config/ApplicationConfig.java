package org.realityforge.bazel.depgen.config;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@SuppressWarnings( { "unused", "WeakerAccess" } )
public final class ApplicationConfig
{
  public static final String MAVEN_CENTRAL_ID = "central";
  public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
  private Path _configLocation;
  @Nonnull
  private OptionsConfig options = new OptionsConfig();
  @Nonnull
  private Map<String, String> repositories = new HashMap<>();
  @Nonnull
  private List<ArtifactConfig> artifacts = new ArrayList<>();
  @Nonnull
  private List<ReplacementConfig> replacements = new ArrayList<>();

  @Nonnull
  public static ApplicationConfig parse( @Nonnull final Path path )
    throws Exception
  {
    final Yaml yaml = new Yaml( new Constructor( ApplicationConfig.class ) );
    final ApplicationConfig config = yaml.load( new FileReader( path.toFile() ) );
    final ApplicationConfig applicationConfig = null == config ? new ApplicationConfig() : config;
    applicationConfig.setConfigLocation( path );
    return applicationConfig;
  }

  public ApplicationConfig()
  {
    repositories.put( MAVEN_CENTRAL_ID, MAVEN_CENTRAL_URL );
  }

  private void setConfigLocation( @Nonnull final Path configLocation )
  {
    _configLocation = configLocation;
  }

  @Nonnull
  public Path getConfigLocation()
  {
    return _configLocation;
  }

  @Nonnull
  public OptionsConfig getOptions()
  {
    return options;
  }

  public void setOptions( @Nonnull final OptionsConfig options )
  {
    this.options = Objects.requireNonNull( options );
  }

  @Nonnull
  public Map<String, String> getRepositories()
  {
    return repositories;
  }

  public void setRepositories( @Nonnull final Map<String, String> repositories )
  {
    this.repositories = Objects.requireNonNull( repositories );
  }

  @Nonnull
  public List<ArtifactConfig> getArtifacts()
  {
    return artifacts;
  }

  public void setArtifacts( @Nonnull final List<ArtifactConfig> artifacts )
  {
    this.artifacts = Objects.requireNonNull( artifacts );
  }

  @Nonnull
  public List<ReplacementConfig> getReplacements()
  {
    return replacements;
  }

  public void setReplacements( @Nonnull final List<ReplacementConfig> replacements )
  {
    this.replacements = replacements;
  }
}
