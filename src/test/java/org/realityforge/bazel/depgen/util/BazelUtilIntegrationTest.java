package org.realityforge.bazel.depgen.util;

import static org.testng.Assert.*;

import gir.io.FileUtil;
import java.nio.file.Path;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;

public class BazelUtilIntegrationTest extends AbstractTest {
    @Test
    public void getInfo() throws Exception {
        final Path cwd = FileUtil.getCurrentDirectory();
        final Path repositoryCache = FileUtil.createLocalTempDir();
        FileUtil.write("WORKSPACE", "");
        writeBazelrc(repositoryCache);

        final BazelUtil.BazelInfo info = requireNonNull(BazelUtil.getInfo(cwd.toFile()));
        assertNotNull(info.getOutputBase());
        assertEquals(requireNonNull(info.getRepositoryCache()).toAbsolutePath().normalize(), repositoryCache);
    }

    @Test
    public void getDefaultRepositoryCache() {
        final Path repositoryCache = requireNonNull(BazelUtil.getDefaultRepositoryCache());
        assertTrue(repositoryCache.toAbsolutePath().toString().endsWith("/cache/repos/v1"));
    }
}
