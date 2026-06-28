package org.realityforge.bazel.depgen;

import java.io.Console;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class Environment {
    @Nullable
    private final Console _console;

    @NonNull
    private Path _currentDirectory;

    @NonNull
    private final Logger _logger;

    @Nullable
    private Path _configFile;

    @Nullable
    private Path _settingsFile;

    @Nullable
    private Path _cacheDir;

    @Nullable
    private Path _repositoryCacheDir;

    @Nullable
    private Command _command;

    private boolean _resetCachedMetadata;

    Environment(@Nullable final Console console, @NonNull final Path currentDirectory, @NonNull final Logger logger) {
        _console = console;
        _currentDirectory = Objects.requireNonNull(currentDirectory);
        _logger = Objects.requireNonNull(logger);
    }

    @Nullable
    Console console() {
        return _console;
    }

    public void setCurrentDirectory(@NonNull final Path currentDirectory) {
        _currentDirectory = Objects.requireNonNull(currentDirectory);
    }

    @NonNull
    Path currentDirectory() {
        return _currentDirectory;
    }

    @NonNull
    Logger logger() {
        return _logger;
    }

    boolean hasConfigFile() {
        return null != _configFile;
    }

    @NonNull
    Path getConfigFile() {
        return Objects.requireNonNull(_configFile);
    }

    void setConfigFile(@Nullable final Path configFile) {
        _configFile = configFile;
    }

    boolean hasSettingsFile() {
        return null != _settingsFile;
    }

    @NonNull
    Path getSettingsFile() {
        return Objects.requireNonNull(_settingsFile);
    }

    void setSettingsFile(@Nullable final Path settingsFile) {
        _settingsFile = settingsFile;
    }

    boolean hasCacheDir() {
        return null != _cacheDir;
    }

    @NonNull
    Path getCacheDir() {
        return Objects.requireNonNull(_cacheDir);
    }

    void setCacheDir(@Nullable final Path cacheDir) {
        _cacheDir = cacheDir;
    }

    boolean hasRepositoryCacheDir() {
        return null != _repositoryCacheDir;
    }

    @NonNull
    Path getRepositoryCacheDir() {
        return Objects.requireNonNull(_repositoryCacheDir);
    }

    void setRepositoryCacheDir(@Nullable final Path repositoryCacheDir) {
        _repositoryCacheDir = repositoryCacheDir;
    }

    boolean hasCommand() {
        return null != _command;
    }

    @NonNull
    Command getCommand() {
        return Objects.requireNonNull(_command);
    }

    void setCommand(@NonNull final Command command) {
        _command = Objects.requireNonNull(command);
    }

    boolean shouldResetCachedMetadata() {
        return _resetCachedMetadata;
    }

    void markResetCachedMetadata() {
        _resetCachedMetadata = true;
    }
}
