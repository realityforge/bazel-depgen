package org.realityforge.bazel.depgen.util;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;

public class BazelUtilTest extends AbstractTest {
    @Test
    public void cleanNamePart() {
        assertEquals(BazelUtil.cleanNamePart("com.example:mylib:0.98"), "com_example_mylib_0_98");
        assertEquals(BazelUtil.cleanNamePart("com.example:My-App:22-RC1"), "com_example_my_app_22_rc1");
    }

    @Test
    public void getOutputBase() {
        final File outputBase = requireNonNull(BazelUtil.getOutputBase(
                FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> "/tmp/bazel-output-base\n"));
        assertEquals(outputBase, new File("/tmp/bazel-output-base"));
    }

    @Test
    public void getOutputBase_failure() {
        final File outputBase =
                BazelUtil.getOutputBase(FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> {
                    throw new IllegalStateException("boom");
                });
        assertNull(outputBase);
    }

    @Test
    public void getRepositoryCache() {
        final Path repositoryCache = requireNonNull(BazelUtil.getRepositoryCache(
                FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> "/tmp/repository-cache\n"));
        assertEquals(repositoryCache, Paths.get("/tmp/repository-cache"));
    }

    @Test
    public void getRepositoryCache_fallback() {
        final var invocations = new AtomicInteger();
        final Path repositoryCache = requireNonNull(
                BazelUtil.getRepositoryCache(FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> {
                    if (0 == invocations.getAndIncrement()) {
                        throw new IllegalStateException("boom");
                    }
                    return "/tmp/default-repository-cache\n";
                }));
        assertEquals(repositoryCache, Paths.get("/tmp/default-repository-cache"));
        assertEquals(invocations.get(), 2);
    }

    @Test
    public void getRepositoryCache_fallbackFailure() {
        final Path repositoryCache =
                BazelUtil.getRepositoryCache(FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> {
                    throw new IllegalStateException("boom");
                });
        assertNull(repositoryCache);
    }

    @Test
    public void getDefaultRepositoryCache() {
        final Path repositoryCache = requireNonNull(
                BazelUtil.getDefaultRepositoryCache((cwd, command) -> "/tmp/default-repository-cache\n"));
        assertEquals(repositoryCache, Paths.get("/tmp/default-repository-cache"));
    }

    @Test
    public void getInfo() {
        final BazelUtil.BazelInfo info = requireNonNull(BazelUtil.getInfo(
                FileUtil.getCurrentDirectory().toFile(),
                (cwd, command) ->
                        "output_base: /tmp/bazel-output-base\n" + "repository_cache: /tmp/repository-cache\n"));
        assertEquals(info.getOutputBase(), new File("/tmp/bazel-output-base"));
        assertEquals(info.getRepositoryCache(), Paths.get("/tmp/repository-cache"));
    }

    @Test
    public void getInfo_missingRepositoryCache() {
        final BazelUtil.BazelInfo info = requireNonNull(BazelUtil.getInfo(
                FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> "output_base: /tmp/bazel-output-base\n"));
        assertEquals(info.getOutputBase(), new File("/tmp/bazel-output-base"));
        assertNull(info.getRepositoryCache());
    }

    @Test
    public void getInfo_failure() {
        final BazelUtil.BazelInfo info =
                BazelUtil.getInfo(FileUtil.getCurrentDirectory().toFile(), (cwd, command) -> {
                    throw new IllegalStateException("boom");
                });
        assertNull(info);
    }

    @Test
    public void parseInfo_ignoresNonKeyLines() {
        final BazelUtil.BazelInfo info = BazelUtil.parseInfo("Starting local Bazel server and connecting to it...\n"
                + "output_base: /tmp/bazel-output-base\n"
                + "repository_cache: /tmp/repository-cache\n");
        assertEquals(info.getOutputBase(), new File("/tmp/bazel-output-base"));
        assertEquals(info.getRepositoryCache(), Paths.get("/tmp/repository-cache"));
    }

    @Test
    public void runnerCommands() {
        final var invocations = new AtomicInteger();
        final BazelUtil.BazelRunner runner = (cwd, command) -> {
            final int invocation = invocations.getAndIncrement();
            if (0 == invocation) {
                assertEquals(command, List.of("bazel", "info", "output_base"));
                return "/tmp/bazel-output-base\n";
            } else if (1 == invocation) {
                assertEquals(command, List.of("bazel", "info", "repository_cache"));
                return "/tmp/repository-cache\n";
            } else {
                assertEquals(command, List.of("bazel", "info", "output_base", "repository_cache"));
                return "output_base: /tmp/bazel-output-base\nrepository_cache: /tmp/repository-cache\n";
            }
        };

        assertNotNull(BazelUtil.getOutputBase(FileUtil.getCurrentDirectory().toFile(), runner));
        assertNotNull(
                BazelUtil.getRepositoryCache(FileUtil.getCurrentDirectory().toFile(), runner));
        assertNotNull(BazelUtil.getInfo(FileUtil.getCurrentDirectory().toFile(), runner));
        assertEquals(invocations.get(), 3);
    }
}
