package org.realityforge.bazel.depgen.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BazelUtil {
    @NonNull
    public static final String COMPONENT_SEPARATOR = "__";

    @NonNull
    private static final BazelRunner DEFAULT_RUNNER =
            (cwd, command) -> Exec.capture(p -> p.command(command).directory(cwd), 0);

    private BazelUtil() {}

    @NonNull
    public static String cleanNamePart(@NonNull final String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    @Nullable
    public static File getOutputBase(@NonNull final File cwd) {
        return getOutputBase(cwd, DEFAULT_RUNNER);
    }

    @Nullable
    static File getOutputBase(@NonNull final File cwd, @NonNull final BazelRunner runner) {
        try {
            return new File(runBazelInfo(cwd, runner, "output_base").trim());
        } catch (final Exception e) {
            return null;
        }
    }

    @Nullable
    public static Path getRepositoryCache(@NonNull final File cwd) {
        return getRepositoryCache(cwd, DEFAULT_RUNNER);
    }

    @Nullable
    static Path getRepositoryCache(@NonNull final File cwd, @NonNull final BazelRunner runner) {
        try {
            return Paths.get(runBazelInfo(cwd, runner, "repository_cache").trim());
        } catch (final Exception e) {
            return getDefaultRepositoryCache(runner);
        }
    }

    @Nullable
    public static BazelInfo getInfo(@NonNull final File cwd) {
        return getInfo(cwd, DEFAULT_RUNNER);
    }

    @Nullable
    static BazelInfo getInfo(@NonNull final File cwd, @NonNull final BazelRunner runner) {
        try {
            return parseInfo(runBazelInfo(cwd, runner, "output_base", "repository_cache"));
        } catch (final Exception e) {
            return null;
        }
    }

    @Nullable
    public static Path getDefaultRepositoryCache() {
        return getDefaultRepositoryCache(DEFAULT_RUNNER);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nullable
    static Path getDefaultRepositoryCache(@NonNull final BazelRunner runner) {
        try {
            final var dir = File.createTempFile("bazel-depgen", "dir");
            dir.delete();
            dir.mkdir();
            final var file = new File(dir, "WORKSPACE");
            Files.write(file.toPath(), new byte[0]);
            final String repositoryCache = runBazelInfo(dir, runner, "repository_cache");
            file.delete();
            dir.delete();
            return Paths.get(repositoryCache.trim());
        } catch (final Throwable ignored) {
            return null;
        }
    }

    @NonNull
    static BazelInfo parseInfo(@NonNull final String output) {
        File outputBase = null;
        Path repositoryCache = null;
        for (final String line : output.split("\\R")) {
            final int index = line.indexOf(": ");
            if (-1 == index) {
                continue;
            }
            final String key = line.substring(0, index);
            final String value = line.substring(index + 2).trim();
            if ("output_base".equals(key)) {
                outputBase = new File(value);
            } else if ("repository_cache".equals(key)) {
                repositoryCache = Paths.get(value);
            }
        }
        return new BazelInfo(outputBase, repositoryCache);
    }

    @NonNull
    private static String runBazelInfo(
            @NonNull final File cwd, @NonNull final BazelRunner runner, @NonNull final String... keys) {
        final String[] command = new String[2 + keys.length];
        command[0] = "bazel";
        command[1] = "info";
        System.arraycopy(keys, 0, command, 2, keys.length);
        return runner.capture(cwd, Arrays.asList(command));
    }

    @FunctionalInterface
    interface BazelRunner {
        @NonNull
        String capture(@NonNull File cwd, @NonNull List<String> command);
    }

    public static final class BazelInfo {
        @Nullable
        private final File _outputBase;

        @Nullable
        private final Path _repositoryCache;

        public BazelInfo(@Nullable final File outputBase, @Nullable final Path repositoryCache) {
            _outputBase = outputBase;
            _repositoryCache = repositoryCache;
        }

        @Nullable
        public File getOutputBase() {
            return _outputBase;
        }

        @Nullable
        public Path getRepositoryCache() {
            return _repositoryCache;
        }
    }
}
