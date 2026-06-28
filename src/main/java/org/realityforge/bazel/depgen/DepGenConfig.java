package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.util.OrderedProperties;

public final class DepGenConfig {
    @NonNull
    static final String PROPERTY_KEY = "bazel-degen.version";

    @NonNull
    private static final Properties c_config = loadConfig();

    @NonNull
    public static String getVersion() {
        final String versionProperty = System.getProperty(PROPERTY_KEY);
        if (null != versionProperty) {
            return versionProperty;
        } else {
            return Objects.requireNonNull(c_config.getProperty("version"));
        }
    }

    @NonNull
    public static String getCoord() {
        return getGroupId() + ":" + getArtifactId() + ":jar:" + getClassifier() + ":" + getVersion();
    }

    @NonNull
    public static String getGroupId() {
        return Objects.requireNonNull(c_config.getProperty("group"));
    }

    @NonNull
    public static String getArtifactId() {
        return Objects.requireNonNull(c_config.getProperty("id"));
    }

    @NonNull
    public static String getClassifier() {
        return "all";
    }

    @NonNull
    private static Properties loadConfig() {
        final InputStream inputStream = DepGenConfig.class.getResourceAsStream("config.properties");
        assert null != inputStream;

        try {
            final var properties = new OrderedProperties();
            properties.load(inputStream);
            return properties;
        } catch (final IOException e) {
            throw new DepgenConfigurationException("Failed to load config.properties", e);
        }
    }

    private DepGenConfig() {}
}
