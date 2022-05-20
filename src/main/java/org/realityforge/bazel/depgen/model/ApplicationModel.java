package org.realityforge.bazel.depgen.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.DepGenConfig;
import org.realityforge.bazel.depgen.DepgenValidationException;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.ExcludeConfig;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.realityforge.bazel.depgen.config.ReplacementConfig;
import org.realityforge.bazel.depgen.config.RepositoryConfig;
import org.realityforge.bazel.depgen.util.HashUtil;
import org.realityforge.bazel.depgen.util.YamlUtil;

public final class ApplicationModel
{
  @Nonnull
  private final ApplicationConfig _source;
  private final boolean _resetCachedMetadata;
  @Nonnull
  private final String _configSha256;
  @Nonnull
  private final OptionsModel _options;
  @Nonnull
  private final List<ArtifactModel> _artifacts;
  @Nonnull
  private final List<ArtifactModel> _systemArtifacts;
  @Nonnull
  private final List<ReplacementModel> _replacements;
  @Nonnull
  private final List<GlobalExcludeModel> _excludes;
  @Nonnull
  private final List<RepositoryModel> _repositories;

  @Nonnull
  public static ApplicationModel load( @Nonnull final ApplicationConfig source, final boolean resetCachedMetadata )
  {
    final String configSha256 = calculateConfigSha256( source );
    final Path baseDirectory = source.getConfigLocation().toAbsolutePath().normalize().getParent();
    final OptionsConfig optionsConfig = source.getOptions();
    final OptionsModel optionsModel =
      OptionsModel.parse( baseDirectory, optionsConfig == null ? new OptionsConfig() : optionsConfig );
    final List<ArtifactConfig> artifactsConfig = source.getArtifacts();
    final List<ArtifactModel> artifactModels =
      null == artifactsConfig ?
      Collections.emptyList() :
      artifactsConfig.stream().map( ArtifactModel::parse ).collect( Collectors.toList() );
    final List<ReplacementConfig> replacementsConfig = source.getReplacements();
    final List<ReplacementModel> replacements =
      null == replacementsConfig ?
      Collections.emptyList() :
      replacementsConfig.stream()
        .map( c -> ReplacementModel.parse( c, optionsModel.getDefaultNature() ) )
        .collect( Collectors.toList() );

    final List<ArtifactModel> systemArtifacts = new ArrayList<>();
    if ( optionsModel.verifyConfigSha256() &&
         artifactModels.stream().noneMatch( a -> DepGenConfig.getGroupId().equals( a.getGroup() ) &&
                                                 DepGenConfig.getArtifactId().equals( a.getId() ) ) &&
         replacements.stream().noneMatch( r -> DepGenConfig.getGroupId().equals( r.getGroup() ) &&
                                               DepGenConfig.getArtifactId().equals( r.getId() ) ) )
    {
      final ArtifactConfig config = new ArtifactConfig();
      config.setCoord( DepGenConfig.getCoord() );
      config.setIncludeSource( false );
      config.setNatures( Collections.singletonList( Nature.Java ) );
      systemArtifacts.add( ArtifactModel.parse( config ) );
    }

    final List<ExcludeConfig> excludesConfig = source.getExcludes();
    final List<GlobalExcludeModel> excludes =
      null == excludesConfig ?
      Collections.emptyList() :
      excludesConfig.stream().map( GlobalExcludeModel::parse ).collect( Collectors.toList() );
    final List<RepositoryConfig> repositoriesConfig = source.getRepositories();
    final List<RepositoryModel> repositories =
      null == repositoriesConfig ?
      Collections.singletonList( RepositoryModel.create( ApplicationConfig.MAVEN_CENTRAL_NAME,
                                                         ApplicationConfig.MAVEN_CENTRAL_URL ) ) :
      repositoriesConfig.stream().map( RepositoryModel::parse ).collect( Collectors.toList() );

    return new ApplicationModel( source,
                                 resetCachedMetadata,
                                 configSha256,
                                 optionsModel,
                                 artifactModels,
                                 systemArtifacts,
                                 replacements,
                                 excludes,
                                 repositories );
  }

  @Nonnull
  static String calculateConfigSha256( @Nonnull final ApplicationConfig config )
  {
    return HashUtil.sha256( DepGenConfig.getVersion().getBytes( StandardCharsets.UTF_8 ),
                            YamlUtil.asYamlString( config ).getBytes() );
  }

