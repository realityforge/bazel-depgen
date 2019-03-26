package org.realityforge.bazel.depgen.config;

import javax.annotation.Nonnull;

@SuppressWarnings( "unused" )
public class OptionsConfig
{
  public static final String DEFAULT_WORKSPACE_DIR = ".";
  public static final String DEFAULT_EXTENSION_FILE = "3rdparty/workspace.bzl";
  @Nonnull
  private String workspaceDirectory = DEFAULT_WORKSPACE_DIR;
  @Nonnull
  private String extensionFile = DEFAULT_EXTENSION_FILE;

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
}
