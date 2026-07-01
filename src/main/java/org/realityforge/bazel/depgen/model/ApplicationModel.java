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
import org.jspecify.annotations.Nullable;
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

public final class ApplicationModel {
    private final ApplicationConfig _source;

    private final boolean _resetCachedMetadata;

    private final String _configSha256;

    private final OptionsModel _options;

    private final List<ArtifactModel> _artifacts;

    private final List<ArtifactModel> _systemArtifacts;

    private final List<ReplacementModel> _replacements;

    private final List<GlobalExcludeModel> _excludes;

    private final List<RepositoryModel> _repositories;

    public static ApplicationModel load(final ApplicationConfig source, final boolean resetCachedMetadata) {
        final String configSha256 = calculateConfigSha256(source);
        final Path baseDirectory = Objects.requireNonNull(
                source.getConfigLocation().toAbsolutePath().normalize().getParent());
        final OptionsConfig optionsConfig = source.getOptions();
        final OptionsModel optionsModel =
                OptionsModel.parse(baseDirectory, optionsConfig == null ? new OptionsConfig() : optionsConfig);
        final List<ArtifactConfig> artifactsConfig = source.getArtifacts();
        final List<ArtifactModel> artifactModels = null == artifactsConfig
                ? Collections.emptyList()
                : artifactsConfig.stream().map(ArtifactModel::parse).collect(Collectors.toList());
        final List<ReplacementConfig> replacementsConfig = source.getReplacements();
        final List<ReplacementModel> replacements = null == replacementsConfig
                ? Collections.emptyList()
                : replacementsConfig.stream()
                        .map(c -> ReplacementModel.parse(c, optionsModel.getDefaultNature()))
                        .collect(Collectors.toList());

        final var systemArtifacts = new ArrayList<ArtifactModel>();
        if (optionsModel.verifyConfigSha256()
                && artifactModels.stream()
                        .noneMatch(a -> DepGenConfig.getGroupId().equals(a.getGroup())
                                && DepGenConfig.getArtifactId().equals(a.getId()))) {
            final var config = new ArtifactConfig();
            config.setCoord(DepGenConfig.getCoord());
            config.setIncludeSource(false);
            config.setNatures(Collections.singletonList(Nature.Java));
            systemArtifacts.add(ArtifactModel.parse(config));
        }

        final List<ExcludeConfig> excludesConfig = source.getExcludes();
        final List<GlobalExcludeModel> excludes = null == excludesConfig
                ? Collections.emptyList()
                : excludesConfig.stream().map(GlobalExcludeModel::parse).collect(Collectors.toList());
        final List<RepositoryConfig> repositoriesConfig = source.getRepositories();
        final List<RepositoryModel> repositories = null == repositoriesConfig
                ? Collections.singletonList(RepositoryModel.create(
                        ApplicationConfig.MAVEN_CENTRAL_NAME, ApplicationConfig.MAVEN_CENTRAL_URL))
                : repositoriesConfig.stream().map(RepositoryModel::parse).collect(Collectors.toList());

        return new ApplicationModel(
                source,
                resetCachedMetadata,
                configSha256,
                optionsModel,
                artifactModels,
                systemArtifacts,
                replacements,
                excludes,
                repositories);
    }

    static String calculateConfigSha256(final ApplicationConfig config) {
        return HashUtil.sha256(
                DepGenConfig.getVersion().getBytes(StandardCharsets.UTF_8),
                YamlUtil.asYamlString(config).getBytes());
    }

    private ApplicationModel(
            final ApplicationConfig source,
            final boolean resetCachedMetadata,
            final String configSha256,
            final OptionsModel options,
            final List<ArtifactModel> artifacts,
            final List<ArtifactModel> systemArtifacts,
            final List<ReplacementModel> replacements,
            final List<GlobalExcludeModel> excludes,
            final List<RepositoryModel> repositories) {
        _source = Objects.requireNonNull(source);
        _resetCachedMetadata = resetCachedMetadata;
        _configSha256 = Objects.requireNonNull(configSha256);
        _options = Objects.requireNonNull(options);
        _artifacts = Objects.requireNonNull(artifacts);
        _systemArtifacts = Objects.requireNonNull(systemArtifacts);
        _replacements = Objects.requireNonNull(replacements);
        _excludes = Objects.requireNonNull(excludes);
        _repositories = Collections.unmodifiableList(Objects.requireNonNull(repositories));
        ensureOptionCombinationIsValid();
        ensureArtifactRepositoriesAlign();
    }

