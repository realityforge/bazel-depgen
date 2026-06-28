package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import java.util.logging.Logger;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.jspecify.annotations.NonNull;

final class SettingsUtil {
    private SettingsUtil() {}

    @NonNull
    static Settings loadSettings(@NonNull final Path settingsFile, @NonNull final Logger logger)
            throws SettingsBuildingException {
        final SettingsBuildingRequest request =
                new DefaultSettingsBuildingRequest().setUserSettingsFile(settingsFile.toFile());
        final SettingsBuildingResult result =
                new DefaultSettingsBuilderFactory().newInstance().build(request);
        result.getProblems().forEach(problem -> logger.warning(problem.toString()));
        return result.getEffectiveSettings();
    }
}
