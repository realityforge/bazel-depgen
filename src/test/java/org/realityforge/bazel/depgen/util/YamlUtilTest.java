package org.realityforge.bazel.depgen.util;

import static org.testng.Assert.*;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.testng.annotations.Test;

public class YamlUtilTest extends AbstractTest {
    @Test
    public void asYamlString() {
        assertEquals(YamlUtil.asYamlString(new ApplicationConfig()), "{}\n");

        // With a few settings
        final var object = new ApplicationConfig();
        final var options = new OptionsConfig();
        options.setDefaultNature(Nature.J2cl);
        object.setOptions(options);
        assertEquals(YamlUtil.asYamlString(object), "options:\n  defaultNature: J2cl\n");
    }
}
