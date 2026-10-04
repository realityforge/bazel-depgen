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
        FileUtil.write("WORKSPACE", "");
        writeBazelrc();

        final BazelUtil.BazelInfo info = requireNonNull(BazelUtil.getInfo(cwd.toFile()));
        assertNotNull(info.getOutputBase());
        assertEquals(
                requireNonNull(info.getRepositoryCache()), requireNonNull(BazelUtil.getRepositoryCache(cwd.toFile())));
    }
}
