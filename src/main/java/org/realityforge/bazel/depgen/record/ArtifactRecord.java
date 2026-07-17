package org.realityforge.bazel.depgen.record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.DepgenValidationException;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.J2clMode;
import org.realityforge.bazel.depgen.config.JspecifyMode;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.PluginConfig;
import org.realityforge.bazel.depgen.model.ArtifactModel;
import org.realityforge.bazel.depgen.model.ReplacementModel;
import org.realityforge.bazel.depgen.model.ReplacementTargetModel;
import org.realityforge.bazel.depgen.util.ArtifactUtil;
import org.realityforge.bazel.depgen.util.BazelUtil;
import org.realityforge.bazel.depgen.util.StarlarkOutput;

public final class ArtifactRecord {
    private static final String JSPECIFY_GROUP_ID = "org.jspecify";
    private static final String JSPECIFY_ARTIFACT_ID = "jspecify";
    private static final String JSPECIFY_SUPPORT_ATTRIBUTE =
            "experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage";
    /**
     * The suffix applied to the name of target that imports artifact for use when defining plugins.
     */
    private static final String PLUGIN_LIBRARY_SUFFIX = "__plugin_library";
    /**
     * The suffix applied to every compile plugin.
     */
    private static final String PLUGIN_SUFFIX = "__plugin";

    private final ApplicationRecord _application;

    private final DependencyNode _node;

    @Nullable
    private final ArtifactModel _artifactModel;

    @Nullable
    private final ReplacementModel _replacementModel;
    // Natures is non-null if _artifactModel is null
    @Nullable
    private final List<Nature> _natures;

    @Nullable
    private final String _sha256;

    @Nullable
    private final List<String> _urls;

    @Nullable
    private final String _sourceSha256;

    @Nullable
    private final List<String> _sourceUrls;

    @Nullable
    private final String _externalAnnotationSha256;

    @Nullable
    private final List<String> _externalAnnotationUrls;

    @Nullable
    private final List<String> _processors;

    @Nullable
    private List<ArtifactRecord> _depsCache;

    @Nullable
    private List<ArtifactRecord> _reverseDepsCache;

    @Nullable
    private List<ArtifactRecord> _runtimeDepsCache;

    @Nullable
    private List<ArtifactRecord> _reverseRuntimeDepsCache;

