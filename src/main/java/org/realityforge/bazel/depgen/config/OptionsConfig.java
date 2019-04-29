package org.realityforge.bazel.depgen.config;

import javax.annotation.Nonnull;

public class OptionsConfig
{
  public static final String DEFAULT_WORKSPACE_DIR = ".";
  public static final String DEFAULT_EXTENSION_FILE = "3rdparty/dependencies.bzl";
  @Nonnull
  private String workspaceDirectory = DEFAULT_WORKSPACE_DIR;
  @Nonnull
  private String extensionFile = DEFAULT_EXTENSION_FILE;
  private boolean failOnInvalidPom = true;
  private boolean failOnMissingPom = true;
  private boolean emitDependencyGraph = true;

  @Nonnull
  public String getWorkspaceDirectory()
  {
    return workspaceDirectory;
  }

  public void setWorkspaceDirectory( @Nonnull final String workspaceDirectory )
  {
    this.workspaceDirectory = workspaceDirectory;
  }

  @Nonnull
  public String getExtensionFile()
  {
    return extensionFile;
  }

  public void setExtensionFile( @Nonnull final String extensionFile )
  {
    this.extensionFile = extensionFile;
  }

  public boolean isFailOnInvalidPom()
  {
    return failOnInvalidPom;
  }

  public void setFailOnInvalidPom( final boolean failOnInvalidPom )
  {
    this.failOnInvalidPom = failOnInvalidPom;
  }

  public boolean isFailOnMissingPom()
  {
    return failOnMissingPom;
  }

  public void setFailOnMissingPom( final boolean failOnMissingPom )
  {
    this.failOnMissingPom = failOnMissingPom;
  }

  public boolean isEmitDependencyGraph()
  {
    return emitDependencyGraph;
  }

  public void setEmitDependencyGraph( final boolean emitDependencyGraph )
  {
    this.emitDependencyGraph = emitDependencyGraph;
  }
}
