package org.realityforge.bazel.depgen.util;

import org.eclipse.aether.artifact.Artifact;
import org.jspecify.annotations.NonNull;

public final class ArtifactUtil {
    private ArtifactUtil() {}

    @NonNull
    public static String artifactToPath(@NonNull final Artifact artifact) {
        return artifactToPath(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getClassifier(),
                artifact.getExtension());
    }

    @NonNull
    public static String artifactToPath(
            @NonNull final String groupId,
            @NonNull final String artifactId,
            @NonNull final String version,
            @NonNull final String classifier,
            @NonNull final String extension) {
        return artifactToDirectory(groupId, artifactId, version) + "/"
                + artifactToLocalFilename(artifactId, version, classifier, extension);
    }

    @NonNull
    public static String artifactToDirectory(
            @NonNull final String groupId, @NonNull final String artifactId, @NonNull final String version) {
        return groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version;
    }

    @NonNull
    public static String artifactToLocalFilename(@NonNull final Artifact artifact) {
        return artifactToLocalFilename(
                artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier(), artifact.getExtension());
    }

    @NonNull
    private static String artifactToLocalFilename(
            @NonNull final String artifactId,
            @NonNull final String version,
            @NonNull final String classifier,
            @NonNull final String extension) {
        return artifactId + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + extension;
    }
}