  private ApplicationModel( @Nonnull final ApplicationConfig source,
                            final boolean resetCachedMetadata,
                            @Nonnull final String configSha256,
                            @Nonnull final OptionsModel options,
                            @Nonnull final List<ArtifactModel> artifacts,
                            @Nonnull final List<ArtifactModel> systemArtifacts,
                            @Nonnull final List<ReplacementModel> replacements,
                            @Nonnull final List<GlobalExcludeModel> excludes,
                            @Nonnull final List<RepositoryModel> repositories )
  {
    _source = Objects.requireNonNull( source );
    _resetCachedMetadata = resetCachedMetadata;
    _configSha256 = Objects.requireNonNull( configSha256 );
    _options = Objects.requireNonNull( options );
    _artifacts = Objects.requireNonNull( artifacts );
    _systemArtifacts = Objects.requireNonNull( systemArtifacts );
    _replacements = Objects.requireNonNull( replacements );
    _excludes = Objects.requireNonNull( excludes );
    _repositories = Collections.unmodifiableList( Objects.requireNonNull( repositories ) );
    ensureArtifactRepositoriesAlign();
  }

  private void ensureArtifactRepositoriesAlign()
  {
    final Set<String> repositoryNames =
      getRepositories().stream().map( RepositoryModel::getName ).collect( Collectors.toSet() );
    for ( final ArtifactModel artifact : getArtifacts() )
    {
      final List<String> repositories = artifact.getRepositories();
      if ( !repositories.isEmpty() )
      {
        for ( final String repository : repositories )
        {
          if ( !repositoryNames.contains( repository ) )
          {
            final String message =
              "Artifact '" + artifact.getGroup() + ":" + artifact.getId() + "' declared a repository named '" +
              repository + "' but no such repository is declared in the repository section. Known repositories " +
              "include: " + repositoryNames.stream().sorted().collect( Collectors.joining( ", " ) );
            throw new DepgenValidationException( message );
          }
        }
      }
    }
  }

  @Nonnull
  public ApplicationConfig getSource()
  {
    return _source;
  }

  public boolean shouldResetCachedMetadata()
  {
    return _resetCachedMetadata;
  }

  @Nonnull
  public String getConfigSha256()
  {
    return _configSha256;
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
  public List<RepositoryModel> getRepositories()
  {
    return _repositories;
  }

  @Nullable
  public RepositoryModel findRepository( @Nonnull final String name )
  {
    return _repositories.stream().filter( r -> r.getName().equals( name ) ).findAny().orElse( null );
  }

  @Nonnull
  public RepositoryModel getRepository( @Nonnull final String name )
  {
    return Objects.requireNonNull( findRepository( name ) );
  }

  @Nonnull
  public List<ArtifactModel> getArtifacts()
  {
    return _artifacts;
  }

  @Nonnull
  public List<ArtifactModel> getSystemArtifacts()
  {
    return _systemArtifacts;
  }

  @Nullable
  public ArtifactModel findArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findArtifact( m -> m.getGroup().equals( groupId ) && m.getId().equals( artifactId ) );
  }

  @Nullable
  public ArtifactModel findApplicationArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findApplicationArtifact( m -> m.getGroup().equals( groupId ) && m.getId().equals( artifactId ) );
  }

  @Nullable
  private ArtifactModel findApplicationArtifact( @Nonnull final Predicate<ArtifactModel> predicate )
  {
    return getArtifacts()
      .stream()
      .filter( predicate )
      .findAny()
      .orElse( null );
  }

  @Nullable
  private ArtifactModel findArtifact( @Nonnull final Predicate<ArtifactModel> predicate )
  {
    final ArtifactModel artifact = findApplicationArtifact( predicate );
    return null != artifact ? artifact : findSystemArtifact( predicate );
  }

  public boolean isSystemArtifact( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return null != findSystemArtifact( m -> m.getGroup().equals( groupId ) && m.getId().equals( artifactId ) );
  }

  @Nullable
  private ArtifactModel findSystemArtifact( @Nonnull final Predicate<ArtifactModel> predicate )
  {
    return getSystemArtifacts()
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

  @Nonnull
  public List<GlobalExcludeModel> getExcludes()
  {
    return _excludes;
  }

  public boolean isExcluded( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return _excludes
      .stream()
      .anyMatch( exclude -> exclude.getGroup().equals( groupId ) && exclude.getId().equals( artifactId ) );
  }

  @Nullable
  public ReplacementModel findReplacement( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return findReplacement( m -> m.getGroup().equals( groupId ) && m.getId().equals( artifactId ) );
  }

  @Nonnull
  public ReplacementModel getReplacement( @Nonnull final String groupId, @Nonnull final String artifactId )
  {
    return Objects.requireNonNull( findReplacement( groupId, artifactId ) );
  }

  @Nonnull
  public String verifyTargetName()
  {
    return getOptions().getNamePrefix() + "verify_config_sha256";
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
