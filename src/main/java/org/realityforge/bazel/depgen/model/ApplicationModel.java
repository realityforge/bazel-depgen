package org.realityforge.bazel.depgen.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.ApplicationConfig;

public final class ApplicationModel
{
  @Nonnull
  private final ApplicationConfig _source;
  @Nonnull
  private final List<ArtifactModel> _artifacts;
  @Nonnull
  private final OptionsModel _options;

  @Nonnull
  public static ApplicationModel parse( @Nonnull final ApplicationConfig source )
  {
    final List<ArtifactModel> artifactModels =
      source.getArtifacts().stream().flatMap( c -> ArtifactModel.parse( c ).stream() ).collect( Collectors.toList() );
    return new ApplicationModel( source, artifactModels, OptionsModel.parse( source.getOptions() ) );
  }

  private ApplicationModel( @Nonnull final ApplicationConfig source,
                            @Nonnull final List<ArtifactModel> artifacts,
                            @Nonnull final OptionsModel options )
  {
    _source = Objects.requireNonNull( source );
    _artifacts = Objects.requireNonNull( artifacts );
    _options = Objects.requireNonNull( options );
  }

  @Nonnull
  public ApplicationConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public Map<String, String> getRepositories()
  {
    return Collections.unmodifiableMap( _source.getRepositories() );
  }

  @Nonnull
  public List<ArtifactModel> getArtifacts()
  {
    return _artifacts;
  }

  @Nonnull
  public OptionsModel getOptions()
  {
    return _options;
  }
}
