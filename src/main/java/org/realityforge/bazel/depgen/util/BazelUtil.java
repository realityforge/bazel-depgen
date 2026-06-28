package org.realityforge.bazel.depgen.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BazelUtil {
    @NonNull
    public static final String COMPONENT_SEPARATOR = "__";

    private BazelUtil() {}

    @NonNull
    public static String cleanNamePart(@NonNull final String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    @Nullable
    public static File getOutputBase(@NonNull final File cwd) {
        try {
            final String repositoryCache =
                    Exec.capture(p -> p.command("bazel", "info", "output_base").directory(cwd), 0);
            return new File(repositoryCache.trim());
        } catch (final Exception e) {
            return null;
        }
    }

    @Nullable
    public static Path getRepositoryCache(@NonNull final File cwd) {
        try {
            final String repositoryCache = Exec.capture(
                    p -> p.command("bazel", "info", "repository_cache").directory(cwd), 0);
            return Paths.get(repositoryCache.trim());
        } catch (final Exception e) {
            return getDefaultRepositoryCache();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nullable
    static Path getDefaultRepositoryCache() {
        try {
            final var dir = File.createTempFile("bazel-depgen", "dir");
            dir.delete();
            dir.mkdir();
            final var file = new File(dir, "WORKSPACE");
            Files.write(file.toPath(), new byte[0]);
            final String repositoryCache = Exec.capture(
                    p -> p.command("bazel", "info", "repository_cache").directory(dir), 0);
            file.delete();
            dir.delete();
            return Paths.get(repositoryCache.trim());
        } catch (final Throwable ignored) {
            return null;
        }
    }
}