    ArtifactRecord(
            final ApplicationRecord application,
            final DependencyNode node,
            @Nullable final String sha256,
            @Nullable final List<String> urls,
            @Nullable final String sourceSha256,
            @Nullable final List<String> sourceUrls,
            @Nullable final String externalAnnotationSha256,
            @Nullable final List<String> externalAnnotationUrls,
            @Nullable final List<String> processors,
            @Nullable final ArtifactModel artifactModel,
            @Nullable final ReplacementModel replacementModel) {
        assert (null == sha256 && null == urls) || (null != sha256 && null != urls && !urls.isEmpty());
        assert (null == sourceSha256 && null == sourceUrls)
                || (null != sourceSha256 && null != sourceUrls && !sourceUrls.isEmpty());
        assert (null == externalAnnotationSha256 && null == externalAnnotationUrls)
                || (null != externalAnnotationSha256
                        && null != externalAnnotationUrls
                        && !externalAnnotationUrls.isEmpty());
        _application = Objects.requireNonNull(application);
        _node = Objects.requireNonNull(node);
        _natures = null == artifactModel ? new ArrayList<>() : null;
        _sha256 = Objects.requireNonNull(sha256);
        _urls = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(urls)));
        _artifactModel = artifactModel;
        _replacementModel = replacementModel;
        _sourceSha256 = sourceSha256;
        _sourceUrls = null != sourceUrls ? Collections.unmodifiableList(new ArrayList<>(sourceUrls)) : null;
        _externalAnnotationSha256 = externalAnnotationSha256;
        _externalAnnotationUrls = null != externalAnnotationUrls
                ? Collections.unmodifiableList(new ArrayList<>(externalAnnotationUrls))
                : null;
        _processors = null != processors ? Collections.unmodifiableList(new ArrayList<>(processors)) : null;
        if (null != _natures && null != _processors && !_processors.isEmpty()) {
            addNature(Nature.Plugin);
        }
    }

    void validate() {
        final var natures = getNatures();
        if (null != _artifactModel) {
            final var java = _artifactModel.getSource().getJava();
            if (null != java) {
                if (!natures.contains(Nature.Java)) {
                    final var message = "Artifact '" + getArtifact()
                            + "' has specified 'java' configuration but does not specify the Java nature.";
                    throw new DepgenValidationException(message);
                }
            }
            final var j2cl = _artifactModel.getSource().getJ2cl();
            if (null != j2cl) {
                if (!natures.contains(Nature.J2cl)) {
                    final var message = "Artifact '" + getArtifact()
                            + "' has specified 'j2cl' configuration but does not specify the J2cl nature.";
                    throw new DepgenValidationException(message);
                } else if (null != j2cl.getSuppress() && J2clMode.Import == j2cl.getMode()) {
                    final var message = "Artifact '" + getArtifact()
                            + "' has specified 'j2cl.suppress' configuration but specified "
                            + "'j2cl.mode = Import' which is incompatible with 'j2cl.suppress'.";
                    throw new DepgenValidationException(message);
                }
            }
            final PluginConfig plugin = _artifactModel.getSource().getPlugin();
            if (null != plugin) {
                if (!natures.contains(Nature.Plugin)) {
                    final var message = "Artifact '" + getArtifact()
                            + "' has specified 'plugin' configuration but does not specify "
                            + "the Plugin nature nor does it contain any annotation processors.";
                    throw new DepgenValidationException(message);
                } else if (null != plugin.getGeneratesApi() && (null == _processors || _processors.isEmpty())) {
                    final var message = "Artifact '" + getArtifact()
                            + "' has specified 'plugin.generatesApi' configuration but does not "
                            + "contain any annotation processors.";
                    throw new DepgenValidationException(message);
                }
            }
        }
        if (natures.contains(Nature.J2cl) && !isNatureReplaced(Nature.J2cl)) {
            final J2clMode j2clMode = getJ2clMode();
            final JspecifyMode jspecifyMode = getJspecifyMode();
            if (J2clMode.Import == j2clMode && JspecifyMode.Disable != jspecifyMode) {
                final var message = "Artifact '" + getArtifact()
                        + "' resolves 'j2cl.jspecifyMode' to '" + jspecifyMode
                        + "' but specifies 'j2cl.mode = Import'. JSpecify support is only available for J2cl"
                        + " libraries.";
                throw new DepgenValidationException(message);
            } else if (J2clMode.Library == j2clMode
                    && JspecifyMode.Enable == jspecifyMode
                    && !hasJspecifyDependency()) {
                final var message = "Artifact '" + getArtifact()
                        + "' resolves 'j2cl.jspecifyMode' to 'Enable' but does not have a direct compile dependency on "
                        + "'org.jspecify:jspecify:jar' with no classifier.";
                throw new DepgenValidationException(message);
            }
            if (J2clMode.Library == j2clMode) {
                if (null != _artifactModel
                        && !_artifactModel.includeSource(
                                _application.getSource().getOptions().includeSource())) {
                    final var message = "Artifact '" + getArtifact()
                            + "' has specified J2cl nature but the 'includeSource' configuration resolves to false.";
                    throw new DepgenValidationException(message);
                }
                if (null == _sourceSha256) {
                    final var message =
                            "Unable to locate the sources classifier artifact for the artifact '" + getArtifact()
                                    + "' but the artifact has the J2cl nature which requires that sources be present."
                                    + "\n\n"
                                    + _application.formatDependencyPathTo(this);
                    throw new DepgenValidationException(message);
                }
            }
        }
        if (null == _sourceSha256 && shouldIncludeSource() && emitsTargets()) {
            final var message =
                    "Unable to locate source for artifact '" + getArtifact() + "'. Specify the 'includeSource' "
                            + "configuration property as 'false' in the artifacts configuration."
                            + "\n\n"
                            + _application.formatDependencyPathTo(this);
            throw new DepgenValidationException(message);
        }
    }

    public String getKey() {
        final var artifact = getArtifact();
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    String getSymbol() {
        return getTargetSymbol();
    }

    private String getTargetSymbol() {
        final var artifact = getArtifact();
        final NameStrategy nameStrategy = getNameStrategy();
        if (NameStrategy.ArtifactId == nameStrategy) {
            return getNamePrefix() + BazelUtil.cleanNamePart(artifact.getArtifactId());
        } else {
            final var sb = new StringBuilder();
            sb.append(getNamePrefix());
            sb.append(BazelUtil.cleanNamePart(artifact.getGroupId()));
            sb.append(BazelUtil.COMPONENT_SEPARATOR);
            sb.append(BazelUtil.cleanNamePart(artifact.getArtifactId()));
            if (NameStrategy.GroupIdAndArtifactIdAndVersion == nameStrategy) {
                sb.append(BazelUtil.COMPONENT_SEPARATOR);
                sb.append(BazelUtil.cleanNamePart(artifact.getVersion()));
            }
            return sb.toString();
        }
    }

    private String getRepository() {
        return getRepositoryBaseName();
    }

    private String getQualifiedBinaryLabel() {
        return "@" + getRepository() + "//file";
    }

    private String getSourceRepository() {
        return getRepository() + "__sources";
    }

    private String getExternalAnnotationsRepository() {
        return getRepository() + "__annotations";
    }

    private String getQualifiedSourcesLabel() {
        return "@" + getSourceRepository() + "//file";
    }

    String getBaseName() {
        return getRepositoryBaseName();
    }

    String getRepositoryBaseName() {
        final var repositoryName =
                null != _artifactModel ? _artifactModel.getSource().getRepositoryName() : null;
        if (null != repositoryName) {
            return repositoryName;
        } else {
            final var artifact = getArtifact();
            final NameStrategy nameStrategy = getRepositoryNameStrategy();
            if (NameStrategy.ArtifactId == nameStrategy) {
                return getNamePrefix() + BazelUtil.cleanNamePart(artifact.getArtifactId());
            } else {
                final var sb = new StringBuilder();
                sb.append(getNamePrefix());
                sb.append(BazelUtil.cleanNamePart(artifact.getGroupId()));
                sb.append(BazelUtil.COMPONENT_SEPARATOR);
                sb.append(BazelUtil.cleanNamePart(artifact.getArtifactId()));
                if (NameStrategy.GroupIdAndArtifactIdAndVersion == nameStrategy) {
                    sb.append(BazelUtil.COMPONENT_SEPARATOR);
                    sb.append(BazelUtil.cleanNamePart(artifact.getVersion()));
                }
                return sb.toString();
            }
        }
    }

    String getLabel(final Nature nature) {
        final var target = findReplacementTarget(nature);
        return null != target ? target : ":" + getName(nature);
    }

    String getName(final Nature nature) {
        String name = null;
        if (null != _artifactModel) {
            final ArtifactConfig source = _artifactModel.getSource();
            if (Nature.Java == nature) {
                final var config = source.getJava();
                name = null != config ? config.getName() : null;
            } else if (Nature.J2cl == nature) {
                final var config = source.getJ2cl();
                name = null != config ? config.getName() : null;
            } else if (Nature.Plugin == nature) {
                final PluginConfig config = source.getPlugin();
                name = null != config ? config.getName() : null;
            }
        }
        return null != name ? name : getSymbol() + deriveSuffix(nature);
    }

    NameStrategy getNameStrategy() {
        final NameStrategy nameStrategy =
                null != _artifactModel ? _artifactModel.getSource().getNameStrategy() : null;
        return null == nameStrategy ? _application.getSource().getOptions().getNameStrategy() : nameStrategy;
    }

    NameStrategy getRepositoryNameStrategy() {
        final NameStrategy nameStrategy =
                null != _artifactModel ? _artifactModel.getSource().getRepositoryNameStrategy() : null;
        return null == nameStrategy ? _application.getSource().getOptions().getRepositoryNameStrategy() : nameStrategy;
    }

    List<Nature> getNatures() {
        final var natures = new ArrayList<Nature>();
        if (null == _artifactModel) {
            if (null != _natures && !_natures.isEmpty()) {
                natures.addAll(_natures);
            } else {
                natures.add(getDefaultNature());
            }
        } else {
            natures.addAll(_artifactModel.getNatures(getDefaultNature()));
        }
        if (null != _replacementModel) {
            for (final ReplacementTargetModel target : _replacementModel.getTargets()) {
                if (!natures.contains(target.getNature())) {
                    natures.add(target.getNature());
                }
            }
        }
        return Collections.unmodifiableList(natures);
    }

    @SuppressWarnings("SameParameterValue")
    boolean addNature(final Nature nature) {
        assert null == _artifactModel && null != _natures;
        final var natures = Objects.requireNonNull(_natures);
        if (!natures.contains(nature)) {
            natures.add(nature);
            return true;
        } else {
            return false;
        }
    }

    boolean isNatureReplaced(final Nature nature) {
        return null != findReplacementTarget(nature);
    }

    @Nullable
    String findReplacementTarget(final Nature nature) {
        return null == _replacementModel ? null : _replacementModel.findTarget(nature);
    }

    boolean emitsTargets() {
        return !getEmittedPublicTargetNames().isEmpty()
                || !getEmittedPrivateTargetNames().isEmpty();
    }

    boolean emitsRepositoryRules() {
        return !getEmittedRepositoryNames().isEmpty();
    }

    boolean generatesApi() {
        if (null == _artifactModel) {
            return true;
        } else {
            final PluginConfig plugin = _artifactModel.getSource().getPlugin();
            final Boolean generatesApi = null == plugin ? null : plugin.getGeneratesApi();
            return null == generatesApi || generatesApi;
        }
    }

    private String getNamePrefix() {
        return BazelUtil.cleanNamePart(_application.getSource().getOptions().getNamePrefix());
    }

    String getMavenCoordinatesBazelTag() {
        final var artifact = getArtifact();
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    public org.eclipse.aether.artifact.Artifact getArtifact() {
        return _node.getArtifact();
    }

    LinkedHashMap<String, String> getEmittedRepositoryNames() {
        final var names = new LinkedHashMap<String, String>();
        final var artifactName = "artifact '" + getArtifact() + "'";
        if (emitsBinaryRepositoryRule()) {
            names.put(getRepository(), artifactName + " binary repository");
        }
        if (null != getSourceSha256() && emitsSourceRepositoryRule()) {
            names.put(getSourceRepository(), artifactName + " sources repository");
        }
        if (null != getExternalAnnotationSha256() && emitsAnnotationsRepositoryRule()) {
            names.put(getExternalAnnotationsRepository(), artifactName + " annotations repository");
        }
        return names;
    }

    LinkedHashMap<String, String> getEmittedPublicTargetNames() {
        final var names = new LinkedHashMap<String, String>();
        final var artifactName = "artifact '" + getArtifact() + "'";
        for (final var nature : getNatures()) {
            if (shouldEmitNatureTarget(nature)) {
                names.put(getName(nature), artifactName + " public " + nature + " target");
            }
        }
        return names;
    }

    LinkedHashMap<String, String> getEmittedPrivateTargetNames() {
        final var names = new LinkedHashMap<String, String>();
        if (shouldEmitNatureTarget(Nature.Plugin)) {
            final var artifactName = "artifact '" + getArtifact() + "'";
            names.put(getName(Nature.Java) + PLUGIN_LIBRARY_SUFFIX, artifactName + " private plugin library target");
            final var processors = getProcessors();
            if (null == processors) {
                names.put(pluginName(null), artifactName + " private plugin target");
            } else {
                for (final var processor : processors) {
                    names.put(
                            pluginName(processor),
                            artifactName + " private plugin target for processor '" + processor + "'");
                }
            }
        }
        return names;
    }

    DependencyNode getNode() {
        return _node;
    }

    /**
     * Return the sha256 of the specified artifact.
     * This MUST be null when {@link #getReplacementModel()} is non-null otherwise it must be non-null.
     *
     * @return the sha256 of artifact or null if {@link #getReplacementModel()} returns a non-null value.
     */
    @Nullable
    public String getSha256() {
        return _sha256;
    }

    /**
     * Return the urls that the artifact can be downloaded from.
     * This MUST be null when {@link #getReplacementModel()} is non-null otherwise it must be non-null and non-empty.
     *
     * @return the urls.
     */
    @Nullable
    List<String> getUrls() {
        return _urls;
    }

    /**
     * Return the sha256 of the source artifact associated with the artifact.
     * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
     * if {@link #getSourceUrls()} returns a non-null value.
     *
     * @return the sha256 of the source artifact associated with the artifact.
     */
    @Nullable
    public String getSourceSha256() {
        return _sourceSha256;
    }

    /**
     * Return the urls that the source artifact can be downloaded from.
     * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
     * if {@link #getSourceSha256()} returns a non-null value.
     *
     * @return the urls.
     */
    @Nullable
    List<String> getSourceUrls() {
        return _sourceUrls;
    }

    /**
     * Return the sha256 of the external annotations artifact associated with the artifact.
     * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
     * if {@link #getExternalAnnotationUrls()} returns a non-null value.
     *
     * @return the sha256 of the external annotations artifact associated with the artifact.
     */
    @Nullable
    public String getExternalAnnotationSha256() {
        return _externalAnnotationSha256;
    }

    /**
     * Return the urls that the external annotations artifact can be downloaded from.
     * This MAY be non-null when {@link #getReplacementModel()} is non-null and MUST be non-null
     * if {@link #getExternalAnnotationSha256()} returns a non-null value.
     *
     * @return the urls.
     */
    @Nullable
    public List<String> getExternalAnnotationUrls() {
        return _externalAnnotationUrls;
    }

    /**
     * Return the model that represents the configuration supplied by the user.
     * This method may return null if the artifact was not declared by the user but
     * is a dependency of another artifact or if {@link #getReplacementModel()} returns
     * a non-null value.
     *
     * @return the model if any.
     */
    @Nullable
    ArtifactModel getArtifactModel() {
        return _artifactModel;
    }

    /**
     * Return the replacement for artifact as supplied by the user.
     * This method may return null if the artifact was not as a replacement.
     *
     * @return the replacement if any.
     */
    @Nullable
    public ReplacementModel getReplacementModel() {
        return _replacementModel;
    }

    /**
     * Return the name of any annotation processors that this artifact defines.
     *
     * @return the name of any annotation processors that this artifact defines.
     */
    @Nullable
    List<String> getProcessors() {
        return _processors;
    }

    List<ArtifactRecord> getDeps() {
        if (null == _depsCache) {
            _depsCache = collectArtifacts(_node.getChildren().stream()
                    .filter(c -> !_application
                            .getSource()
                            .isExcluded(
                                    c.getDependency().getArtifact().getGroupId(),
                                    c.getDependency().getArtifact().getArtifactId()))
                    .filter(c -> shouldIncludeDependency(Artifact.SCOPE_COMPILE, c))
                    .map(c -> _application.getArtifact(
                            c.getDependency().getArtifact().getGroupId(),
                            c.getDependency().getArtifact().getArtifactId())));
        }
        return _depsCache;
    }

    private List<ArtifactRecord> getDeps(final Nature nature) {
        return isNatureReplaced(nature) ? Collections.emptyList() : getDeps();
    }

    List<ArtifactRecord> getReverseDeps() {
        if (null == _reverseDepsCache) {
            _reverseDepsCache = collectArtifacts(
                    _application.getArtifacts().stream().filter(a -> a.getDeps().contains(this)));
        }
        return _reverseDepsCache;
    }

    List<ArtifactRecord> getRuntimeDeps() {
        if (null == _runtimeDepsCache) {
            _runtimeDepsCache = collectArtifacts(_node.getChildren().stream()
                    .filter(c -> shouldIncludeDependency(Artifact.SCOPE_RUNTIME, c))
                    .filter(c -> !_application
                            .getSource()
                            .isExcluded(
                                    c.getDependency().getArtifact().getGroupId(),
                                    c.getDependency().getArtifact().getArtifactId()))
                    .map(c -> _application.getArtifact(
                            c.getDependency().getArtifact().getGroupId(),
                            c.getDependency().getArtifact().getArtifactId())));
        }
        return _runtimeDepsCache;
    }

    private List<ArtifactRecord> getRuntimeDeps(final Nature nature) {
        return isNatureReplaced(nature) ? Collections.emptyList() : getRuntimeDeps();
    }

    List<ArtifactRecord> getReverseRuntimeDeps() {
        if (null == _reverseRuntimeDepsCache) {
            _reverseRuntimeDepsCache = collectArtifacts(_application.getArtifacts().stream()
                    .filter(a -> a.getRuntimeDeps().contains(this)));
        }
        return _reverseRuntimeDepsCache;
    }

    boolean shouldExportDeps() {
        return null != _artifactModel
                && _artifactModel.exportDeps(
                        _application.getSource().getOptions().exportDeps());
    }

    private boolean shouldIncludeSource() {
        return null != _artifactModel
                ? _artifactModel.includeSource(
                        _application.getSource().getOptions().includeSource())
                : _application.getSource().getOptions().includeSource();
    }

    boolean shouldEmitNatureTarget(final Nature nature) {
        return getNatures().contains(nature) && !isNatureReplaced(nature);
    }

    private boolean emitsBinaryRepository() {
        return shouldEmitNatureTarget(Nature.Java)
                || shouldEmitNatureTarget(Nature.Plugin)
                || (shouldEmitNatureTarget(Nature.J2cl) && J2clMode.Import == getJ2clMode());
    }

    boolean emitsBinaryRepositoryRule() {
        return emitsBinaryRepository();
    }

    boolean emitsSourceRepositoryRule() {
        return emitsBinaryRepository() || shouldEmitNatureTarget(Nature.J2cl);
    }

    boolean emitsAnnotationsRepositoryRule() {
        return emitsTargets();
    }

    private List<ArtifactRecord> collectArtifacts(final Stream<ArtifactRecord> stream) {
        return stream.distinct()
                .sorted(Comparator.comparing(ArtifactRecord::getKey))
                .toList();
    }

    private boolean shouldIncludeDependency(final String scope, final DependencyNode c) {
        final boolean includeOptional = null != _artifactModel && _artifactModel.includeOptional();
        return (includeOptional || !c.getDependency().isOptional())
                && scope.equals(c.getDependency().getScope());
    }

    J2clMode getJ2clMode() {
        final var j2clConfig =
                null != _artifactModel ? _artifactModel.getSource().getJ2cl() : null;
        return null != j2clConfig && null != j2clConfig.getMode() ? j2clConfig.getMode() : J2clMode.Library;
    }

    private JspecifyMode getJspecifyMode() {
        final JspecifyMode defaultMode = _application.getSource().getOptions().jspecifyMode();
        return null != _artifactModel ? _artifactModel.jspecifyMode(defaultMode) : defaultMode;
    }

    private boolean hasJspecifyDependency() {
        return _node.getChildren().stream()
                .filter(c -> shouldIncludeDependency(Artifact.SCOPE_COMPILE, c))
                .filter(c -> !_application
                        .getSource()
                        .isExcluded(
                                c.getDependency().getArtifact().getGroupId(),
                                c.getDependency().getArtifact().getArtifactId()))
                .map(c -> c.getDependency().getArtifact())
                .anyMatch(a -> JSPECIFY_GROUP_ID.equals(a.getGroupId())
                        && JSPECIFY_ARTIFACT_ID.equals(a.getArtifactId())
                        && "jar".equals(a.getExtension())
                        && a.getClassifier().isEmpty());
    }

    private boolean shouldEnableJspecifySupport() {
        return JspecifyMode.Disable != getJspecifyMode() && hasJspecifyDependency();
    }

    boolean shouldMatch(final String groupId, final String artifactId) {
        final DependencyNode node = getNode();
        final var artifact = node.getDependency().getArtifact();
        return groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId());
    }

    void emitJavaImport(final StarlarkOutput output, final String nameSuffix) throws IOException {
        emitJavaImport(output, nameSuffix, Nature.Java);
    }

    void emitJavaImport(final StarlarkOutput output, final String nameSuffix, final Nature dependencyNature)
            throws IOException {
        // nameSuffix is still used so that plugins base library can be satisfied
        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", "\"" + getName(Nature.Java) + nameSuffix + "\"");
        arguments.put("jars", Collections.singletonList(asString(getQualifiedBinaryLabel())));
        if (null != getSourceSha256()) {
            arguments.put("srcjar", asString(getQualifiedSourcesLabel()));
        }
        arguments.put("tags", Collections.singletonList("\"maven_coordinates=" + getMavenCoordinatesBazelTag() + "\""));
        final ArtifactModel artifactModel = getArtifactModel();
        if (null != artifactModel) {
            final var visibility = artifactModel.getVisibility();
            if (!visibility.isEmpty()) {
                arguments.put(
                        "visibility", visibility.stream().map(this::asString).collect(Collectors.toList()));
            }
        } else {
            arguments.put("visibility", Collections.singletonList("\"//visibility:private\""));
        }
        final var runtimeDeps = getRuntimeDeps(dependencyNature);
        if (!runtimeDeps.isEmpty()) {
            arguments.put(
                    "runtime_deps",
                    runtimeDeps.stream()
                            .map(a -> asString(a.getLabel(Nature.Java)))
                            .sorted()
                            .collect(Collectors.toList()));
        }
        final var deps = getDeps(dependencyNature);
        if (!deps.isEmpty()) {
            arguments.put(
                    "deps",
                    deps.stream()
                            .map(a -> asString(a.getLabel(Nature.Java)))
                            .sorted()
                            .collect(Collectors.toList()));
        }
        if (shouldExportDeps()) {
            arguments.put(
                    "exports",
                    deps.stream()
                            .map(a -> asString(a.getLabel(Nature.Java)))
                            .sorted()
                            .collect(Collectors.toList()));
        }
        output.writeCall("_java_import", arguments);
    }

    void writeJ2clLibrary(final StarlarkOutput output) throws IOException {
        final var j2clConfig =
                null != _artifactModel ? _artifactModel.getSource().getJ2cl() : null;
        final J2clMode mode = getJ2clMode();
        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", asString(getName(Nature.J2cl)));
        if (J2clMode.Library == mode) {
            arguments.put("srcs", Collections.singletonList(asString(getQualifiedSourcesLabel())));
            if (shouldEnableJspecifySupport()) {
                arguments.put(JSPECIFY_SUPPORT_ATTRIBUTE, Boolean.TRUE);
            }
            if (null != j2clConfig) {
                final var suppress = j2clConfig.getSuppress();
                if (null != suppress) {
                    arguments.put(
                            "js_suppress", suppress.stream().map(this::asString).collect(Collectors.toList()));
                }
            }
            final ArtifactModel artifactModel = getArtifactModel();
            if (null != artifactModel) {
                final var visibility = artifactModel.getVisibility();
                if (!visibility.isEmpty()) {
                    arguments.put(
                            "visibility",
                            visibility.stream().map(this::asString).collect(Collectors.toList()));
                }
            } else {
                arguments.put("visibility", Collections.singletonList("\"//visibility:private\""));
            }
            final var deps = getDeps(Nature.J2cl);
            if (!deps.isEmpty()) {
                arguments.put(
                        "deps",
                        deps.stream()
                                .map(a -> asString(a.getLabel(Nature.J2cl)))
                                .sorted()
                                .collect(Collectors.toList()));
            }
            output.writeCall("_j2cl_library", arguments);
        } else {
            assert J2clMode.Import == mode;
            arguments.put("jar", asString(getQualifiedBinaryLabel()));
            final var artifactModel = Objects.requireNonNull(getArtifactModel());
            final var visibility = artifactModel.getVisibility();
            if (!visibility.isEmpty()) {
                arguments.put(
                        "visibility", visibility.stream().map(this::asString).collect(Collectors.toList()));
            }
            output.writeCall("_j2cl_import", arguments);
        }
    }

    void emitJavaPlugin(final StarlarkOutput output, @Nullable final String processorClass) throws IOException {
        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", asString(pluginName(processorClass)));
        if (null != processorClass && generatesApi()) {
            arguments.put("generates_api", "True");
        }
        if (null != processorClass) {
            arguments.put("processor_class", asString(processorClass));
        }
        arguments.put("visibility", Collections.singletonList("\"//visibility:private\""));
        arguments.put("deps", Collections.singletonList("\":" + getName(Nature.Java) + PLUGIN_LIBRARY_SUFFIX + "\""));
        output.writeCall("_java_plugin", arguments);
    }

    String pluginName(@Nullable final String processorClass) {
        return getName(Nature.Java)
                + (null == processorClass
                        ? ""
                        : BazelUtil.cleanNamePart(BazelUtil.COMPONENT_SEPARATOR + processorClass))
                + PLUGIN_SUFFIX;
    }

    void writePluginLibrary(final StarlarkOutput output) throws IOException {
        emitJavaImport(output, PLUGIN_LIBRARY_SUFFIX, Nature.Plugin);
        final var processors = getProcessors();
        if (null == processors) {
            emitJavaPlugin(output, null);
        } else {
            for (final var processor : processors) {
                emitJavaPlugin(output, processor);
            }
        }
        writeJavaPluginLibrary(output);
    }

    void writeJavaPluginLibrary(final StarlarkOutput output) throws IOException {
        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", asString(getName(Nature.Plugin)));
        final var plugins = new ArrayList<String>();
        final var processors = getProcessors();
        if (null == processors) {
            plugins.add(asString(pluginName(null)));
        } else {
            for (final var processor : processors) {
                plugins.add(asString(pluginName(processor)));
            }
        }
        arguments.put("exported_plugins", plugins);
        final var artifactModel = getArtifactModel();
        if (null != artifactModel) {
            final var visibility = artifactModel.getVisibility();
            if (!visibility.isEmpty()) {
                arguments.put(
                        "visibility", visibility.stream().map(this::asString).collect(Collectors.toList()));
            }
        } else {
            arguments.put("visibility", Collections.singletonList("\"//visibility:private\""));
        }
        output.writeCall("_java_library", arguments);
    }

    void writeArtifactTargets(final StarlarkOutput output) throws IOException {
        int round = 0;
        for (final Nature nature : getNatures()) {
            if (!shouldEmitNatureTarget(nature)) {
                continue;
            }
            if (round++ > 0) {
                output.newLine();
            }
            if (Nature.Java == nature) {
                emitJavaImport(output, "");
            } else if (Nature.J2cl == nature) {
                writeJ2clLibrary(output);
            } else // if ( Nature.Plugin == nature )
            {
                assert Nature.Plugin == nature;
                writePluginLibrary(output);
            }
        }
    }

    void writeArtifactHttpFileRule(final StarlarkOutput output) throws IOException {
        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", asString(getRepository()));
        final var a = Objects.requireNonNull(getNode().getArtifact());
        arguments.put("downloaded_file_path", asString(ArtifactUtil.artifactToPath(a)));
        final var sha256 = Objects.requireNonNull(getSha256());
        arguments.put("sha256", asString(sha256.toLowerCase()));
        final var urls = Objects.requireNonNull(getUrls());
        assert !urls.isEmpty();
        arguments.put("urls", urls.stream().map(this::asString).collect(Collectors.toList()));
        output.writeCall("_http_file", arguments);
    }

    void writeArtifactSourcesHttpFileRule(final StarlarkOutput output) throws IOException {
        final var sourceSha256 = Objects.requireNonNull(getSourceSha256());

        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", asString(getSourceRepository()));
        final var a = Objects.requireNonNull(getNode().getArtifact());

        final var artifactPath =
                ArtifactUtil.artifactToPath(a.getGroupId(), a.getArtifactId(), a.getVersion(), "sources", "jar");
        arguments.put("downloaded_file_path", asString(artifactPath));
        arguments.put("sha256", asString(sourceSha256.toLowerCase()));
        final var urls = Objects.requireNonNull(getSourceUrls());
        assert !urls.isEmpty();
        arguments.put("urls", urls.stream().map(this::asString).collect(Collectors.toList()));
        output.writeCall("_http_file", arguments);
    }

    void writeArtifactAnnotationsHttpFileRule(final StarlarkOutput output) throws IOException {
        final var sha256 = Objects.requireNonNull(getExternalAnnotationSha256());

        final var arguments = new LinkedHashMap<String, Object>();
        arguments.put("name", asString(getExternalAnnotationsRepository()));
        final var a = Objects.requireNonNull(getNode().getArtifact());

        final var artifactPath =
                ArtifactUtil.artifactToPath(a.getGroupId(), a.getArtifactId(), a.getVersion(), "annotations", "jar");
        arguments.put("downloaded_file_path", asString(artifactPath));
        arguments.put("sha256", asString(sha256.toLowerCase()));
        final var urls = Objects.requireNonNull(getExternalAnnotationUrls());
        assert !urls.isEmpty();
        arguments.put("urls", urls.stream().map(this::asString).collect(Collectors.toList()));
        output.writeCall("_http_file", arguments);
    }

    private String deriveSuffix(final Nature nature) {
        return nature.suffix(getNatures().size() > 1, getDefaultNature());
    }

    private Nature getDefaultNature() {
        return null != getProcessors()
                ? Nature.Plugin
                : _application.getSource().getOptions().getDefaultNature();
    }

    private String asString(final String value) {
        return value.contains("\n")
                ? "\"\"\"\n" + value + (value.endsWith("\n") ? "" : "\n") + "\"\"\""
                : "\"" + value + "\"";
    }
}
