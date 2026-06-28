package org.realityforge.bazel.depgen.util;

import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

public final class YamlUtil {
    private YamlUtil() {}

    public static String asYamlString(@NonNull final Object object) {
        return new Yaml(new OmitNullRepresenter()).dumpAs(object, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
    }
}
