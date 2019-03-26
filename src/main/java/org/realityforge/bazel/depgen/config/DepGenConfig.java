package org.realityforge.bazel.depgen.config;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@SuppressWarnings( "unused" )
public class DepGenConfig
{
  @Nonnull
  private OptionsConfig options = new OptionsConfig();
  @Nonnull
  private Map<String, String> repositories = new HashMap<>();
  @Nonnull
  private List<ArtifactConfig> artifacts = new ArrayList<>();

  @Nonnull
  public static DepGenConfig parse( @Nonnull final Path path )
    throws Exception
  {
    final Yaml yaml = new Yaml( new Constructor( DepGenConfig.class ) );
    final DepGenConfig config = yaml.load( new FileReader( path.toFile() ) );
    return null == config ? new DepGenConfig() : config;
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
}
