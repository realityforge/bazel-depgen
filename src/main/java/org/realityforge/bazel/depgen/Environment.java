package org.realityforge.bazel.depgen;

import java.io.Console;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class Environment
{
  @Nullable
  private final Console _console;
  @Nonnull
  private final Path _currentDirectory;
  @Nonnull
  private final Logger _logger;
  @Nullable
  private Path _dependenciesFile;
  @Nullable
  private Path _settingsFile;
  @Nullable
  private Path _cacheDir;
  @Nullable
  private Command _command;
  private boolean _resetCachedMetadata;

  Environment( @Nullable final Console console, @Nonnull final Path currentDirectory, @Nonnull final Logger logger )
  {
    _console = console;
    _currentDirectory = Objects.requireNonNull( currentDirectory );
    _logger = Objects.requireNonNull( logger );
  }

  @Nullable
  Console console()
  {
    return _console;
  }

  @Nonnull
  Path currentDirectory()
  {
    return _currentDirectory;
  }

  @Nonnull
  Logger logger()
  {
    return _logger;
  }

  boolean hasDependenciesFile()
  {
    return null != _dependenciesFile;
  }

  @Nonnull
  Path getDependenciesFile()
  {
    assert null != _dependenciesFile;
    return _dependenciesFile;
  }

  void setDependenciesFile( @Nullable final Path dependenciesFile )
  {
    _dependenciesFile = dependenciesFile;
  }

  boolean hasSettingsFile()
  {
    return null != _settingsFile;
  }

  @Nonnull
  Path getSettingsFile()
  {
    assert null != _settingsFile;
    return _settingsFile;
  }

  void setSettingsFile( @Nullable final Path settingsFile )
  {
    _settingsFile = settingsFile;
  }

  boolean hasCacheDir()
  {
    return null != _cacheDir;
  }

  @Nonnull
  Path getCacheDir()
  {
    assert null != _cacheDir;
    return _cacheDir;
  }

  void setCacheDir( @Nullable final Path cacheDir )
  {
    _cacheDir = cacheDir;
  }

  boolean hasCommand()
  {
    return null != _command;
  }

  @Nonnull
  Command getCommand()
  {
    assert null != _command;
    return _command;
  }

  void setCommand( @Nonnull final Command command )
  {
    _command = Objects.requireNonNull( command );
  }

  boolean shouldResetCachedMetadata()
  {
    return _resetCachedMetadata;
  }

  void markResetCachedMetadata()
  {
    _resetCachedMetadata = true;
  }
}
