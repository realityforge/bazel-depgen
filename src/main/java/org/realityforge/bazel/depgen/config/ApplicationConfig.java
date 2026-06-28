package org.realityforge.bazel.depgen.config;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class ApplicationConfig {
    @NonNull
    public static final String DEFAULT_MODULE = "thirdparty";

    @NonNull
    public static final String FILENAME = "dependencies.yml";

    @NonNull
    public static final String MAVEN_CENTRAL_NAME = "central";

    @NonNull
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

    @NonNull
    public static ApplicationConfig load(@NonNull final Path path) throws Exception {
        final var yaml = new Yaml(new Constructor(ApplicationConfig.class));
        final ApplicationConfig config = yaml.load(new FileReader(path.toFile()));
        final ApplicationConfig applicationConfig = null == config ? new ApplicationConfig() : config;
        applicationConfig.setConfigLocation(path);
        return applicationConfig;
    }

    private void setConfigLocation(@NonNull final Path configLocation) {
        _configLocation = Objects.requireNonNull(configLocation);
    }

    @NonNull
    public Path getConfigLocation() {
        return Objects.requireNonNull(_configLocation);
    }

    @Nullable
    public OptionsConfig getOptions() {
        return options;
    }

    public void setOptions(@NonNull final OptionsConfig options) {
        this.options = Objects.requireNonNull(options);
    }

    @Nullable
    public List<RepositoryConfig> getRepositories() {
        return repositories;
    }

    public void setRepositories(@NonNull final List<RepositoryConfig> repositories) {
        this.repositories = Objects.requireNonNull(repositories);
    }

    @Nullable
    public List<ArtifactConfig> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(@NonNull final List<ArtifactConfig> artifacts) {
        this.artifacts = Objects.requireNonNull(artifacts);
    }

    @Nullable
    public List<ReplacementConfig> getReplacements() {
        return replacements;
    }

    public void setReplacements(@NonNull final List<ReplacementConfig> replacements) {
        this.replacements = Objects.requireNonNull(replacements);
    }

    @Nullable
    public List<ExcludeConfig> getExcludes() {
        return excludes;
    }

    public void setExcludes(@NonNull final List<ExcludeConfig> excludes) {
        this.excludes = Objects.requireNonNull(excludes);
    }
}
