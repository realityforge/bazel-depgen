package org.realityforge.bazel.depgen;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class DepGenConfig
{
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

  @Nullable
  public Map<String, String> getRepositories()
  {
    return repositories;
  }

  public void setRepositories( @Nullable final Map<String, String> repositories )
  {
    this.repositories = repositories;
  }
}
