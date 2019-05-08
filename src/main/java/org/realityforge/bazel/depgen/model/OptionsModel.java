package org.realityforge.bazel.depgen.model;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.OptionsConfig;

public final class OptionsModel
{
  @Nonnull
  private final OptionsConfig _source;
  @Nonnull
  private final Path _workspaceDirectory;
  @Nonnull
  private final Path _extensionFile;

  /**
   * Create the OptionsModel from config.
   * All paths are relative to baseDirectory.
   *
   * @param baseDirectory the directory that paths are relative to.
   * @param source        the original configuration source.
   */
  @Nonnull
  static OptionsModel parse( @Nonnull final Path baseDirectory, @Nonnull final OptionsConfig source )
  {
    final Path workspaceDirectory =
      baseDirectory.resolve( source.getWorkspaceDirectory() ).toAbsolutePath().normalize();
    final Path extensionFile = baseDirectory.resolve( source.getExtensionFile() ).toAbsolutePath().normalize();
    return new OptionsModel( source, workspaceDirectory, extensionFile );
  }

  private OptionsModel( @Nonnull final OptionsConfig source,
                        @Nonnull final Path workspaceDirectory,
                        @Nonnull final Path extensionFile )
  {
    _source = Objects.requireNonNull( source );
    _workspaceDirectory = Objects.requireNonNull( workspaceDirectory );
    _extensionFile = Objects.requireNonNull( extensionFile );
  }

  @Nonnull
  public OptionsConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public Path getWorkspaceDirectory()
  {
    return _workspaceDirectory;
  }

  @Nonnull
  public Path getExtensionFile()
  {
    return _extensionFile;
  }

  @Nonnull
  public String getWorkspaceMacroName()
  {
    return _source.getWorkspaceMacroName();
  }

  @Nonnull
  public String getNamePrefix()
  {
    return _source.getNamePrefix();
  }

  public boolean failOnInvalidPom()
  {
    return _source.isFailOnInvalidPom();
  }

  public boolean failOnMissingPom()
  {
    return _source.isFailOnMissingPom();
  }

  public boolean emitDependencyGraph()
  {
    return _source.isEmitDependencyGraph();
  }

  public boolean includeSource()
  {
    return _source.isIncludeSource();
  }
}
