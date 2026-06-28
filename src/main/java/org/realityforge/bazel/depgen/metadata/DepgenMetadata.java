package org.realityforge.bazel.depgen.metadata;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.DepgenException;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.model.RepositoryModel;
import org.realityforge.bazel.depgen.util.OrderedProperties;

/**
 * The depgen specific metadata for a artifact groupId+artifactId+version combination in the context of a particular context.
 * This includes information such as the sha256 values, urls in particular repositories, annotation processors present etc
 * for each artifact/classifier.
 *
 * <p>The data is derived in the context of a particular application record so that only the repositories that are
 * registered in <code>dependency.yaml</code> are checked etc.</p>
 */
public final class DepgenMetadata {
    @NonNull
    public static final String FILENAME = "_depgen.properties";

    static final String SENTINEL = "-";

    @NonNull
    private final ApplicationModel _model;

    @NonNull
    private final Path _file;

    @Nullable
    private OrderedProperties _properties;

    @NonNull
    public static DepgenMetadata fromDirectory(@NonNull final ApplicationModel model, @NonNull final Path dir) {
        return new DepgenMetadata(model, dir.resolve(FILENAME));
    }

    private DepgenMetadata(@NonNull final ApplicationModel model, @NonNull final Path file) {
        _model = Objects.requireNonNull(model);
        _file = Objects.requireNonNull(file);
    }

    public void updateProperty(@NonNull final String key, @NonNull final String value) {
        getCachedProperties().setProperty(key, value);
        saveCachedProperties();
    }

    /**
     * Return the sha256 for artifact with filename and classifier.
     *
     * @param classifier the artifacts classifier or the empty string if no classifier.
     * @param file       the artifact file.
     * @return the sha256 of the specified artifact.
     */
    @NonNull
    public String getSha256(@NonNull final String classifier, @NonNull final File file) {
        return getOrCompute(classifierAsKey(classifier) + ".sha256", () -> RecordUtil.sha256(file));
    }

    /**
     * Return the urls where the artifact can be found.
     *
     * @param artifact               the artifact.
     * @param repositories           the remote repositories associated with the artifact.
     * @param authenticationContexts the authentication contexts used to authenticate against repositories.
     * @param callback               the callback that used to notify invoker or errors/warnings.
     * @return the urls where the artifact is present.
     */
    @NonNull
    public List<String> getUrls(
            @NonNull final Artifact artifact,
            @NonNull final List<RemoteRepository> repositories,
            @NonNull final Map<String, AuthenticationContext> authenticationContexts,
            @NonNull final RecordBuildCallback callback) {
        final var urls = new ArrayList<String>();
        for (final RemoteRepository remoteRepository : repositories) {
            final String name = remoteRepository.getId();
            final RepositoryModel repository = Objects.requireNonNull(_model.findRepository(name));
            final String key = classifierAsKey(artifact.getClassifier()) + "." + name + ".url";
            final Properties properties = getCachedProperties();
            final String existing = properties.getProperty(key);
            if (null != existing && !repository.cacheLookups()) {
                callback.onWarning("Cache entry '" + key + "' for artifact '" + artifact + "' contains a url '"
                        + existing + "' for a repository where cacheLookups is false. Removing cache entry.");
                properties.remove(key);
                saveCachedProperties();
            } else if (!shouldResetCachedProperties()
                    && null != existing
                    && !SENTINEL.equals(existing)
                    && !existing.startsWith(remoteRepository.getUrl())) {
                callback.onWarning(
                        "Cache entry '" + key + "' for artifact '" + artifact + "' contains a url '" + existing
                                + "' that does not match the repository url '" + remoteRepository.getUrl()
                                + "'. Removing cache entry.");
                properties.remove(key);
                saveCachedProperties();
            }

            final String url = repository.cacheLookups()
                    ? getOrCompute(key, () -> lookupArtifact(artifact, remoteRepository, authenticationContexts))
                    : lookupArtifact(artifact, remoteRepository, authenticationContexts);
            if (!SENTINEL.equals(url)) {
                urls.add(url);
            }
        }

        if (urls.isEmpty()) {
            throw new DepgenException("Unable to locate artifact " + artifact + " in any repository.");
        }
        return urls;
    }

    @Nullable
    public List<String> getProcessors(@NonNull final File file) {
        final String processors = getOrCompute("processors", () -> RecordUtil.readAnnotationProcessors(file));
        return SENTINEL.equals(processors) ? null : Collections.unmodifiableList(Arrays.asList(processors.split(",")));
    }

    @Nullable
    public List<String> getJsAssets(@NonNull final File file) {
        final String assets = getOrCompute("js_assets", () -> RecordUtil.readJsAssets(file));
        return SENTINEL.equals(assets) ? null : Collections.unmodifiableList(Arrays.asList(assets.split(",")));
    }

    @NonNull
    private String lookupArtifact(
            @NonNull final Artifact artifact,
            @NonNull final RemoteRepository remoteRepository,
            @NonNull final Map<String, AuthenticationContext> authenticationContexts) {
        final String url = RecordUtil.lookupArtifactInRepository(artifact, remoteRepository, authenticationContexts);
        return null == url ? SENTINEL : url;
    }

    /**
     * Return value cached under key or compute value and cache it before returning value.
     *
     * @param key    the key under which to cache value.
     * @param action the action to calculate value.
     * @return the value.
     */
    @NonNull
    private String getOrCompute(@NonNull final String key, @NonNull final Supplier<String> action) {
        String existingValue = getCachedProperties().getProperty(key);
        if (null != existingValue && shouldResetCachedProperties()) {
            getCachedProperties().remove(key);
            saveCachedProperties();
            existingValue = null;
        }
        if (null != existingValue) {
            return existingValue;
        } else {
            final String value = action.get();
            updateProperty(key, value);
            return value;
        }
    }

    @NonNull
    private String classifierAsKey(@NonNull final String classifier) {
        return classifier.isEmpty() ? "<default>" : classifier;
    }

    private void saveCachedProperties() {
        final OrderedProperties properties = Objects.requireNonNull(_properties);
        try {
            try (final Writer writer = Files.newBufferedWriter(_file)) {
                properties.store(writer, null);
            }
        } catch (final IOException ignored) {
            // Ignored. Metadata cache writes are best-effort.
        }
    }

    @NonNull
    private Properties getCachedProperties() {
        if (null == _properties) {
            final var properties = new OrderedProperties();
            if (_file.toFile().exists() && _file.toFile().isFile()) {
                try {
                    properties.load(Files.newBufferedReader(_file));
                } catch (final IOException ignored) {
                    // Ignored. Assumed to be invalid formatted file that will be fixed when we write to it.
                }
            }
            _properties = properties;
        }
        return _properties;
    }

    private boolean shouldResetCachedProperties() {
        return _model.shouldResetCachedMetadata();
    }
}
