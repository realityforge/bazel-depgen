package org.realityforge.bazel.depgen.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.ApplicationConfig;

public final class ApplicationModel
{
  @Nonnull
  private final ApplicationConfig _source;
  @Nonnull
  private final OptionsModel _options;
  @Nonnull
  private final List<ArtifactModel> _artifacts;
  @Nonnull
  private final List<ReplacementModel> _replacements;
  @Nonnull
  private final Map<String, String> _repositories;

  @Nonnull
  public static ApplicationModel parse( @Nonnull final ApplicationConfig source )
  {
    final Path baseDirectory = source.getConfigLocation().toAbsolutePath().normalize().getParent();
    final OptionsModel optionsModel = OptionsModel.parse( baseDirectory, source.getOptions() );
    final List<ArtifactModel> artifactModels =
      source.getArtifacts().stream().flatMap( c -> ArtifactModel.parse( c ).stream() ).collect( Collectors.toList() );
    final List<ReplacementModel> replacements =
      source.getReplacements().stream().map( ReplacementModel::parse ).collect( Collectors.toList() );
    final Map<String, String> sourceRepositories = source.getRepositories();
    final Map<String, String> repositories =
      sourceRepositories.isEmpty() ?
      Collections.singletonMap( ApplicationConfig.MAVEN_CENTRAL_ID, ApplicationConfig.MAVEN_CENTRAL_URL ) :
      sourceRepositories;

    return new ApplicationModel( source, optionsModel, artifactModels, replacements, repositories );
  }

  private ApplicationModel( @Nonnull final ApplicationConfig source,
                            @Nonnull final OptionsModel options,
                            @Nonnull final List<ArtifactModel> artifacts,
                            @Nonnull final List<ReplacementModel> replacements,
                            @Nonnull final Map<String, String> repositories )
  {
    _source = Objects.requireNonNull( source );
    _options = Objects.requireNonNull( options );
    _artifacts = Objects.requireNonNull( artifacts );
    _replacements = Objects.requireNonNull( replacements );
    _repositories = Collections.unmodifiableMap( Objects.requireNonNull( repositories ) );
  }

  @Nonnull
  public ApplicationConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public Path getConfigLocation()
  {
    return getSource().getConfigLocation();
  }

  @Nonnull
  public OptionsModel getOptions()
  {
    return _options;
  }

  @Nonnull
  public Map<String, String> getRepositories()
  {
    return _repositories;
  }

  @Nonnull
  public List<ArtifactModel> getArtifacts()
  {
    return _artifacts;
  }

  @Nullable
  public ArtifactModel findArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findArtifact( m -> m.getGroup().equals( groupId ) && m.getId().equals( artifactId ) );
  }

  @Nullable
  private ArtifactModel findArtifact( @Nonnull final Predicate<ArtifactModel> predicate )
  {
    return getArtifacts()
      .stream()
      .filter( predicate )
      .findAny()
      .orElse( null );
  }

  @Nonnull
  public List<ReplacementModel> getReplacements()
  {
    return _replacements;
  }

  @Nullable
  public ReplacementModel findReplacement( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findReplacement( m -> m.getGroup().equals( groupId ) && m.getId().equals( artifactId ) );
  }

  @Nullable
  private ReplacementModel findReplacement( @Nonnull final Predicate<ReplacementModel> predicate )
  {
    return getReplacements()
      .stream()
      .filter( predicate )
      .findAny()
      .orElse( null );
  }
}
