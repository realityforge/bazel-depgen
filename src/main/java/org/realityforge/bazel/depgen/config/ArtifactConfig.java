package org.realityforge.bazel.depgen.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class ArtifactConfig {
    @Nullable
    private NameStrategy nameStrategy;

    @Nullable
    private NameStrategy repositoryNameStrategy;

    @Nullable
    private String repositoryName;

    @Nullable
    private String coord;

    @Nullable
    private Boolean includeOptional;

    @Nullable
    private Boolean includeSource;

    @Nullable
    private Boolean includeExternalAnnotations;

    @Nullable
    private List<String> excludes;

    @Nullable
    private List<String> repositories;

    @Nullable
    private List<String> visibility;

    @Nullable
    private List<Nature> natures;

    @Nullable
    private JavaConfig java;

    @Nullable
    private J2clConfig j2cl;

    @Nullable
    private PluginConfig plugin;

    @Nullable
    public NameStrategy getNameStrategy() {
        return nameStrategy;
    }

    public void setNameStrategy(final NameStrategy nameStrategy) {
        this.nameStrategy = Objects.requireNonNull(nameStrategy);
    }

    @Nullable
    public NameStrategy getRepositoryNameStrategy() {
        return repositoryNameStrategy;
    }

    public void setRepositoryNameStrategy(final NameStrategy repositoryNameStrategy) {
        this.repositoryNameStrategy = Objects.requireNonNull(repositoryNameStrategy);
    }

    @Nullable
    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(final String repositoryName) {
        this.repositoryName = Objects.requireNonNull(repositoryName);
    }

    @Nullable
    public String getCoord() {
        return coord;
    }

    public void setCoord(final String coord) {
        this.coord = Objects.requireNonNull(coord);
    }

    @Nullable
    public Boolean getIncludeOptional() {
        return includeOptional;
    }

    public void setIncludeOptional(final Boolean includeOptional) {
        this.includeOptional = Objects.requireNonNull(includeOptional);
    }

    @Nullable
    public Boolean getIncludeSource() {
        return includeSource;
    }

    public void setIncludeSource(final Boolean includeSource) {
        this.includeSource = Objects.requireNonNull(includeSource);
    }

    @Nullable
    public Boolean getIncludeExternalAnnotations() {
        return includeExternalAnnotations;
    }

    public void setIncludeExternalAnnotations(final Boolean includeExternalAnnotations) {
        this.includeExternalAnnotations = Objects.requireNonNull(includeExternalAnnotations);
    }

    @Nullable
    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(final List<String> excludes) {
        this.excludes = Objects.requireNonNull(excludes);
    }

    @Nullable
    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(final List<String> repositories) {
        this.repositories = Objects.requireNonNull(repositories);
    }

    @Nullable
    public List<String> getVisibility() {
        return visibility;
    }

    public void setVisibility(final List<String> visibility) {
        this.visibility = Objects.requireNonNull(visibility);
    }

    @Nullable
    public List<Nature> getNatures() {
        return natures;
    }

    public void setNatures(final List<Nature> natures) {
        this.natures = Collections.unmodifiableList(Objects.requireNonNull(natures));
    }

    @Nullable
    public JavaConfig getJava() {
        return java;
    }

    public void setJava(final JavaConfig java) {
        this.java = Objects.requireNonNull(java);
    }

    @Nullable
    public J2clConfig getJ2cl() {
        return j2cl;
    }

    public void setJ2cl(final J2clConfig j2cl) {
        this.j2cl = Objects.requireNonNull(j2cl);
    }

    @Nullable
    public PluginConfig getPlugin() {
        return plugin;
    }

    public void setPlugin(final PluginConfig plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }
}
