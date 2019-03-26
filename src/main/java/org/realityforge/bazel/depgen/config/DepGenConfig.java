package org.realityforge.bazel.depgen.config;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@SuppressWarnings( "unused" )
public class DepGenConfig
{
  @Nonnull
  private OptionsConfig options = new OptionsConfig();
  @Nullable
  private Map<String, String> repositories;
  private List<ArtifactConfig> artifacts;

  @Nullable
  public static DepGenConfig parse( @Nonnull final Path path )
    throws Exception
  {
    final Yaml yaml = new Yaml( new Constructor( DepGenConfig.class ) );
    return yaml.load( new FileReader( path.toFile() ) );
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

  @Nullable
  public Map<String, String> getRepositories()
  {
    return repositories;
  }

  public void setRepositories( @Nullable final Map<String, String> repositories )
  {
    this.repositories = repositories;
  }

  @Nullable
  public List<ArtifactConfig> getArtifacts()
  {
    return artifacts;
  }

  public void setArtifacts( @Nullable final List<ArtifactConfig> artifacts )
  {
    this.artifacts = artifacts;
  }
}