    private void ensureOptionCombinationIsValid() {
        if (getOptions().supportDependencyOmit()
                && (!getOptions().isRepositoryRuleGenerationInExtensionFile()
                        || !getOptions().isTargetGenerationInExtensionFile())) {
            throw new DepgenValidationException(
                    "The options.supportDependencyOmit property is only supported when both "
                            + "repository rules and targets are generated into the extension file.");
        }
    }

    private void ensureArtifactRepositoriesAlign() {
        final Set<String> repositoryNames =
                getRepositories().stream().map(RepositoryModel::getName).collect(Collectors.toSet());
        for (final ArtifactModel artifact : getArtifacts()) {
            final List<String> repositories = artifact.getRepositories();
            if (!repositories.isEmpty()) {
                for (final String repository : repositories) {
                    if (!repositoryNames.contains(repository)) {
                        final String message = "Artifact '" + artifact.getGroup() + ":" + artifact.getId()
                                + "' declared a repository named '" + repository
                                + "' but no such repository is declared in the repository section. Known repositories "
                                + "include: "
                                + repositoryNames.stream().sorted().collect(Collectors.joining(", "));
                        throw new DepgenValidationException(message);
                    }
                }
            }
        }
    }

    public ApplicationConfig getSource() {
        return _source;
    }

    public boolean shouldResetCachedMetadata() {
        return _resetCachedMetadata;
    }

    public String getConfigSha256() {
        return _configSha256;
    }

    public Path getConfigLocation() {
        return getSource().getConfigLocation();
    }

    public OptionsModel getOptions() {
        return _options;
    }

    public List<RepositoryModel> getRepositories() {
        return _repositories;
    }

    @Nullable
    public RepositoryModel findRepository(final String name) {
        return _repositories.stream()
                .filter(r -> r.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    public RepositoryModel getRepository(final String name) {
        return Objects.requireNonNull(findRepository(name));
    }

    public List<ArtifactModel> getArtifacts() {
        return _artifacts;
    }

    public List<ArtifactModel> getSystemArtifacts() {
        return _systemArtifacts;
    }

    @Nullable
    public ArtifactModel findArtifact(final String groupId, final String artifactId) {
        return findArtifact(m -> m.getGroup().equals(groupId) && m.getId().equals(artifactId));
    }

    @Nullable
    public ArtifactModel findApplicationArtifact(final String groupId, final String artifactId) {
        return findApplicationArtifact(
                m -> m.getGroup().equals(groupId) && m.getId().equals(artifactId));
    }

    @Nullable
    private ArtifactModel findApplicationArtifact(final Predicate<ArtifactModel> predicate) {
        return getArtifacts().stream().filter(predicate).findAny().orElse(null);
    }

    @Nullable
    private ArtifactModel findArtifact(final Predicate<ArtifactModel> predicate) {
        final ArtifactModel artifact = findApplicationArtifact(predicate);
        return null != artifact ? artifact : findSystemArtifact(predicate);
    }

    public boolean isSystemArtifact(final String groupId, final String artifactId) {
        return null
                != findSystemArtifact(
                        m -> m.getGroup().equals(groupId) && m.getId().equals(artifactId));
    }

    @Nullable
    private ArtifactModel findSystemArtifact(final Predicate<ArtifactModel> predicate) {
        return getSystemArtifacts().stream().filter(predicate).findAny().orElse(null);
    }

    public List<ReplacementModel> getReplacements() {
        return _replacements;
    }

    public List<GlobalExcludeModel> getExcludes() {
        return _excludes;
    }

    public boolean isExcluded(final String groupId, final String artifactId) {
        return _excludes.stream()
                .anyMatch(exclude ->
                        exclude.getGroup().equals(groupId) && exclude.getId().equals(artifactId));
    }

    @Nullable
    public ReplacementModel findReplacement(final String groupId, final String artifactId) {
        return findReplacement(m -> m.getGroup().equals(groupId) && m.getId().equals(artifactId));
    }

    public ReplacementModel getReplacement(final String groupId, final String artifactId) {
        return Objects.requireNonNull(findReplacement(groupId, artifactId));
    }

    public String verifyTargetName() {
        return getOptions().getNamePrefix() + "verify_config_sha256";
    }

    @Nullable
    private ReplacementModel findReplacement(final Predicate<ReplacementModel> predicate) {
        return getReplacements().stream().filter(predicate).findAny().orElse(null);
    }
}
